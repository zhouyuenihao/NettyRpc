package com.netty.rpc.client.connect;

import com.netty.rpc.client.handler.RpcClientHandler;
import com.netty.rpc.client.handler.RpcClientInitializer;
import com.netty.rpc.protocol.RpcProtocol;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * RPC Connect Manage of ZooKeeper
 * Created by luxiaoxun on 2016-03-16.
 */
public class ConnectManage {
    private static final Logger logger = LoggerFactory.getLogger(ConnectManage.class);
    private volatile static ConnectManage connectManage;

    private EventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);
    private static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(4, 8,
            600L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(1000));

    private Map<RpcProtocol, RpcClientHandler> connectedServerNodes = new ConcurrentHashMap<>();

    private ReentrantLock lock = new ReentrantLock();
    private Condition connected = lock.newCondition();
    private long waitTimeout = 5000;
    private AtomicInteger roundRobin = new AtomicInteger(0);
    private volatile boolean isRuning = true;

    private ConnectManage() {
    }

    private static class SingletonHolder {
        private static final ConnectManage instance = new ConnectManage();
    }

    public static ConnectManage getInstance() {
        return SingletonHolder.instance;
    }

    public void updateConnectedServer(List<RpcProtocol> serviceList) {
        if (serviceList != null) {
            if (serviceList.size() > 0) {
                //update local serverNodes cache
                HashSet<RpcProtocol> serviceSet = new HashSet<>(serviceList.size());
                for (int i = 0; i < serviceList.size(); ++i) {
                    RpcProtocol rpcProtocol = serviceList.get(i);
                    serviceSet.add(rpcProtocol);
                }

                // Add new server node
                for (final RpcProtocol rpcProtocol : serviceSet) {
                    if (!connectedServerNodes.keySet().contains(rpcProtocol)) {
                        connectServerNode(rpcProtocol);
                    }
                }

                // Close and remove invalid server nodes
                for (RpcProtocol rpcProtocol : connectedServerNodes.keySet()) {
                    if (!serviceSet.contains(rpcProtocol)) {
                        logger.info("Remove invalid service: " + rpcProtocol.toJson());
                        RpcClientHandler handler = connectedServerNodes.get(rpcProtocol);
                        if (handler != null) {
                            handler.close();
                        }
                        connectedServerNodes.remove(rpcProtocol);
                    }
                }
            } else {
                // No available server node ( All server nodes are down )
                logger.error("No available server node. All server nodes are down !!!");
                for (RpcProtocol rpcProtocol : connectedServerNodes.keySet()) {
                    RpcClientHandler handler = connectedServerNodes.get(rpcProtocol);
                    handler.close();
                    connectedServerNodes.remove(rpcProtocol);
                }
            }
        }
    }

    private void connectServerNode(RpcProtocol rpcProtocol) {
        logger.info("New service: {}, uuid: {}, host: {}, port:{}", rpcProtocol.getServiceName(),
                rpcProtocol.getUuid(), rpcProtocol.getHost(), rpcProtocol.getPort());
        final InetSocketAddress remotePeer = new InetSocketAddress(rpcProtocol.getHost(), rpcProtocol.getPort());
        threadPoolExecutor.submit(new Runnable() {
            @Override
            public void run() {
                Bootstrap b = new Bootstrap();
                b.group(eventLoopGroup)
                        .channel(NioSocketChannel.class)
                        .handler(new RpcClientInitializer());

                ChannelFuture channelFuture = b.connect(remotePeer);
                channelFuture.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(final ChannelFuture channelFuture) throws Exception {
                        if (channelFuture.isSuccess()) {
                            logger.info("Successfully connect to remote server. remote peer = " + remotePeer);
                            RpcClientHandler handler = channelFuture.channel().pipeline().get(RpcClientHandler.class);
                            addHandler(handler, rpcProtocol);
                        }
                    }
                });
            }
        });
    }

    private void addHandler(RpcClientHandler handler, RpcProtocol rpcProtocol) {
        connectedServerNodes.put(rpcProtocol, handler);
        signalAvailableHandler();
    }

    private void signalAvailableHandler() {
        lock.lock();
        try {
            connected.signalAll();
        } finally {
            lock.unlock();
        }
    }

    private boolean waitingForHandler() throws InterruptedException {
        lock.lock();
        try {
            logger.warn("Waiting for available service");
            return connected.await(this.waitTimeout, TimeUnit.MILLISECONDS);
        } finally {
            lock.unlock();
        }
    }

    public RpcClientHandler chooseHandler() {
        int size = connectedServerNodes.values().size();
        while (isRuning && size <= 0) {
            try {
                waitingForHandler();
                size = connectedServerNodes.values().size();
            } catch (InterruptedException e) {
                logger.error("Waiting for available node is interrupted!", e);
            }
        }
        int index = (roundRobin.getAndAdd(1) + size) % size;
        List<RpcClientHandler> connectedHandlers = new ArrayList<>(connectedServerNodes.values());
        return connectedHandlers.get(index);
    }

    public void stop() {
        isRuning = false;
        for (RpcProtocol rpcProtocol : connectedServerNodes.keySet()) {
            RpcClientHandler handler = connectedServerNodes.get(rpcProtocol);
            handler.close();
            connectedServerNodes.remove(rpcProtocol);
        }
        signalAvailableHandler();
        threadPoolExecutor.shutdown();
        eventLoopGroup.shutdownGracefully();
    }
}

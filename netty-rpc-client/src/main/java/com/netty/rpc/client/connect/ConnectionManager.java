package com.netty.rpc.client.connect;

import com.netty.rpc.client.handler.RpcClientHandler;
import com.netty.rpc.client.handler.RpcClientInitializer;
import com.netty.rpc.client.route.RpcLoadBalance;
import com.netty.rpc.client.route.impl.RpcLoadBalanceRoundRobin;
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
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * RPC Connection Manager
 * Created by luxiaoxun on 2016-03-16.
 */
public class ConnectionManager {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);

    private EventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);
    private static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(4, 8,
            600L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(1000));

    private Map<RpcProtocol, RpcClientHandler> connectedServerNodes = new ConcurrentHashMap<>();
    private ReentrantLock lock = new ReentrantLock();
    private Condition connected = lock.newCondition();
    private long waitTimeout = 5000;
    private RpcLoadBalance loadBalance = new RpcLoadBalanceRoundRobin();
    private volatile boolean isRunning = true;

    private ConnectionManager() {
    }

    private static class SingletonHolder {
        private static final ConnectionManager instance = new ConnectionManager();
    }

    public static ConnectionManager getInstance() {
        return SingletonHolder.instance;
    }

    public void updateConnectedServer(List<RpcProtocol> serviceList) {
        threadPoolExecutor.submit(new Runnable() {
            @Override
            public void run() {
                if (serviceList != null) {
                    if (serviceList.size() > 0) {
                        // Update local serverNodes cache
                        HashSet<RpcProtocol> serviceSet = new HashSet<>(serviceList.size());
                        for (int i = 0; i < serviceList.size(); ++i) {
                            RpcProtocol rpcProtocol = serviceList.get(i);
                            serviceSet.add(rpcProtocol);
                        }

                        // Add new server info
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
                        // No available service
                        logger.error("No available service!");
                        for (RpcProtocol rpcProtocol : connectedServerNodes.keySet()) {
                            RpcClientHandler handler = connectedServerNodes.get(rpcProtocol);
                            handler.close();
                            connectedServerNodes.remove(rpcProtocol);
                        }
                    }
                }
            }
        });
    }

    private void connectServerNode(RpcProtocol rpcProtocol) {
        logger.info("New service: {}, version:{}, uuid: {}, host: {}, port:{}", rpcProtocol.getServiceName(),
                rpcProtocol.getVersion(), rpcProtocol.getUuid(), rpcProtocol.getHost(), rpcProtocol.getPort());
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
                            connectedServerNodes.put(rpcProtocol, handler);
                            signalAvailableHandler();
                        }
                    }
                });
            }
        });
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

    public RpcClientHandler chooseHandler(String serviceKey) throws Exception {
        int size = connectedServerNodes.values().size();
        while (isRunning && size <= 0) {
            try {
                waitingForHandler();
                size = connectedServerNodes.values().size();
            } catch (InterruptedException e) {
                logger.error("Waiting for available service is interrupted!", e);
            }
        }
        RpcProtocol rpcProtocol = loadBalance.route(serviceKey, connectedServerNodes);
        return connectedServerNodes.get(rpcProtocol);
    }

    public void stop() {
        isRunning = false;
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

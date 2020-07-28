package com.netty.rpc.client.connect;

import com.netty.rpc.client.handler.RpcClientHandler;
import com.netty.rpc.client.handler.RpcClientInitializer;
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

    private Map<String, RpcClientHandler> connectedServerNodes = new ConcurrentHashMap<>();

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

    public void updateConnectedServer(List<String> allServerAddress) {
        if (allServerAddress != null) {
            if (allServerAddress.size() > 0) {
                //update local serverNodes cache
                HashSet<String> newAllServerNodeSet = new HashSet<>(allServerAddress.size());
                for (int i = 0; i < allServerAddress.size(); ++i) {
                    String address = allServerAddress.get(i);
                    newAllServerNodeSet.add(address);
                }

                // Add new server node
                for (final String address : newAllServerNodeSet) {
                    if (!connectedServerNodes.keySet().contains(address)) {
                        connectServerNode(address);
                    }
                }

                // Close and remove invalid server nodes
                for (String address : connectedServerNodes.keySet()) {
                    if (!newAllServerNodeSet.contains(address)) {
                        logger.info("Remove invalid server node " + address);
                        RpcClientHandler handler = connectedServerNodes.get(address);
                        if (handler != null) {
                            handler.close();
                        }
                        connectedServerNodes.remove(address);
                    }
                }
            } else {
                // No available server node ( All server nodes are down )
                logger.error("No available server node. All server nodes are down !!!");
                for (String address : connectedServerNodes.keySet()) {
                    RpcClientHandler handler = connectedServerNodes.get(address);
                    handler.close();
                    connectedServerNodes.remove(address);
                }
            }
        }
    }

    private void connectServerNode(String address) {
        String[] array = address.split(":");
        // Check the format, uuid:IP:port
        if (array.length != 3) {
            logger.warn("Wrong address info: " + address);
            return;
        }
        String uuid = array[0];
        String host = array[1];
        int port = Integer.parseInt(array[2]);
        logger.info("New service info: uuid: {}, host: {}, port:{}", uuid, host, port);
        final InetSocketAddress remotePeer = new InetSocketAddress(host, port);
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
                            addHandler(handler, address);
                        }
                    }
                });
            }
        });
    }

    private void addHandler(RpcClientHandler handler, String address) {
        connectedServerNodes.put(address, handler);
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
        for (String address : connectedServerNodes.keySet()) {
            RpcClientHandler handler = connectedServerNodes.get(address);
            handler.close();
            connectedServerNodes.remove(address);
        }
        signalAvailableHandler();
        threadPoolExecutor.shutdown();
        eventLoopGroup.shutdownGracefully();
    }
}

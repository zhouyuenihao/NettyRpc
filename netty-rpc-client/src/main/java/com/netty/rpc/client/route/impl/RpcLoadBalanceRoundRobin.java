package com.netty.rpc.client.route.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netty.rpc.client.handler.RpcClientHandler;
import com.netty.rpc.client.route.RpcLoadBalance;
import com.netty.rpc.protocol.RpcProtocol;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Round robin load balance
 * Created by luxiaoxun on 2020-08-01.
 */
public class RpcLoadBalanceRoundRobin extends RpcLoadBalance {
    private AtomicInteger roundRobin = new AtomicInteger(0);

    public RpcProtocol doRoute(List<RpcProtocol> addressList) {
        int size = addressList.size();
        // Round robin
        int index = (roundRobin.getAndAdd(1) + size) % size;
        return addressList.get(index);
    }

    @Override
    public RpcProtocol route(String serviceKey, Map<RpcProtocol, RpcClientHandler> connectedServerNodes) throws Exception {
        Map<String, List<RpcProtocol>> serviceMap = getServiceMap(connectedServerNodes);
        List<RpcProtocol> addressList = serviceMap.get(serviceKey);
        if (addressList != null && addressList.size() > 0) {
            return doRoute(addressList);
        } else {
            throw new Exception("Can not find connection for service: " + serviceKey);
        }
    }

    private final AtomicInteger nextServerCyclicCounter = new AtomicInteger(0);

    private int incrementAndGetModulo(int modulo) {
        for (; ; ) {
            int current = nextServerCyclicCounter.get();
            int next = (current + 1) % modulo;
            if (nextServerCyclicCounter.compareAndSet(current, next))
                System.out.println(nextServerCyclicCounter.get());
            return next;
        }
    }

    public static void main(String[] args) {
        Thread.currentThread().interrupt();
        System.out.println("随机数：" + ThreadLocalRandom.current().nextInt(10));
        RpcLoadBalanceRoundRobin robin = new RpcLoadBalanceRoundRobin();
        for (int i = 0; i < 10; i++) {
            int a = robin.incrementAndGetModulo(10);
            System.out.println("server:" + a);
        }
        //robin.demo1();
        //robin.demo2();
    }

    /*public void demo1() {
        for (;;) {
            ObjectMapper mapper = new ObjectMapper();
            System.out.println("a");
        }
    }

    public void demo2() {
        while (true) {
            System.out.println("b");
        }
    }*/

}

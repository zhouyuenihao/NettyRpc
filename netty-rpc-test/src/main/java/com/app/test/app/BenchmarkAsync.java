package com.app.test.app;

import com.netty.rpc.client.handler.RPCFuture;
import com.netty.rpc.client.RpcClient;
import com.netty.rpc.client.proxy.IAsyncObjectProxy;
import com.netty.rpc.client.discovery.ServiceDiscovery;
import com.app.test.client.HelloService;

import java.util.concurrent.TimeUnit;

/**
 * Created by luxiaoxun on 2016/3/16.
 */
public class BenchmarkAsync {
    public static void main(String[] args) throws InterruptedException {
        ServiceDiscovery serviceDiscovery = new ServiceDiscovery("10.217.59.164:2181");
        final RpcClient rpcClient = new RpcClient(serviceDiscovery);

        int threadNum = 1;
        final int requestNum = 100;
        Thread[] threads = new Thread[threadNum];

        long startTime = System.currentTimeMillis();
        //benchmark for async call
        for (int i = 0; i < threadNum; ++i) {
            threads[i] = new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < requestNum; i++) {
                        try {
                            IAsyncObjectProxy client = rpcClient.createAsync(HelloService.class);
                            RPCFuture helloFuture = client.call("hello", Integer.toString(i));
                            String result = (String) helloFuture.get(3000, TimeUnit.MILLISECONDS);
                            if (!result.equals("Hello! " + i)) {
                                System.out.println("error = " + result);
                            } else {
                                System.out.println("result = " + result);
                            }
                        } catch (Exception e) {
                            System.out.println(e);
                        }
                    }
                }
            });
            threads[i].start();
        }
        for (int i = 0; i < threads.length; i++) {
            threads[i].join();
        }
        long timeCost = (System.currentTimeMillis() - startTime);
        String msg = String.format("Async call total-time-cost:%sms, req/s=%s", timeCost, ((double) (requestNum * threadNum)) / timeCost * 1000);
        System.out.println(msg);

        rpcClient.stop();

    }
}

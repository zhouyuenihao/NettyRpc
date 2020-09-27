package com.app.test;

import com.app.test.service.Foo;
import com.app.test.service.HelloService;
import com.netty.rpc.client.RpcClient;
import com.netty.rpc.client.handler.RpcFuture;
import com.netty.rpc.client.proxy.RpcFunction;
import com.netty.rpc.client.proxy.RpcFunction2;
import com.netty.rpc.client.proxy.RpcService;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:client-spring.xml")
public class ServiceTest2 {
    @Autowired
    private Foo foo;
    @Autowired
    private RpcClient rpcClient;

    @Test
    public void say() {
        String result = foo.say("Foo");
        Assert.assertEquals("Hello Foo", result);
    }

    @Test
    public void mr() throws Exception {
        RpcService<HelloService, String, RpcFunction<HelloService,String>> helloService = rpcClient.createAsyncService(HelloService.class, "1.0");
        RpcFuture result = helloService.call(HelloService::hello, "World");
        Assert.assertEquals("Hello World", result.get());

        RpcService<HelloService, String, RpcFunction2<HelloService, String, Integer>> helloServicev1 = rpcClient.createAsyncService(HelloService.class, "1.0");
        RpcFuture resultv1 = helloServicev1.call(HelloService::substring, "World", 2);
        Assert.assertEquals("Wo", resultv1.get());

    }
}

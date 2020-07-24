package com.app.test.server;

import com.netty.rpc.server.RpcService;
import com.app.test.service.HelloService;
import com.app.test.service.Person;

@RpcService(HelloService.class)
public class HelloServiceImpl implements HelloService {

    public HelloServiceImpl(){

    }

    @Override
    public String hello(String name) {
        return "Hello! " + name;
    }

    @Override
    public String hello(Person person) {
        return "Hello! " + person.getFirstName() + " " + person.getLastName();
    }
}

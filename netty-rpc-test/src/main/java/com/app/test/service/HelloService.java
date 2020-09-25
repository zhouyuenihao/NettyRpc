package com.app.test.service;

public interface HelloService {
    String hello(String name);

    String hello(Person person);

    String substring(String str, Integer indexOf);
}

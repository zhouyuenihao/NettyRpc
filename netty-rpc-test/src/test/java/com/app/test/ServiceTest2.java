package com.app.test;

import com.app.test.service.Foo;
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

    @Test
    public void say(){
        String result = foo.say("Foo");
        Assert.assertEquals("Hello Foo", result);
    }
}

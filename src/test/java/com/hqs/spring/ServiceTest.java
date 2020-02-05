package com.hqs.spring;


import com.hqs.HelloService;
import com.hqs.async.AsyncRPCCallback;
import com.hqs.async.RPCFuture;
import com.hqs.client.RPCClient;
import com.hqs.proxy.AsyncObjectProxy;
import com.hqs.registry.ServiceDiscovery;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:client.xml")
public class ServiceTest {

    private static final Logger logger = LoggerFactory.getLogger(ServiceTest.class);

    @Autowired
    private RPCClient rpcClient;


    @Test
    public void syncTest() {
        HelloService helloService = rpcClient.create(HelloService.class);
        String result = helloService.sayHi("hqs");
        System.out.println(result);
        Assert.assertEquals("Hi hqs", result);
    }

    @Test
    public void asyncInvokeTest() {
        ServiceDiscovery serviceDiscovery = new ServiceDiscovery("127.0.0.1:2181");
        RPCClient rpcClient = new RPCClient(serviceDiscovery);

        AsyncObjectProxy asyncClient = rpcClient.createAsync(HelloService.class);
        RPCFuture future = asyncClient.call("sayHi", "hqs");
        try {
            String result = (String) future.get(5, TimeUnit.SECONDS);
            Assert.assertEquals("Hi hqs", result);
            System.out.println(result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void syncCallbackTest() {
        ServiceDiscovery serviceDiscovery = new ServiceDiscovery("127.0.0.1:2181");
        RPCClient rpcClient = new RPCClient(serviceDiscovery);

        AsyncObjectProxy asyncClient = rpcClient.createAsync(HelloService.class);

        RPCFuture future = asyncClient.call("sayHi", "hqs");

        final CountDownLatch latch = new CountDownLatch(1);

        future.addCallback(new AsyncRPCCallback() {
            @Override
            public void success(Object result) {
                System.out.println("result:" + result.toString());
                Assert.assertEquals("Hi hqs", result);
                latch.countDown();
            }

            @Override
            public void fail(Exception e) {
                System.out.println("fail:" + e.getMessage());
                latch.countDown();
            }
        });

        try {
            latch.await();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @After
    public void setTear() {
        if (rpcClient != null) {
            rpcClient.stop();
        }
    }
}

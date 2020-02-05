package com.hqs.client;

import com.hqs.HelloService;
import com.hqs.async.AsyncRPCCallback;
import com.hqs.async.RPCFuture;
import com.hqs.proxy.AsyncObjectProxy;
import com.hqs.registry.ServiceDiscovery;

import java.util.concurrent.CountDownLatch;

public class SyncCallback {

    public static void main(String[] args) {

        ServiceDiscovery serviceDiscovery = new ServiceDiscovery("127.0.0.1:2181");
        RPCClient rpcClient = new RPCClient(serviceDiscovery);

        AsyncObjectProxy asyncClient = rpcClient.createAsync(HelloService.class);

        RPCFuture future = asyncClient.call("sayHi", "hqs");

        final CountDownLatch latch = new CountDownLatch(1);

        future.addCallback(new AsyncRPCCallback() {
            @Override
            public void success(Object result) {
                System.out.println("result:" + result.toString());
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
        } finally {
            rpcClient.stop();
        }
    }

}

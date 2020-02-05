package com.hqs.client;

import com.hqs.HelloService;
import com.hqs.async.RPCFuture;
import com.hqs.proxy.AsyncObjectProxy;
import com.hqs.registry.ServiceDiscovery;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class AsyncInvoke {

    public static void main(String[] args) {

        ServiceDiscovery serviceDiscovery = new ServiceDiscovery("127.0.0.1:2181");
        RPCClient rpcClient = new RPCClient(serviceDiscovery);

        AsyncObjectProxy asyncClient = rpcClient.createAsync(HelloService.class);
        RPCFuture future = asyncClient.call("sayHi", "hqs");
        try {
            String result = (String) future.get(5, TimeUnit.SECONDS);
            System.out.println(result);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        } finally {
            rpcClient.stop();
        }

    }

}

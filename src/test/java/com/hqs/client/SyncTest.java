package com.hqs.client;

import com.hqs.HelloService;
import com.hqs.registry.ServiceDiscovery;

public class SyncTest {

    public static void main(String[] args) {
        ServiceDiscovery serviceDiscovery = new ServiceDiscovery("127.0.0.1:2181");
        RPCClient rpcClient = new RPCClient(serviceDiscovery);
        HelloService syncClient = rpcClient.create(HelloService.class);
        System.out.println(syncClient.sayHi("hqs"));
        rpcClient.stop();
    }
}

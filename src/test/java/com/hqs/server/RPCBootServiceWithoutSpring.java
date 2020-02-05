package com.hqs.server;

import com.hqs.HelloService;
import com.hqs.HelloServiceImpl;
import com.hqs.registry.ServiceRegistry;

public class RPCBootServiceWithoutSpring {

    public static void main(String[] args) {
        String serverAddress = "127.0.0.1:8888";
        ServiceRegistry serviceRegistry = new ServiceRegistry("127.0.0.1:2181");
        RPCServer rpcServer = new RPCServer(serverAddress, serviceRegistry);

        HelloService helloService = new HelloServiceImpl();
        rpcServer.addService("com.hqs.HelloService", helloService);
        try {
            rpcServer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

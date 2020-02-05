package com.hqs.client;

import com.hqs.ConnectionManager;
import com.hqs.proxy.AsyncObjectProxy;
import com.hqs.proxy.ObjectProxy;
import com.hqs.registry.ServiceDiscovery;

import java.lang.reflect.Proxy;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RPCClient {

    private ServiceDiscovery serviceDiscovery;
    private static ThreadPoolExecutor threadPoolExecutor= new ThreadPoolExecutor(16, 16,
        600L, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(65536)
    );

    public RPCClient(ServiceDiscovery serviceDiscovery) {
        this.serviceDiscovery = serviceDiscovery;
    }

    public static <T> T create(Class<T> interfaceClass) {
        return (T) Proxy.newProxyInstance(
                interfaceClass.getClassLoader(),
                new Class<?>[] {interfaceClass},
                new ObjectProxy<T>(interfaceClass)
                );
    }

    public static <T> AsyncObjectProxy createAsync(Class<T> interfaceClass) {
        return new ObjectProxy<>(interfaceClass);
    }

    public static void submit(Runnable task) {
        threadPoolExecutor.submit(task);
    }

    public void stop() {
        threadPoolExecutor.shutdown();
        serviceDiscovery.close();
        ConnectionManager.getInstance().stop();
    }
}

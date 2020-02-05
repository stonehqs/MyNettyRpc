package com.hqs.registry;

import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.util.concurrent.CountDownLatch;

public class ServiceRegistry {

    private CountDownLatch latch = new CountDownLatch(1);

    private String registryAddress;

    public ServiceRegistry(String registryAddress) {
        this.registryAddress = registryAddress;
    }

    public void register(String data) {
        if(data != null) {
            ZooKeeper zk = connectServer();
            if(zk != null) {
                AddRootNode(zk);
                createNode(zk, data);
            }
        }
    }


    private ZooKeeper connectServer() {
        ZooKeeper zk = null;

        try {
            zk = new ZooKeeper(registryAddress, Constant.ZK_SESSION_TIMEOUT, new Watcher() {
                @Override
                public void process(WatchedEvent event) {
                    if(event.getState() == Event.KeeperState.SyncConnected) {
                        latch.countDown();
                    }
                }
            });
            latch.await();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return zk;
    }

    private void AddRootNode(ZooKeeper zk) {
        try {
            Stat s =  zk.exists(Constant.ZK_REGISTRY_PATH, false);
            if(s == null) {
                zk.create(Constant.ZK_REGISTRY_PATH, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE,
                        CreateMode.PERSISTENT);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createNode(ZooKeeper zk, String data) {
        try {
            byte[] dataBytes= data.getBytes();
            String path = zk.create(Constant.ZK_DATA_PATH, dataBytes, ZooDefs.Ids.OPEN_ACL_UNSAFE,
                    CreateMode.EPHEMERAL);
            System.out.println("createNode:path" + path + " data:" + data);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

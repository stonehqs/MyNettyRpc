package com.hqs.registry;

import com.hqs.ConnectionManager;
import org.apache.zookeeper.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class ServiceDiscovery {

    private CountDownLatch latch = new CountDownLatch(1);

    private volatile List<String> dataList = new ArrayList<>();

    private String registryAddress;
    private ZooKeeper zooKeeper;

    public ServiceDiscovery(String registryAddress) {
        this.registryAddress = registryAddress;
        zooKeeper = connectServer();
        if(zooKeeper != null) {
            try {
                watchNode(zooKeeper);
            } catch (Exception e) {
                try {
                    watchNode(zooKeeper);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
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
        } catch (Exception e ) {
            e.printStackTrace();
        }
        return zk;
    }

    private void watchNode(final ZooKeeper zk) {
        try {
            List<String> nodeList = zk.getChildren(Constant.ZK_REGISTRY_PATH, new Watcher() {
                @Override
                public void process(WatchedEvent event) {
                    if(event.getType() == Event.EventType.NodeDataChanged) {
                        watchNode(zk);
                    }
                }
            });

            List<String> dataList = new ArrayList<>();
            for(String node : nodeList) {
                byte[] bytes = zk.getData(Constant.ZK_REGISTRY_PATH + "/" + node, false,
                        null);
                dataList.add(new String(bytes));
            }

            this.dataList = dataList;

            UpdateConnectServer();
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void UpdateConnectServer() {
        ConnectionManager.getInstance().UpdateConnectedServer(dataList);
    }

    public void close() {
        if(zooKeeper != null) {
            try {
                zooKeeper.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

}

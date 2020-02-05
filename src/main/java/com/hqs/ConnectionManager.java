package com.hqs;

import com.hqs.client.RPCClientHandler;
import com.hqs.client.RPCClientInitializer;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ConnectionManager {

    private volatile static ConnectionManager connectionManager;

    private EventLoopGroup eventLoopGroup = new NioEventLoopGroup(4);

    private static ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(16, 16,
            600L, TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(65536));

    private CopyOnWriteArrayList<RPCClientHandler> connectedHandlers = new CopyOnWriteArrayList<>();
    private Map<InetSocketAddress, RPCClientHandler> connectedServerNodes = new ConcurrentHashMap<>();

    private ReentrantLock lock = new ReentrantLock();
    private Condition connected = lock.newCondition();
    private long connectTimeoutMillis = 6000L;
    private AtomicInteger roundRobin = new AtomicInteger(0);
    private volatile boolean isRunning = true;

    private ConnectionManager() {}

    public static ConnectionManager getInstance() {
        if(connectionManager == null) {
            synchronized (ConnectionManager.class) {
                if(connectionManager == null) {
                    connectionManager = new ConnectionManager();
                }
            }
        }
        return connectionManager;
    }

    public void UpdateConnectedServer(List<String> allServerAddress) {
        if(allServerAddress != null) {
            if(allServerAddress.size() > 0) {
                HashSet<InetSocketAddress> newAllServerNodeSet = new HashSet<>();
                for(int i = 0; i < allServerAddress.size(); i++) {
                    String[] addresses = allServerAddress.get(i).split(":");
                    if(addresses.length == 2) {
                        String host = addresses[0];
                        int port = Integer.valueOf(addresses[1]);
                        InetSocketAddress address = new InetSocketAddress(host, port);
                        newAllServerNodeSet.add(address);
                    }
                }
                //如果有服务没有添加上，需要添加上
                for(InetSocketAddress address : newAllServerNodeSet) {
                    if(!connectedServerNodes.containsKey(address)) {
                        connectServerNode(address);
                    }
                }

                //关闭和移除不可用的server节点
                for(int i = 0; i < connectedHandlers.size(); i++) {
                    RPCClientHandler connectedHandler = connectedHandlers.get(i);
                    SocketAddress remotePeer = connectedHandler.getRemotePeer();
                    if(!newAllServerNodeSet.contains(remotePeer)) {
                        RPCClientHandler rpcClientHandler = connectedServerNodes.get(remotePeer);
                        if(rpcClientHandler != null) {
                            rpcClientHandler.close();
                        }
                        connectedServerNodes.remove(remotePeer);
                        connectedHandlers.remove(rpcClientHandler);
                    }
                }
            //没有可用的server node(也就是所有server都停了）
            } else {
                for(RPCClientHandler connectedClientHandler : connectedHandlers) {
                    SocketAddress remotePeer = connectedClientHandler.getRemotePeer();
                    RPCClientHandler handler = connectedServerNodes.get(remotePeer);
                    handler.close();
                    connectedServerNodes.remove(remotePeer);
                }
                connectedHandlers.clear();
            }
        }
    }


    private void connectServerNode(final InetSocketAddress address) {
        threadPoolExecutor.execute(new Runnable() {
            @Override
            public void run() {
                Bootstrap b = new Bootstrap();
                b.group(eventLoopGroup)
                        .channel(NioSocketChannel.class)
                        .handler(new RPCClientInitializer());
                ChannelFuture future = b.connect(address);
                future.addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        if(future.isSuccess()) {
                            System.out.println("Successfully connect to remote server. remote peer = " + address);
                            RPCClientHandler handler = future.channel().pipeline().get(RPCClientHandler.class);
                            addHandler(handler);
                        }
                    }
                });
            }
        });
    }

    private void addHandler(RPCClientHandler handler) {
        connectedHandlers.add(handler);
        InetSocketAddress remoteAddress = (InetSocketAddress) handler.getChannel().remoteAddress();
        connectedServerNodes.put(remoteAddress, handler);
        signalAvailableHandler();
    }

    private void signalAvailableHandler() {
        lock.lock();
        try {
            connected.signalAll();
        } finally {
            lock.unlock();
        }
    }

    private boolean waitingForHandler() throws InterruptedException {
        lock.lock();
        try {
            return connected.await(this.connectTimeoutMillis, TimeUnit.MILLISECONDS);
        } finally {
            lock.unlock();
        }
    }

    public RPCClientHandler chooseHandler() {
        int size = connectedHandlers.size();
        while(isRunning && size <= 0) {
            try {
                boolean available = waitingForHandler();
                if(available) {
                    size = connectedHandlers.size();
                }
            } catch (InterruptedException e) {
                System.out.println("Waiting for available node is interrupted! ");
                e.printStackTrace();
                throw new RuntimeException("Can't connect any servers!", e);
            }
        }
        int  index = (roundRobin.getAndAdd(1) + size) % size;
        return connectedHandlers.get(index);
    }

    public void stop() {
        isRunning = false;
        for(int i = 0; i < connectedHandlers.size(); i++) {
            RPCClientHandler rpcClientHandler = connectedHandlers.get(i);
            rpcClientHandler.close();
        }
        signalAvailableHandler();
        threadPoolExecutor.shutdown();
        eventLoopGroup.shutdownGracefully();
    }
}

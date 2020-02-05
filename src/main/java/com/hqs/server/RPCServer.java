package com.hqs.server;

import com.hqs.client.RPCClientHandler;
import com.hqs.codec.RPCDecoder;
import com.hqs.codec.RPCEncoder;
import com.hqs.protocol.Request;
import com.hqs.protocol.Response;
import com.hqs.registry.ServiceDiscovery;
import com.hqs.registry.ServiceRegistry;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class RPCServer implements ApplicationContextAware, InitializingBean {

    private String serverAddress;
    private ServiceRegistry serviceRegistry;

    private Map<String, Object> handlerMap = new HashMap<>();
    private static ThreadPoolExecutor threadPoolExecutor;

    private EventLoopGroup bossGroup = null;
    private EventLoopGroup workerGroup = null;

    public RPCServer(String serverAddress) {
        this.serverAddress = serverAddress;
    }

    public RPCServer(String serverAddress, ServiceRegistry serviceRegistry) {
        this.serverAddress = serverAddress;
        this.serviceRegistry = serviceRegistry;
    }


    @Override
    public void afterPropertiesSet() throws Exception {
        start();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        Map<String, Object> serverBeanMap = applicationContext.getBeansWithAnnotation(RPCService.class);
        if(!MapUtils.isEmpty(serverBeanMap)) {
            for(Object serviceBean : serverBeanMap.values()) {
               String interfaceName = serviceBean.getClass().getAnnotation(RPCService.class).value().getName();
               handlerMap.put(interfaceName, serviceBean);
            }
        }
    }

    public void start() throws InterruptedException {
        if(bossGroup == null && workerGroup == null) {
            bossGroup = new NioEventLoopGroup();
            workerGroup = new NioEventLoopGroup();
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    .addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 0))
                                    .addLast(new RPCDecoder(Request.class))
                                    .addLast(new RPCEncoder(Response.class))
                                    .addLast(new RPCServerHandler(handlerMap));

                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);

            String[] address = serverAddress.split(":");
            String host = address[0];
            int port = Integer.parseInt(address[1]);

            ChannelFuture future = serverBootstrap.bind(host, port).sync();
            System.out.println("servier 启动");
            if(serviceRegistry != null) {
                serviceRegistry.register(serverAddress);
            }

            future.channel().closeFuture().sync();
        }

    }

    public static void submit(Runnable task) {
        if(threadPoolExecutor == null) {
            synchronized (RPCServer.class) {
                if(threadPoolExecutor == null) {
                    threadPoolExecutor = new ThreadPoolExecutor(16, 16, 600L,
                            TimeUnit.SECONDS, new ArrayBlockingQueue<Runnable>(65536));
                }
            }
        }
        threadPoolExecutor.submit(task);
    }

    public RPCServer addService(String interfaceName, Object serviceBean) {
        if(!handlerMap.containsKey(interfaceName)) {
            handlerMap.put(interfaceName, serviceBean);
        }
        return this;
    }
}

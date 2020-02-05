package com.hqs.server;

import com.hqs.protocol.Request;
import com.hqs.protocol.Response;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import net.sf.cglib.reflect.FastClass;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public class RPCServerHandler extends SimpleChannelInboundHandler<Request> {

    private final Map<String, Object> handerMap ;

    public RPCServerHandler(Map<String, Object> handlerMap) {
        this.handerMap = handlerMap;
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final Request request) throws Exception {
        RPCServer.submit(new Runnable() {
            @Override
            public void run() {
                System.out.println("received requestId: " + request.getRequestId());
                Response response = new Response();
                response.setRequestId(request.getRequestId());
                try {
                    Object result = handle(request);
                    response.setResult(result);

                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
                System.out.println("result:" + response.getResult());

                ctx.writeAndFlush(response).addListener(new ChannelFutureListener() {
                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception {
                        System.out.println("Send response for request successfully:" + request.getRequestId());
                    }
                });
            }
        });
    }

    private Object handle(Request request) throws InvocationTargetException {
        String className = request.getClassName();
        Object serviceBean = handerMap.get(className);

        Class<?> serviceClass = serviceBean.getClass();
        String methodName = request.getMethodName();
        Class<?>[] parameterTypes = request.getParameterTypes();
        Object[] parameters = request.getParameters();

        System.out.println("className:" + serviceClass.getName());
        for (int i = 0; i < parameterTypes.length; ++i) {
            System.out.println(parameterTypes[i].getName());
        }
        for (int i = 0; i < parameters.length; ++i) {
            System.out.println(parameters[i].toString());
        }

        FastClass fastClass = FastClass.create(serviceClass);
        int methodIndex = fastClass.getIndex(methodName, parameterTypes);
        return fastClass.invoke(methodIndex, serviceBean, parameters);
    }
}

package com.hqs.client;

import com.hqs.codec.RPCDecoder;
import com.hqs.codec.RPCEncoder;
import com.hqs.protocol.Request;
import com.hqs.protocol.Response;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

public class RPCClientInitializer extends ChannelInitializer<SocketChannel> {
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.
                addLast(new RPCEncoder(Request.class)).
                addLast(new LengthFieldBasedFrameDecoder(65536, 0, 4, 0, 0)).
                addLast(new RPCDecoder(Response.class)).
                addLast(new RPCClientHandler());
    }
}

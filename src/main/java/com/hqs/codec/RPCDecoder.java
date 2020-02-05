package com.hqs.codec;

import com.hqs.protocol.ProtostuffUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

public class RPCDecoder extends ByteToMessageDecoder {

    private Class<?> genericClass;

    public RPCDecoder(Class<?> genericClass) {
        this.genericClass = genericClass;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        //如果读取的数据小于4，也就是小于他的长度。
        if(in.readableBytes() < 4) {
            return;
        }
        //标记下读到的位置，用于还原。
        in.markReaderIndex();
        //读取数据长度
        int dataLength = in.readInt();
        //如果可读byte小于数据长度
        if(in.readableBytes() < dataLength) {
            //重置到刚才标记的数据位置
            in.resetReaderIndex();
            return;
        }
        //声明指定数据长度的data数据
        byte[] data = new byte[dataLength];
        //将数据读取到data数据组中
        in.readBytes(data);
        //进行反序列化
        Object obj = ProtostuffUtils.deserialize(data, genericClass);
        //将结果添加到List中，用于后续处理
        out.add(obj);
    }
}

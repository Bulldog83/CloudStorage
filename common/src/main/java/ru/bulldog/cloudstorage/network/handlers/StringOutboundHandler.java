package ru.bulldog.cloudstorage.network.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import ru.bulldog.cloudstorage.data.DataBuffer;

public class StringOutboundHandler extends SimpleChannelOutboundHandler<String> {

	@Override
	public void write0(ChannelHandlerContext ctx, String message, ChannelPromise promise) throws Exception {
		DataBuffer buffer = new DataBuffer(ctx.alloc());
		buffer.writeString(message);
		ctx.writeAndFlush(message);
	}
}

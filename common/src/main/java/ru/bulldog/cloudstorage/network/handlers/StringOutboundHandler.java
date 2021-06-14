package ru.bulldog.cloudstorage.network.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.bulldog.cloudstorage.data.DataBuffer;

public class StringOutboundHandler extends SimpleChannelOutboundHandler<String> {

	private final static Logger logger = LogManager.getLogger(StringOutboundHandler.class);

	@Override
	public void write0(ChannelHandlerContext ctx, String message, ChannelPromise promise) throws Exception {
		logger.debug("Received message: " + message);
		DataBuffer buffer = new DataBuffer(ctx.alloc());
		buffer.writeString(message);
		ctx.writeAndFlush(buffer);
	}
}

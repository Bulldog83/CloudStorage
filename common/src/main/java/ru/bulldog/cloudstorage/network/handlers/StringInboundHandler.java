package ru.bulldog.cloudstorage.network.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class StringInboundHandler extends SimpleChannelInboundHandler<String> {

	protected final static Logger logger = LogManager.getLogger(StringInboundHandler.class);

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, String str) throws Exception {
		logger.info("Received message: " + str);
	}
}

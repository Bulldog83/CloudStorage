package ru.bulldog.cloudstorage.network;

import io.netty.channel.ChannelHandlerContext;
import ru.bulldog.cloudstorage.event.EventsHandler;
import ru.bulldog.cloudstorage.network.handlers.StringInboundHandler;

public class ClientStringInboundHandler extends StringInboundHandler {

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, String str) throws Exception {
		EventsHandler.getInstance().onMessageReceived(str);
		logger.debug("Received message: " + str);
	}
}

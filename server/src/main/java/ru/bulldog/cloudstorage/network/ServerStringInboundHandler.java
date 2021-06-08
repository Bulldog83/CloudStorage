package ru.bulldog.cloudstorage.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.bulldog.cloudstorage.command.ServerCommand;
import ru.bulldog.cloudstorage.network.handlers.StringInboundHandler;

import java.util.Optional;

public class ServerStringInboundHandler extends StringInboundHandler {

	private final static Logger logger = LogManager.getLogger(ServerStringInboundHandler.class);

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, String str) throws Exception {
		Optional<ServerCommand> command = ServerCommand.of(str);
		if (command.isPresent()) {
			ctx.fireChannelRead(command.get());
		} else {
			logger.info("Received message: " + str);
		}
	}
}

package ru.bulldog.cloudstorage.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.bulldog.cloudstorage.command.ServerCommand;

import java.util.Optional;

public class StringInboundHandler extends SimpleChannelInboundHandler<String> {

	private final static Logger logger = LogManager.getLogger(StringInboundHandler.class);

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

package ru.bulldog.cloudstorage.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import ru.bulldog.cloudstorage.command.ServerCommand;

import java.util.Optional;

public class StringInboundHandler extends SimpleChannelInboundHandler<String> {

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, String str) throws Exception {
		System.out.println(str);
		Optional<ServerCommand> command = ServerCommand.of(str);
		if (command.isPresent()) {
			ctx.fireChannelRead(command.get());
		} else {
			ctx.writeAndFlush(str);
		}
	}
}

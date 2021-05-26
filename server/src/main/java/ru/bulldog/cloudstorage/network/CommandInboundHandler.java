package ru.bulldog.cloudstorage.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import ru.bulldog.cloudstorage.command.ServerCommand;

import java.io.ByteArrayOutputStream;

public class CommandInboundHandler extends SimpleChannelInboundHandler<ServerCommand> {

	private final ServerNetworkHandler server;

	public CommandInboundHandler(ServerNetworkHandler server) {
		this.server = server;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, ServerCommand command) throws Exception {
		ByteArrayOutputStream response = new ByteArrayOutputStream();
		server.handleCommand(command, response);
		ByteBuf buffer = ctx.alloc().directBuffer();
		buffer.writeBytes(response.toByteArray());
		ctx.writeAndFlush(buffer);
	}
}

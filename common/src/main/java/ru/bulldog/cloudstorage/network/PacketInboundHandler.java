package ru.bulldog.cloudstorage.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import ru.bulldog.cloudstorage.network.packet.Packet;

public class PacketInboundHandler extends SimpleChannelInboundHandler<Packet> {

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Packet packet) throws Exception {

	}
}

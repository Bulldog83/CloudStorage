package ru.bulldog.cloudstorage.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import ru.bulldog.cloudstorage.network.packet.Packet;

public class PacketOutboundHandler extends SimpleChannelOutboundHandler<Packet> {

	@Override
	public void write0(ChannelHandlerContext ctx, Packet msg, ChannelPromise promise) throws Exception {

	}
}

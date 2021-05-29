package ru.bulldog.cloudstorage.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import ru.bulldog.cloudstorage.network.packet.Packet;

public class ClientPacketOutboundHandler extends PacketOutboundHandler {
	@Override
	public void write0(ChannelHandlerContext ctx, Packet packet, ChannelPromise promise) throws Exception {
		ByteBuf buffer = ctx.alloc().buffer();
		packet.write(buffer);
		ctx.writeAndFlush(buffer);
	}
}

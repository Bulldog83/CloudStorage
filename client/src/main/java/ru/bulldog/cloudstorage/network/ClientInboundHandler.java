package ru.bulldog.cloudstorage.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.bulldog.cloudstorage.network.packet.FilesListPacket;
import ru.bulldog.cloudstorage.network.packet.Packet;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class ClientInboundHandler extends ChannelInboundHandlerAdapter {

	private final static Logger logger = LogManager.getLogger(ClientInboundHandler.class);

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		super.channelActive(ctx);
		SocketAddress address = ctx.channel().remoteAddress();
		logger.info("Connected: " + address);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		super.channelInactive(ctx);
		SocketAddress address = ctx.channel().remoteAddress();
		logger.info("Disconnected: " + address);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		ByteBuf buffer = (ByteBuf) msg;
		Optional<Packet> optionalPacket = Packet.read(buffer);
		if (optionalPacket.isPresent()) {
			ctx.fireChannelRead(optionalPacket.get());
		} else {
			ctx.fireChannelRead(buffer.toString(StandardCharsets.UTF_8));
		}
	}
}

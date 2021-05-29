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

public class ServerInboundHandler extends ChannelInboundHandlerAdapter {

	private final static Logger logger = LogManager.getLogger(ServerInboundHandler.class);

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		SocketAddress address = ctx.channel().remoteAddress();
		logger.info("Connected: " + address);
		ctx.write(new FilesListPacket());
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		SocketAddress address = ctx.channel().remoteAddress();
		logger.info("Disconnected: " + address);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		ByteBuf buffer = (ByteBuf) msg;
		Optional<Packet> optionalPacket = Packet.read(buffer);
		if (optionalPacket.isPresent()) {
			System.out.println(optionalPacket);
			ctx.fireChannelRead(optionalPacket.get());
		} else {
			ctx.fireChannelRead(buffer.toString(StandardCharsets.UTF_8));
		}
	}
}

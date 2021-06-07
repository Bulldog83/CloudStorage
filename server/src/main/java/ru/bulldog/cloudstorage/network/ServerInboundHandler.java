package ru.bulldog.cloudstorage.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.bulldog.cloudstorage.data.DataBuffer;
import ru.bulldog.cloudstorage.network.packet.*;
import ru.bulldog.cloudstorage.network.packet.Packet.PacketType;

import java.net.SocketAddress;
import java.util.Optional;
import java.util.UUID;

public class ServerInboundHandler extends ChannelInboundHandlerAdapter {

	private final static Logger logger = LogManager.getLogger(ServerInboundHandler.class);
	private final ServerNetworkHandler networkHandler;

	public ServerInboundHandler(ServerNetworkHandler networkHandler) {
		this.networkHandler = networkHandler;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		Channel channel = ctx.channel();
		SocketAddress address = channel.remoteAddress();
		logger.info("Connected: " + address);
		channel.attr(ChannelAttributes.FILE_CHANNEL).set(false);
		networkHandler.registerChannel(channel);
		ctx.writeAndFlush(new AuthRequest());
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		Channel channel = ctx.channel();
		SocketAddress address = channel.remoteAddress();
		UUID sessionId = channel.attr(ChannelAttributes.SESSION_KEY).get();
		Session session = networkHandler.getSession(sessionId);
		networkHandler.disconnect(session);
		logger.info("Disconnected: " + address);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		DataBuffer buffer = new DataBuffer((ByteBuf) msg);
		Channel channel = ctx.channel();
		UUID sessionId = channel.attr(ChannelAttributes.SESSION_KEY).get();
		Session session = networkHandler.getSession(sessionId);
		if (session != null) {
			if (channel.attr(ChannelAttributes.FILE_CHANNEL).get()) {
				FileConnection fileConnection = session.getFileChannel(channel.id());
				if (fileConnection != null) {
					networkHandler.handleFile(fileConnection, buffer);
				}
			} else {
				Optional<Packet> optionalPacket = Packet.read(buffer);
				if (optionalPacket.isPresent()) {
					Packet packet = optionalPacket.get();
					ctx.fireChannelRead(packet);
				} else {
					buffer.resetReaderIndex();
					ctx.fireChannelRead(msg);
				}
			}
		} else {
			Optional<Packet> optionalPacket = Packet.read(buffer);
			if (optionalPacket.isPresent()) {
				Packet packet = optionalPacket.get();
				if (packet.getType() == PacketType.AUTH_DATA ||
					packet.getType() == PacketType.SESSION)
				{
					ctx.fireChannelRead(packet);
					return;
				}
			}
			logger.info("Unregistered connection detected: " + channel.remoteAddress());
			ctx.writeAndFlush("No registered session found.");
			channel.close();
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		Channel channel = ctx.channel();
		UUID sessionId = channel.attr(ChannelAttributes.SESSION_KEY).get();
		Session session = networkHandler.getSession(sessionId);
		if (session != null) {
			SocketAddress address = channel.remoteAddress();
			if (session.isConnected()) {
				logger.warn("Handled error: " + address, cause);
			}
			networkHandler.disconnect(session);
		} else {
			channel.close();
		}
	}
}

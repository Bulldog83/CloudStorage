package ru.bulldog.cloudstorage.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.SocketChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.bulldog.cloudstorage.data.DataBuffer;
import ru.bulldog.cloudstorage.network.packet.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.Optional;

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
		Connection connection = networkHandler.register(address, (SocketChannel) ctx.channel());
		channel.attr(Connection.SESSION_KEY).set(connection);
		ctx.write(new SessionPacket(connection.getUUID()));
		ctx.writeAndFlush(new FilesListPacket());
		logger.info("Connected: " + address);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		Channel channel = ctx.channel();
		SocketAddress address = channel.remoteAddress();
		Connection connection = channel.attr(Connection.SESSION_KEY).get();
		networkHandler.disconnect(address, connection);
		logger.info("Disconnected: " + address);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		DataBuffer buffer = new DataBuffer((ByteBuf) msg);
		Connection connection = ctx.channel().attr(Connection.SESSION_KEY).get();
		if (connection != null) {
			if (connection.isFileConnection()) {
				networkHandler.handleFile((FileConnection) connection, buffer);
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
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		Channel channel = ctx.channel();
		Connection connection = channel.attr(Connection.SESSION_KEY).get();
		if (connection != null) {
			SocketAddress address = channel.remoteAddress();
			if (connection.isConnected()) {
				logger.warn("Handled error: " + address, cause);
			}
			networkHandler.disconnect(address, connection);
		} else {
			channel.close();
		}
	}
}

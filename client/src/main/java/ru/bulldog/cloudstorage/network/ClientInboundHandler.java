package ru.bulldog.cloudstorage.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.bulldog.cloudstorage.data.DataBuffer;
import ru.bulldog.cloudstorage.network.packet.Packet;

import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class ClientInboundHandler extends ChannelInboundHandlerAdapter {

	private final static Logger logger = LogManager.getLogger(ClientInboundHandler.class);

	private final ClientNetworkHandler networkHandler;
	private DataBuffer temp;

	public ClientInboundHandler(ClientNetworkHandler networkHandler) {
		this.networkHandler = networkHandler;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		SocketAddress address = ctx.channel().remoteAddress();
		logger.debug("Connected channel: " + ctx.channel());
		logger.info("Connected to: " + address);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		SocketAddress address = ctx.channel().remoteAddress();
		logger.debug("Disconnected channel: " + ctx.channel());
		logger.info("Disconnected from: " + address);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		DataBuffer buffer;
		if (temp != null) {
			buffer = temp.merge((ByteBuf) msg);
			this.temp = null;
		} else {
			buffer = new DataBuffer((ByteBuf) msg);
		}
		Connection connection = ctx.channel().attr(Connection.SESSION_KEY).get();
		if (connection == null) connection = networkHandler.getConnection();
		if (connection.isFileConnection()) {
			networkHandler.handleFile((FileConnection) connection, buffer);
		} else {
			while (buffer.isReadable()) {
				try {
					Optional<Packet> optionalPacket = Packet.read(buffer);
					if (optionalPacket.isPresent()) {
						Packet packet = optionalPacket.get();
						ctx.fireChannelRead(packet);
						if (packet.getType().isFile()) {
							return;
						}
						buffer.markReaderIndex();
					} else {
						ctx.fireChannelRead(buffer.toString(StandardCharsets.UTF_8));
						break;
					}
				} catch (Exception ex) {
					buffer.resetReaderIndex();
					this.temp = new DataBuffer(ctx.alloc(), buffer.readableBytes());
					buffer.readBytes(temp);
					break;
				}
			}
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		logger.warn("Connection error", cause);
	}
}

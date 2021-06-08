package ru.bulldog.cloudstorage.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
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
	private DataBuffer tempBuffer;

	public ClientInboundHandler(ClientNetworkHandler networkHandler) {
		this.networkHandler = networkHandler;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		ctx.channel().attr(ChannelAttributes.FILE_CHANNEL).set(false);
		logger.info("Connected: " + ctx.channel());
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		logger.info("Disconnected: " + ctx.channel());
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		DataBuffer buffer = getBuffer((ByteBuf) msg);
		Channel channel = ctx.channel();
		if (channel.attr(ChannelAttributes.FILE_CHANNEL).get()) {
			Session session = networkHandler.getSession();
			FileConnection fileConnection = session.getFileChannel(channel.id());
			networkHandler.handleFile(fileConnection, buffer);
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
					tempBuffer = new DataBuffer(ctx.alloc(), buffer.readableBytes());
					buffer.readBytes(tempBuffer);
					break;
				}
			}
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		logger.warn("Connection error", cause);
	}

	private DataBuffer getBuffer(ByteBuf msg) {
		if (tempBuffer != null) {
			DataBuffer buffer = tempBuffer.merge(msg);
			tempBuffer = null;
			return buffer;
		}
		return new DataBuffer(msg);
	}
}

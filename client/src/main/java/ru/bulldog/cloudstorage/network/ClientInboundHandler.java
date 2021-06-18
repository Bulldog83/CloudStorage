package ru.bulldog.cloudstorage.network;

import com.google.common.collect.Maps;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.bulldog.cloudstorage.data.DataBuffer;
import ru.bulldog.cloudstorage.event.EventsHandler;
import ru.bulldog.cloudstorage.network.packet.Packet;

import java.util.Map;
import java.util.Optional;

public class ClientInboundHandler extends ChannelInboundHandlerAdapter {

	private final static Logger logger = LogManager.getLogger(ClientInboundHandler.class);

	private final ClientNetworkHandler networkHandler;
	private final EventsHandler eventsHandler;
	private final Map<ChannelId, DataBuffer> channelBuffers = Maps.newConcurrentMap();

	public ClientInboundHandler(ClientNetworkHandler networkHandler) {
		this.eventsHandler = EventsHandler.getInstance();
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
		Channel channel = ctx.channel();
		ChannelId channelId = channel.id();
		DataBuffer buffer = getBuffer(channelId, (ByteBuf) msg);
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
					} else {
						buffer.resetReaderIndex();
						ctx.fireChannelRead(buffer.readString());
					}
					buffer.markReaderIndex();
				} catch (Exception ex) {
					buffer.resetReaderIndex();
					DataBuffer tempBuffer = new DataBuffer(ctx.alloc(), buffer.readableBytes());
					channelBuffers.put(channelId, tempBuffer);
					buffer.readBytes(tempBuffer);
					break;
				}
			}
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		eventsHandler.onHandleError(cause.getMessage());
		logger.warn("Connection error", cause);
	}

	private DataBuffer getBuffer(ChannelId channelId, ByteBuf msg) {
		if (channelBuffers.containsKey(channelId)) {
			DataBuffer buffer = channelBuffers.get(channelId).merge(msg);
			channelBuffers.remove(channelId);
			return buffer;
		}
		return new DataBuffer(msg);
	}
}

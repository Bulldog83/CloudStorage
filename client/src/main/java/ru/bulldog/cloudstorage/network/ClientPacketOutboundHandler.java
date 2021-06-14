package ru.bulldog.cloudstorage.network;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.stream.ChunkedFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.bulldog.cloudstorage.data.DataBuffer;
import ru.bulldog.cloudstorage.event.EventsHandler;
import ru.bulldog.cloudstorage.network.handlers.PacketOutboundHandler;
import ru.bulldog.cloudstorage.network.packet.FilePacket;
import ru.bulldog.cloudstorage.network.packet.FileRequest;
import ru.bulldog.cloudstorage.network.packet.Packet;
import ru.bulldog.cloudstorage.network.packet.SessionPacket;
import ru.bulldog.cloudstorage.tasks.NamedTask;
import ru.bulldog.cloudstorage.tasks.ThreadManager;

import java.io.RandomAccessFile;

public class ClientPacketOutboundHandler extends PacketOutboundHandler {
	private final static Logger logger = LogManager.getLogger(ClientPacketOutboundHandler.class);

	private final ClientNetworkHandler networkHandler;
	private final ThreadManager threadManager;
	private final EventsHandler eventsHandler;

	public ClientPacketOutboundHandler(ClientNetworkHandler networkHandler) {
		this.threadManager = networkHandler.getThreadManager();
		this.eventsHandler = EventsHandler.getInstance();
		this.networkHandler = networkHandler;
	}

	@Override
	public void write0(ChannelHandlerContext ctx, Packet packet, ChannelPromise promise) throws Exception {
		logger.debug("Received packet: " + packet);
		switch (packet.getType()) {
			case FILE:
				handleFile((FilePacket) packet);
				return;
			case FILE_REQUEST:
				handleFileRequest((FileRequest) packet);
				return;
		}
		DataBuffer buffer = new DataBuffer(ctx.alloc());
		packet.write(buffer);
		ctx.writeAndFlush(buffer);
	}

	private void handleFile(FilePacket packet) {
		if (packet.isEmpty()) return;
		threadManager.execute(new NamedTask("FileUpload", () -> {
			Channel channel = networkHandler.openChannel();
			DataBuffer buffer = new DataBuffer(channel.alloc());
			try {
				RandomAccessFile raFile = new RandomAccessFile(packet.getFile(), "rw");
				packet.write(buffer);
				channel.write(buffer);
				channel.writeAndFlush(new ChunkedFile(raFile));
				eventsHandler.onFileStart("Upload", packet.getName());
			} catch (Exception ex) {
				logger.error("Upload file error: " + packet.getFile(), ex);
			}
		}, false));
	}

	private void handleFileRequest(FileRequest packet) {
		threadManager.execute(new NamedTask("FileRequest", () -> {
			Channel channel = networkHandler.openChannel();
			DataBuffer buffer = new DataBuffer(channel.alloc());
			try {
				packet.write(buffer);
				channel.writeAndFlush(buffer);
			} catch (Exception ex) {
				logger.error("Request file error: " + packet.getName(), ex);
			}
		}, false));
	}
}

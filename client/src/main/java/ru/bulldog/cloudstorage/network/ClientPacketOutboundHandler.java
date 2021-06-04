package ru.bulldog.cloudstorage.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.stream.ChunkedFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.bulldog.cloudstorage.data.DataBuffer;
import ru.bulldog.cloudstorage.network.packet.FilePacket;
import ru.bulldog.cloudstorage.network.packet.FileRequest;
import ru.bulldog.cloudstorage.network.packet.Packet;

import java.io.RandomAccessFile;

public class ClientPacketOutboundHandler extends PacketOutboundHandler {
	private final static Logger logger = LogManager.getLogger(ClientPacketOutboundHandler.class);

	private final ClientNetworkHandler networkHandler;

	public ClientPacketOutboundHandler(ClientNetworkHandler networkHandler) {
		this.networkHandler = networkHandler;
	}

	@Override
	public void write0(ChannelHandlerContext ctx, Packet packet, ChannelPromise promise) throws Exception {
		logger.debug("Received packet: " + packet);
		switch (packet.getType()) {
			case FILE:
				handleFile(ctx, (FilePacket) packet);
				return;
			case FILE_REQUEST:
				handleFileRequest((FileRequest) packet);
				return;
		}
		DataBuffer buffer = new DataBuffer(ctx.alloc());
		packet.write(buffer);
		ctx.writeAndFlush(buffer);
	}

	private void handleFile(ChannelHandlerContext ctx, FilePacket packet) {
		if (packet.isEmpty()) return;
		DataBuffer buffer = new DataBuffer(ctx.alloc());
		try {
			packet.write(buffer);
			ctx.write(buffer);
			RandomAccessFile raFile = new RandomAccessFile(packet.getFile(), "rw");
			ctx.writeAndFlush(new ChunkedFile(raFile));
			networkHandler.getController().startTransfer("Отправляю", packet.getName());
		} catch (Exception ex) {
			logger.error("Upload file error: " + packet.getFile(), ex);
		}
	}

	private void handleFileRequest(FileRequest packet) throws Exception {
		Channel channel = networkHandler.openChannel();
		DataBuffer buffer = new DataBuffer(channel.alloc());
		packet.write(buffer);
		channel.writeAndFlush(buffer);
	}
}

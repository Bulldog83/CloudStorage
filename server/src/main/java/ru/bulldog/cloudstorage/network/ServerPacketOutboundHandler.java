package ru.bulldog.cloudstorage.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.stream.ChunkedFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.bulldog.cloudstorage.data.DataBuffer;
import ru.bulldog.cloudstorage.network.packet.FilePacket;
import ru.bulldog.cloudstorage.network.packet.FilesListPacket;
import ru.bulldog.cloudstorage.network.packet.Packet;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class ServerPacketOutboundHandler extends PacketOutboundHandler {

	private final static Logger logger = LogManager.getLogger(ServerPacketOutboundHandler.class);
	private final ServerNetworkHandler networkHandler;

	public ServerPacketOutboundHandler(ServerNetworkHandler networkHandler) {
		this.networkHandler = networkHandler;
	}

	@Override
	public void write0(ChannelHandlerContext ctx, Packet packet, ChannelPromise promise) throws Exception {
		logger.debug("Received packet: " + packet);
		switch (packet.getType()) {
			case FILES_LIST:
				handleFilesList((FilesListPacket) packet);
				break;
			case FILE:
				handleFile(ctx, (FilePacket) packet);
				return;
		}
		DataBuffer buffer = new DataBuffer(ctx.alloc());
		packet.write(buffer);
		ctx.writeAndFlush(buffer);
		buffer.clear();
	}

	private void handleFilesList(FilesListPacket packet) {
		try {
			Path filesDir = networkHandler.getFilesDir();
			List<String> filesNames = Files.list(filesDir)
					.map(file -> file.getFileName().toString())
					.collect(Collectors.toList());
			packet.addAll(filesNames);
		} catch (Exception ex) {
			logger.error(ex.getLocalizedMessage(), ex);
		}
	}

	private void handleFile(ChannelHandlerContext ctx, FilePacket packet) {
		if (packet.isEmpty()) return;
		try {
			DataBuffer buffer = new DataBuffer(ctx.alloc());
			packet.write(buffer);
			ctx.write(buffer);
			ctx.writeAndFlush(new ChunkedFile(packet.getFile()));
		} catch (Exception ex) {
			logger.error("Error send file: " + packet.getFile());
		}
	}
}

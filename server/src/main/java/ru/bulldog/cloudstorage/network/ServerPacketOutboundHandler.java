package ru.bulldog.cloudstorage.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.stream.ChunkedFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.bulldog.cloudstorage.network.packet.FilePacket;
import ru.bulldog.cloudstorage.network.packet.FilesListPacket;
import ru.bulldog.cloudstorage.network.packet.Packet;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

public class ServerPacketOutboundHandler extends PacketOutboundHandler {

	private final static Logger logger = LogManager.getLogger(ServerPacketOutboundHandler.class);
	private final static Path filesDir;
	private final ServerNetworkHandler networkHandler;

	public ServerPacketOutboundHandler(ServerNetworkHandler networkHandler) {
		this.networkHandler = networkHandler;
	}

	@Override
	public void write0(ChannelHandlerContext ctx, Packet packet, ChannelPromise promise) throws Exception {
		logger.debug("Received packet: " + packet.getType());
		switch (packet.getType()) {
			case FILES_LIST:
				handleFilesList((FilesListPacket) packet);
				break;
			case FILE:
				handleFile(ctx, (FilePacket) packet);
				return;
		}
		ByteBuf buffer = ctx.alloc().buffer();
		packet.write(buffer);
		ctx.write(buffer);
	}

	private void handleFilesList(FilesListPacket packet) {
		try {
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
			ByteBuf buffer = ctx.alloc().buffer();
			packet.write(buffer);
			ctx.write(buffer);
			ctx.writeAndFlush(new ChunkedFile(packet.getFile()));
		} catch (Exception ex) {
			logger.error("Error send file: " + packet.getFile());
		}
	}

	static {
		File dirFiles = new File("files");
		if (!dirFiles.exists() && !dirFiles.mkdirs()) {
			logger.error("Can't create files dir.");
		}
		filesDir = dirFiles.toPath();
	}
}

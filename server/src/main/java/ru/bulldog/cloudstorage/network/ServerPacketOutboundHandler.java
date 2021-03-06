package ru.bulldog.cloudstorage.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.stream.ChunkedFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.bulldog.cloudstorage.data.DataBuffer;
import ru.bulldog.cloudstorage.data.FileInfo;
import ru.bulldog.cloudstorage.data.FileSystem;
import ru.bulldog.cloudstorage.network.handlers.PacketOutboundHandler;
import ru.bulldog.cloudstorage.network.packet.FilePacket;
import ru.bulldog.cloudstorage.network.packet.FilesListPacket;
import ru.bulldog.cloudstorage.network.packet.Packet;

import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
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
				handleFilesList(ctx, (FilesListPacket) packet);
				break;
			case FILE:
				handleFile(ctx, (FilePacket) packet);
				return;
		}
		DataBuffer buffer = new DataBuffer(ctx.alloc());
		packet.write(buffer);
		ctx.writeAndFlush(buffer);
	}

	private void handleFilesList(ChannelHandlerContext ctx, FilesListPacket packet) {
		try {
			UUID sessionId = ctx.channel().attr(ChannelAttributes.SESSION_KEY).get();
			Session session = networkHandler.getSession(sessionId);
			String rootPath = session.getRootFolder().toString();
			Path filesDir = session.getActiveFolder();
			packet.setFolder(filesDir.toString().replace(rootPath, "." + FileSystem.PATH_DELIMITER));
			List<FileInfo> filesNames = Files.list(filesDir).map(FileInfo::new).collect(Collectors.toList());
			packet.addAll(filesNames);
		} catch (Exception ex) {
			logger.error(ex.getMessage(), ex);
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

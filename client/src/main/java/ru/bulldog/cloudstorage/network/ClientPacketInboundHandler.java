package ru.bulldog.cloudstorage.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.SocketChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.bulldog.cloudstorage.network.packet.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class ClientPacketInboundHandler extends PacketInboundHandler {

	private final static Logger logger = LogManager.getLogger(ClientPacketInboundHandler.class);

	private final ClientNetworkHandler networkHandler;

	public ClientPacketInboundHandler(ClientNetworkHandler networkHandler) {
		this.networkHandler = networkHandler;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Packet packet) throws Exception {
		logger.debug("Received packet: " + packet);
		switch (packet.getType()) {
			case FILES_LIST:
				handleFilesList((FilesListPacket) packet);
				break;
			case FILE_PROGRESS:
				handleFileProgress((FileProgressPacket) packet);
				break;
			case SESSION:
				handleSessionPacket(ctx, (SessionPacket) packet);
				break;
			case FILE:
				handleFilePacket(ctx, (FilePacket) packet);
				break;
		}
	}

	private void handleSessionPacket(ChannelHandlerContext ctx, SessionPacket packet) {
		Channel channel = ctx.channel();
		Connection connection = new Connection((SocketChannel) channel, packet.getSession());
		channel.attr(Connection.SESSION_KEY).set(connection);
		networkHandler.setSession(connection);
	}

	private void handleFileProgress(FileProgressPacket packet) {
		double progress = packet.getProgress();
		if (progress < 1.0) {
			networkHandler.getController().setClientProgress(progress);
		} else {
			networkHandler.getController().resetClientProgress();
		}
	}

	private void handleFilesList(FilesListPacket packet) {
		List<String> names = packet.getNames();
		networkHandler.getController().refreshServerFiles(names);
	}

	private void handleFilePacket(ChannelHandlerContext ctx, FilePacket packet) throws Exception {
		String fileName = packet.getName();
		File file = networkHandler.getFilesDir().resolve(fileName).toFile();
		if (file.exists()) {
			logger.warn("File exists: " + file + ". Will recreate file.");
			file.delete();
		}
		SocketChannel channel = (SocketChannel) ctx.channel();
		logger.debug("Current channel: " + channel);
		Connection connection = networkHandler.getConnection();
		logger.debug("Main channel: " + connection.getChannel());
		ReceivingFile receivingFile = new ReceivingFile(file, packet.getSize());
		FileConnection fileConnection = new FileConnection(connection, channel, receivingFile);
		channel.attr(Connection.SESSION_KEY).set(fileConnection);
		networkHandler.handleFile(fileConnection, packet.getBuffer());
	}
}

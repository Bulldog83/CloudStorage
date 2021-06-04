package ru.bulldog.cloudstorage.network;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.SocketChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.bulldog.cloudstorage.network.packet.*;

import java.io.File;
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
		Connection connection = networkHandler.getConnection();
		if (channel.equals(connection.getChannel())) {
			channel.attr(Connection.SESSION_KEY).set(connection);
			connection.setSessionId(packet.getSession());
		}
	}

	private void handleFileProgress(FileProgressPacket packet) {
		double progress = packet.getProgress();
		if (progress < 1.0) {
			networkHandler.getController().updateProgress(progress);
		} else {
			networkHandler.getController().stopTransfer();
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
		Connection connection = networkHandler.getConnection();
		ReceivingFile receivingFile = new ReceivingFile(file, packet.getSize());
		FileConnection fileConnection = new FileConnection(connection, channel, receivingFile);
		channel.attr(Connection.SESSION_KEY).set(fileConnection);
		networkHandler.getController().startTransfer("Принимаю", fileName);
		networkHandler.handleFile(fileConnection, packet.getBuffer());
	}
}

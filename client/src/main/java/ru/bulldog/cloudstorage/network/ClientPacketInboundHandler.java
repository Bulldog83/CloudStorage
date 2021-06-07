package ru.bulldog.cloudstorage.network;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.SocketChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.bulldog.cloudstorage.network.handlers.PacketInboundHandler;
import ru.bulldog.cloudstorage.network.packet.*;

import java.io.File;
import java.util.List;
import java.util.UUID;

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
			case AUTH_REQUEST:
				handleAuthRequest(ctx);
				break;
		}
	}

	private void handleAuthRequest(ChannelHandlerContext ctx) {
		Session session = networkHandler.getSession();
		if (session == null) {
			ctx.writeAndFlush(new AuthData("login", "password"));
			logger.debug("Auth data sent to: " + ctx.channel().remoteAddress());
		}
	}

	private void handleSessionPacket(ChannelHandlerContext ctx, SessionPacket packet) {
		UUID sessionId = packet.getSession();
		ctx.channel().attr(ChannelAttributes.SESSION_KEY).set(sessionId);
		networkHandler.setSession(sessionId);
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
		Channel channel = ctx.channel();
		Session session = networkHandler.getSession();
		channel.attr(ChannelAttributes.FILE_CHANNEL).set(true);
		ReceivingFile receivingFile = new ReceivingFile(file, packet.getSize());
		FileConnection fileConnection = new FileConnection(session.getSessionId(), channel, receivingFile);
		session.addFileChannel(channel.id(), fileConnection);
		networkHandler.getController().startTransfer("Download", fileName);
		networkHandler.handleFile(fileConnection, packet.getBuffer());
	}
}

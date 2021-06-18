package ru.bulldog.cloudstorage.network;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.bulldog.cloudstorage.event.EventsHandler;
import ru.bulldog.cloudstorage.network.handlers.PacketInboundHandler;
import ru.bulldog.cloudstorage.network.packet.*;

import java.io.File;
import java.util.List;
import java.util.UUID;

public class ClientPacketInboundHandler extends PacketInboundHandler {

	private final static Logger logger = LogManager.getLogger(ClientPacketInboundHandler.class);

	private final ClientNetworkHandler networkHandler;
	private final EventsHandler eventsHandler;

	public ClientPacketInboundHandler(ClientNetworkHandler networkHandler) {
		this.eventsHandler = EventsHandler.getInstance();
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
		if (networkHandler.hasSession()) {
			Session session = networkHandler.getSession();
			UUID sessionId = session.getSessionId();
			ctx.channel().attr(ChannelAttributes.SESSION_KEY).set(sessionId);
			ctx.writeAndFlush(new SessionPacket(sessionId));
		} else {
			ctx.writeAndFlush(networkHandler.getAuthData());
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
			eventsHandler.onFileProgress(progress);
		} else {
			eventsHandler.onFileReceived();
		}
	}

	private void handleFilesList(FilesListPacket packet) {
		eventsHandler.onFilesList(packet);
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
		FileConnection fileConnection = new FileConnection(channel, session.getSessionId(), receivingFile);
		session.addFileChannel(channel.id(), fileConnection);
		eventsHandler.onFileStart("Download", fileName);
		networkHandler.handleFile(fileConnection, packet.getBuffer());
	}
}

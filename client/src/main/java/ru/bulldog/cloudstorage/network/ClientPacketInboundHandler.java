package ru.bulldog.cloudstorage.network;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.SocketChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.bulldog.cloudstorage.gui.controllers.MainController;
import ru.bulldog.cloudstorage.network.packet.FileProgressPacket;
import ru.bulldog.cloudstorage.network.packet.FilesListPacket;
import ru.bulldog.cloudstorage.network.packet.Packet;
import ru.bulldog.cloudstorage.network.packet.SessionPacket;

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
		logger.debug("Received packet: " + packet.getType());
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
		}
	}

	private void handleSessionPacket(ChannelHandlerContext ctx, SessionPacket packet) {
		Session session = new Session((SocketChannel) ctx.channel(), packet.getSession());
		networkHandler.setSession(session);
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
}

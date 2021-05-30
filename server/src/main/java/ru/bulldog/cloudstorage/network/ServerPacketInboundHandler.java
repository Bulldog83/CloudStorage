package ru.bulldog.cloudstorage.network;

import io.netty.channel.ChannelHandlerContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.bulldog.cloudstorage.network.packet.FilePacket;
import ru.bulldog.cloudstorage.network.packet.FilesListPacket;
import ru.bulldog.cloudstorage.network.packet.Packet;

import java.net.SocketAddress;
import java.nio.file.Path;
import java.util.Optional;

public class ServerPacketInboundHandler extends PacketInboundHandler {
	private final static Logger logger = LogManager.getLogger(ServerPacketInboundHandler.class);

	private final ServerNetworkHandler networkHandler;

	public ServerPacketInboundHandler(ServerNetworkHandler networkHandler) {
		this.networkHandler = networkHandler;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Packet packet) throws Exception {
		logger.debug("Received: " + packet.getType());
		switch (packet.getType()) {
			case LIST_REQUEST:
				ctx.writeAndFlush(new FilesListPacket());
				break;
			case FILE:
				handleFile(ctx, (FilePacket) packet);
				break;
		}
	}

	private void handleFile(ChannelHandlerContext ctx, FilePacket packet) {
		SocketAddress address = ctx.channel().remoteAddress();
		Optional<ClientConnection> clientConnection = networkHandler.getConnection(address);
		clientConnection.ifPresent(connection -> {
			Path filesDir = networkHandler.getFilesDir();
			Path waitingFile = filesDir.resolve(packet.getName());
			connection.waitFile(waitingFile.toFile());
			logger.debug(address + " waiting file: " + waitingFile);
		});
	}
}

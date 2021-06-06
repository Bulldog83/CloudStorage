package ru.bulldog.cloudstorage.network;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.SocketChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.bulldog.cloudstorage.network.handlers.PacketInboundHandler;
import ru.bulldog.cloudstorage.network.packet.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;
import java.util.UUID;

public class ServerPacketInboundHandler extends PacketInboundHandler {
	private final static Logger logger = LogManager.getLogger(ServerPacketInboundHandler.class);

	private final ServerNetworkHandler networkHandler;

	public ServerPacketInboundHandler(ServerNetworkHandler networkHandler) {
		this.networkHandler = networkHandler;
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Packet packet) throws Exception {
		logger.debug("Received packet: " + packet);
		switch (packet.getType()) {
			case LIST_REQUEST:
				ctx.writeAndFlush(new FilesListPacket());
				break;
			case FILE_REQUEST:
				handleFileRequest(ctx, (FileRequest) packet);
				break;
			case FILE:
				handleFilePacket(ctx, (FilePacket) packet);
				break;
		}
	}

	private void handleFilePacket(ChannelHandlerContext ctx, FilePacket packet) throws Exception {
		Channel channel = ctx.channel();
		Optional<Connection> connectionOptional = networkHandler.getConnection(packet.getSession());
		if (connectionOptional.isPresent()) {
			String fileName = packet.getName();
			File file = networkHandler.getFilesDir().resolve(fileName).toFile();
			if (file.exists()) {
				logger.warn("File exists: " + file + ". Will recreate file.");
				file.delete();
			}
			ReceivingFile receivingFile = new ReceivingFile(file, packet.getSize());
			Connection channelConnection = channel.attr(Connection.SESSION_KEY).get();
			if (channelConnection != null) {
				Connection connection = connectionOptional.get();
				UUID channelId = channelConnection.getUUID();
				connection.registerChannel(channelId, channel);
				networkHandler.unregister(channelId);
				FileConnection fileConnection = new FileConnection(channelId, connection, (SocketChannel) channel, receivingFile);
				channel.attr(Connection.SESSION_KEY).set(fileConnection);
				networkHandler.handleFile(fileConnection, packet.getBuffer());
			}
		} else {
			ctx.writeAndFlush("No registered session found.");
			channel.close();
		}
	}

	private void handleFileRequest(ChannelHandlerContext ctx, FileRequest packet) throws IOException {
		String fileName = packet.getName();
		Files.list(networkHandler.getFilesDir()).forEach(file -> {
			try {
				if (fileName.equals(file.getFileName().toString())) {
					FilePacket filePacket = new FilePacket(packet.getSession(), file);
					ctx.writeAndFlush(filePacket);
				}
			} catch (IOException ex) {
				logger.error(ex.getLocalizedMessage(), ex);
			}
		});
	}
}

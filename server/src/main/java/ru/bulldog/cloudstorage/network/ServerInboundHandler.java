package ru.bulldog.cloudstorage.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.SocketChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.bulldog.cloudstorage.network.packet.FilePacket;
import ru.bulldog.cloudstorage.network.packet.FilesListPacket;
import ru.bulldog.cloudstorage.network.packet.Packet;
import ru.bulldog.cloudstorage.network.packet.ReceivingFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.Optional;

public class ServerInboundHandler extends ChannelInboundHandlerAdapter {

	private final static Logger logger = LogManager.getLogger(ServerInboundHandler.class);
	private final ServerNetworkHandler networkHandler;

	public ServerInboundHandler(ServerNetworkHandler networkHandler) {
		this.networkHandler = networkHandler;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		SocketAddress address = ctx.channel().remoteAddress();
		networkHandler.register(address, (SocketChannel) ctx.channel());
		logger.info("Connected: " + address);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		SocketAddress address = ctx.channel().remoteAddress();
		networkHandler.disconnect(address);
		logger.info("Disconnected: " + address);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		ByteBuf buffer = (ByteBuf) msg;
		SocketAddress address = ctx.channel().remoteAddress();
		Optional<Connection> clientConnection = networkHandler.getConnection(address);
		if (clientConnection.isPresent()) {
			Connection connection = clientConnection.get();
			if (connection.isReceiving()) {
				Optional<ReceivingFile> receivingFile = connection.getReceivingFile();
				receivingFile.ifPresent(file -> {
					try {
						handleReceivingFile(connection, file, buffer);
					} catch (IOException ex) {
						logger.error("File receive error " + file, ex);
					}
				});
			} else {
				Optional<Packet> optionalPacket = Packet.read(buffer);
				if (optionalPacket.isPresent()) {
					Packet packet = optionalPacket.get();
					ctx.fireChannelRead(packet);
					if (packet.getType() == Packet.PacketType.FILE) {
						buffer.markReaderIndex();
						handleReceivingFile(connection, (FilePacket) packet, buffer);
					}
				} else {
					buffer.resetReaderIndex();
					ctx.fireChannelRead(msg);
				}
			}
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		SocketAddress address = ctx.channel().remoteAddress();
		Optional<Connection> clientConnection = networkHandler.getConnection(address);
		clientConnection.ifPresent(connection -> {
			if (connection.isConnected()) {
				logger.warn("Handled error: " + address, cause);
			}
			networkHandler.disconnect(address);
		});
	}

	private void handleReceivingFile(Connection connection, FilePacket packet, ByteBuf buffer) throws IOException {
		String fileName = packet.getName();
		Path filePath = networkHandler.getFilesDir().resolve(fileName);
		File file = filePath.toFile();
		if (file.exists()) {
			logger.warn("File exists: " + file + ". Will recreate file.");
			file.delete();
		}
		ReceivingFile receivingFile = new ReceivingFile(file, packet.getSize());
		connection.setReceivingFile(receivingFile);
		handleReceivingFile(connection, receivingFile, buffer);
	}

	private void handleReceivingFile(Connection connection, ReceivingFile receivingFile, ByteBuf buffer) throws IOException {
		File file = receivingFile.getFile();
		try (FileOutputStream fos = new FileOutputStream(file, true)) {
			FileChannel fileChannel = fos.getChannel();
			ByteBuffer nioBuffer = buffer.nioBuffer();
			while (nioBuffer.hasRemaining()) {
				receivingFile.receive(nioBuffer.remaining());
				fileChannel.write(nioBuffer);
			}
			if (receivingFile.toReceive() == 0) {
				connection.fileReceived();
				logger.debug("Received file: " + file);
				connection.sendPacket(new FilesListPacket());
			}
		}
	}
}

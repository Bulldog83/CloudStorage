package ru.bulldog.cloudstorage.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.SocketChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.bulldog.cloudstorage.network.packet.*;

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
		Optional<Session> clientConnection = networkHandler.getConnection(address);
		if (clientConnection.isPresent()) {
			Session session = clientConnection.get();
			if (session.isReceiving()) {
				Optional<ReceivingFile> receivingFile = session.getReceivingFile();
				receivingFile.ifPresent(file -> {
					try {
						handleReceivingFile(session, file, buffer);
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
						handleReceivingFile(session, (FilePacket) packet, buffer);
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
		Optional<Session> clientConnection = networkHandler.getConnection(address);
		clientConnection.ifPresent(connection -> {
			if (connection.isConnected()) {
				logger.warn("Handled error: " + address, cause);
			}
			networkHandler.disconnect(address);
		});
	}

	private void handleReceivingFile(Session session, FilePacket packet, ByteBuf buffer) throws IOException {
		String fileName = packet.getName();
		Path filePath = networkHandler.getFilesDir().resolve(fileName);
		File file = filePath.toFile();
		if (file.exists()) {
			logger.warn("File exists: " + file + ". Will recreate file.");
			file.delete();
		}
		ReceivingFile receivingFile = new ReceivingFile(file, packet.getSize());
		session.setReceivingFile(receivingFile);
		handleReceivingFile(session, receivingFile, buffer);
	}

	private void handleReceivingFile(Session session, ReceivingFile receivingFile, ByteBuf buffer) throws IOException {
		File file = receivingFile.getFile();
		try (FileOutputStream fos = new FileOutputStream(file, true)) {
			FileChannel fileChannel = fos.getChannel();
			ByteBuffer nioBuffer = buffer.nioBuffer();
			while (nioBuffer.hasRemaining()) {
				receivingFile.receive(nioBuffer.remaining());
				fileChannel.write(nioBuffer);
				long received = receivingFile.getReceived();
				double progress = (double) received / receivingFile.getSize();
				session.sendPacket(new FileProgressPacket(progress));
			}
			if (receivingFile.toReceive() == 0) {
				session.fileReceived();
				logger.debug("Received file: " + file);
				session.sendPacket(new FilesListPacket());
			}
		}
	}
}

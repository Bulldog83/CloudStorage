package ru.bulldog.cloudstorage.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.bulldog.cloudstorage.data.DataBuffer;
import ru.bulldog.cloudstorage.gui.controllers.MainController;
import ru.bulldog.cloudstorage.network.packet.FilePacket;
import ru.bulldog.cloudstorage.network.packet.Packet;
import ru.bulldog.cloudstorage.network.packet.ReceivingFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;

public class ClientInboundHandler extends ChannelInboundHandlerAdapter {

	private final static Logger logger = LogManager.getLogger(ClientInboundHandler.class);

	private final ClientNetworkHandler networkHandler;

	public ClientInboundHandler(ClientNetworkHandler networkHandler) {
		this.networkHandler = networkHandler;
	}

	@Override
	public void channelActive(ChannelHandlerContext ctx) throws Exception {
		super.channelActive(ctx);
		SocketAddress address = ctx.channel().remoteAddress();
		logger.info("Connected to: " + address);
	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {
		super.channelInactive(ctx);
		SocketAddress address = ctx.channel().remoteAddress();
		logger.info("Disconnected from: " + address);
	}

	@Override
	public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
		DataBuffer buffer = new DataBuffer((ByteBuf) msg);
		Connection connection = networkHandler.getConnection();
		if (connection.isFileConnection()) {
//			Optional<ReceivingFile> receivingFile = connection.getReceivingFile();
//			receivingFile.ifPresent(file -> {
//				try {
//					handleReceivingFile(connection, file, buffer);
//				} catch (IOException ex) {
//					logger.error("File receive error " + file, ex);
//				}
//			});
		} else {
			while (buffer.isReadable()) {
				Optional<Packet> optionalPacket = Packet.read(buffer);
				if (optionalPacket.isPresent()) {
					Packet packet = optionalPacket.get();
					ctx.fireChannelRead(packet);
					if (packet.getType() == Packet.PacketType.FILE) {
						buffer.markReaderIndex();
						handleReceivingFile(connection, (FilePacket) packet, buffer);
						break;
					}
				} else{
					ctx.fireChannelRead(buffer.toString(StandardCharsets.UTF_8));
					break;
				}
			}
		}
	}

	private void handleReceivingFile(Connection connection, FilePacket packet, ByteBuf buffer) throws IOException {
		String fileName = packet.getName();
		Path filePath = networkHandler.getController().getFilesDir().resolve(fileName);
		File file = filePath.toFile();
		if (file.exists()) {
			logger.warn("File exists: " + file + ". Will recreate file.");
			file.delete();
		}
		ReceivingFile receivingFile = new ReceivingFile(file, packet.getSize());
		//connection.setReceivingFile(receivingFile);
		handleReceivingFile(connection, receivingFile, buffer);
	}

	private void handleReceivingFile(Connection connection, ReceivingFile receivingFile, ByteBuf buffer) throws IOException {
		File file = receivingFile.getFile();
		try (FileOutputStream fos = new FileOutputStream(file, true)) {
			FileChannel fileChannel = fos.getChannel();
			ByteBuffer nioBuffer = buffer.nioBuffer();
			MainController controller = networkHandler.getController();
			while (nioBuffer.hasRemaining()) {
				receivingFile.receive(nioBuffer.remaining());
				fileChannel.write(nioBuffer);
				long received = receivingFile.getReceived();
				double progress = (double) received / receivingFile.getSize();
				controller.setServerProgress(progress);
			}
			if (receivingFile.toReceive() == 0) {
				//connection.fileReceived();
				logger.debug("Received file: " + file);
				controller.resetServerProgress();
				controller.refreshClientFiles();
			}
		}
	}
}

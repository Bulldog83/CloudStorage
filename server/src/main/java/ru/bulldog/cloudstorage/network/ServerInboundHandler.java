package ru.bulldog.cloudstorage.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.stream.ChunkedFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.bulldog.cloudstorage.network.packet.FilePacket;
import ru.bulldog.cloudstorage.network.packet.FilesListPacket;
import ru.bulldog.cloudstorage.network.packet.Packet;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
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
		networkHandler.register(address, ctx.channel());
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
		Optional<Packet> optionalPacket = Packet.read(buffer);
		if (optionalPacket.isPresent()) {
			Packet packet = optionalPacket.get();
			if (packet.getType() == Packet.PacketType.FILE) {
				buffer.markReaderIndex();
				handleReceivedFile(ctx, (FilePacket) packet, buffer);
			} else {
				ctx.fireChannelRead(optionalPacket.get());
			}
		} else {
			buffer.resetReaderIndex();
			ctx.fireChannelRead(msg);
		}
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		SocketAddress address = ctx.channel().remoteAddress();
		Optional<ClientConnection> clientConnection = networkHandler.getConnection(address);
		clientConnection.ifPresent(connection -> {
			if (connection.isConnected()) {
				logger.warn("Handled error: " + address, cause);
			}
			networkHandler.disconnect(address);
		});
	}

	private void handleReceivedFile(ChannelHandlerContext ctx, FilePacket packet, ByteBuf buffer) throws IOException {
		String fileName = packet.getName();
		Path filePath = networkHandler.getFilesDir().resolve(fileName);
		File fileToStore = filePath.toFile();
		if (!fileToStore.exists()) {
			fileToStore.createNewFile();
		}
		RandomAccessFile raFile = new RandomAccessFile(fileToStore, "rw");
		try (FileChannel fileChannel = raFile.getChannel()) {
			ByteBuffer nioBuffer = buffer.nioBuffer();
			while (nioBuffer.hasRemaining()) {
				fileChannel.write(nioBuffer);
			}
		}
		logger.debug("Received file: " + fileToStore);
		ctx.writeAndFlush(new FilesListPacket());
	}
}

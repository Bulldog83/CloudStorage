package ru.bulldog.cloudstorage.network;

import com.google.common.collect.Maps;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.bulldog.cloudstorage.command.ServerCommand;
import ru.bulldog.cloudstorage.command.ServerCommands;
import ru.bulldog.cloudstorage.network.handlers.StringOutboundHandler;
import ru.bulldog.cloudstorage.network.packet.FileProgressPacket;
import ru.bulldog.cloudstorage.network.packet.FilesListPacket;
import ru.bulldog.cloudstorage.network.packet.ReceivingFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.*;

public class ServerNetworkHandler implements AutoCloseable {

	private final static Logger logger = LogManager.getLogger(ServerNetworkHandler.class);
	private final static Path filesDir;

	private final Map<UUID, Connection> activeConnections = Maps.newHashMap();

	private final ServerCommands commands;
	private final int port;

	public ServerNetworkHandler(int port) {
		this.commands = new ServerCommands(this);
		this.port = port;
	}

	public void start() {
		Thread serverThread = new Thread(() -> {
			EventLoopGroup boss = new NioEventLoopGroup(1);
			EventLoopGroup worker = new NioEventLoopGroup();
			try {
				ServerBootstrap server = new ServerBootstrap();
				server.group(boss, worker)
						.channel(NioServerSocketChannel.class)
						.childHandler(new ChannelInitializer<SocketChannel>() {
							@Override
							protected void initChannel(SocketChannel channel) throws Exception {
								ServerNetworkHandler networkHandler = ServerNetworkHandler.this;
								channel.pipeline().addLast(
										new ObjectEncoder(),
										new StringOutboundHandler(),
										new ChunkedWriteHandler(),
										new ServerPacketOutboundHandler(networkHandler),
										new ServerInboundHandler(networkHandler),
										new ServerPacketInboundHandler(networkHandler),
										new ObjectDecoder(ClassResolvers.cacheDisabled(null)),
										new ServerStringInboundHandler(),
										new CommandInboundHandler(networkHandler)
								);
							}
						});
				ChannelFuture channelFuture = server.bind(port).sync();
				logger.info("Server started");
				channelFuture.channel().closeFuture().sync();
			} catch (Exception ex) {
				logger.error("Server error.", ex);
			} finally {
				boss.shutdownGracefully();
				worker.shutdownGracefully();
			}
		}, "Server Bootstrap");
		serverThread.setDaemon(true);
		serverThread.start();
	}

	public Path getFilesDir() {
		return filesDir;
	}

	public Connection register(SocketAddress address, SocketChannel channel) {
		Connection connection = new Connection(channel);
		activeConnections.put(connection.getUUID(), connection);
		return connection;
	}

	public void unregister(UUID uuid) {
		activeConnections.remove(uuid);
	}

	public void disconnect(SocketAddress address, Connection connection) {
		if (connection != null) {
			try {
				if (connection.isConnected()) {
					connection.close();
				}
			} catch (Exception ex) {
				logger.warn("Error close connection: " + address, ex);
			}
			activeConnections.remove(connection.getUUID());
		} else {
			logger.warn("Error close connection: " + address + ", session not specified.");
		}
	}

	public Optional<Connection> getConnection(UUID sessionId) {
		return Optional.ofNullable(activeConnections.get(sessionId));
	}

	public void handleCommand(String data, OutputStream output) {
		Optional<ServerCommand> command = ServerCommand.of(data);
		if (command.isPresent()) {
			try {
				handleCommand(command.get(), output);
			} catch (Exception ex) {
				logger.warn(ex.getMessage(), ex);
			}
		}
	}

	public void handleCommand(ServerCommand command, OutputStream output) throws Exception {
		byte[] result = commands.execute(command);
		if (output != null) {
			output.write(result);
		}
	}

	public void handleFile(FileConnection fileConnection, ByteBuf buffer) throws Exception {
		ReceivingFile receivingFile = fileConnection.getReceivingFile();
		File file = receivingFile.getFile();
		try (FileOutputStream fos = new FileOutputStream(file, true)) {
			FileChannel fileChannel = fos.getChannel();
			ByteBuffer nioBuffer = buffer.nioBuffer();
			while (nioBuffer.hasRemaining()) {
				receivingFile.receive(nioBuffer.remaining());
				fileChannel.write(nioBuffer);
				long received = receivingFile.getReceived();
				double progress = (double) received / receivingFile.getSize();
				fileConnection.sendPacket(new FileProgressPacket(progress));
			}
			if (receivingFile.toReceive() == 0) {
				logger.debug("Received file: " + file);
				fileConnection.sendPacket(new FilesListPacket());
				Optional<Connection> session = getConnection(fileConnection.getUUID());
				session.ifPresent(connection ->
						connection.closeChannel(fileConnection.getChannelId()));
			}
		}
	}

	@Override
	public void close() throws Exception {
		activeConnections.values().forEach(connection -> {
			try {
				connection.sendMessage("Server shutdown...");
				connection.close();
			} catch (Exception ex) {
				logger.warn("Connection close error: " + connection.getChannel().remoteAddress(), ex);
			}
		});
	}

	static {
		File dirFiles = new File("files");
		if (!dirFiles.exists() && !dirFiles.mkdirs()) {
			logger.error("Can't create files dir.");
		}
		filesDir = dirFiles.toPath();
	}
}

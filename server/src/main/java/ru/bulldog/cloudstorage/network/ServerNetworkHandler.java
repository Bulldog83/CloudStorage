package ru.bulldog.cloudstorage.network;

import com.google.common.collect.Maps;
import io.netty.bootstrap.ServerBootstrap;
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

import java.io.File;
import java.io.OutputStream;
import java.net.SocketAddress;
import java.nio.file.Path;
import java.util.*;

public class ServerNetworkHandler {

	private final static Logger logger = LogManager.getLogger(ServerNetworkHandler.class);
	private final static Path filesDir;

	private final Map<UUID, Session> activeConnections = Maps.newHashMap();

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
										new ChunkedWriteHandler(),
										new ServerPacketOutboundHandler(networkHandler),
										new ServerInboundHandler(networkHandler),
										new ServerPacketInboundHandler(networkHandler),
										new ObjectDecoder(ClassResolvers.cacheDisabled(null)),
										new StringInboundHandler(),
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

	public Session register(SocketAddress address, SocketChannel channel) {
		Session session = new Session(channel);
		activeConnections.put(session.getUUID(), session);
		return session;
	}

	public void disconnect(SocketAddress address, Session session) {
		if (session != null) {
			try {
				if (session.isConnected()) {
					session.close();
				}
			} catch (Exception ex) {
				logger.warn("Error close connection: " + address, ex);
			}
			activeConnections.remove(session.getUUID());
		} else {
			logger.warn("Error close connection: " + address + ", session not specified.");
		}
	}

	public Optional<Session> getConnection(UUID sessionId) {
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

	static {
		File dirFiles = new File("files");
		if (!dirFiles.exists() && !dirFiles.mkdirs()) {
			logger.error("Can't create files dir.");
		}
		filesDir = dirFiles.toPath();
	}
}

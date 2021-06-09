package ru.bulldog.cloudstorage.network;

import com.google.common.collect.Maps;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
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
import ru.bulldog.cloudstorage.database.AuthService;
import ru.bulldog.cloudstorage.database.DBAuthService;
import ru.bulldog.cloudstorage.network.handlers.StringOutboundHandler;
import ru.bulldog.cloudstorage.network.packet.FileProgressPacket;
import ru.bulldog.cloudstorage.network.packet.FilesListPacket;
import ru.bulldog.cloudstorage.network.packet.ReceivingFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ServerNetworkHandler implements AutoCloseable {

	private final static Logger logger = LogManager.getLogger(ServerNetworkHandler.class);
	private final static Path filesDir;

	private final Map<UUID, Session> activeSessions = Maps.newHashMap();
	private final Map<ChannelId, Channel> activeChannels = Maps.newHashMap();

	private final ServerCommands commands;
	private final AuthService authService;
	private final int port;

	public ServerNetworkHandler(int port) {
		this.authService = new DBAuthService();
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
		}, "ServerBootstrap");
		serverThread.setDaemon(true);
		serverThread.start();
	}

	public Path getFilesDir() {
		return filesDir;
	}

	public Path getFilesDir(UUID uuid) {
		Path userDir = filesDir.resolve(uuid.toString());
		File directory = userDir.toFile();
		if (!directory.exists()) {
			directory.mkdirs();
		}
		return userDir;
	}

	public void registerChannel(Channel channel) {
		activeChannels.put(channel.id(), channel);
	}

	public Optional<UUID> getUserId(String email, String password) {
		return authService.getUserId(email, password);
	}

	public Optional<UUID> registerUser(String email, String password, String nickname) {
		return authService.registerUser(email, password, nickname);
	}

	public Session registerSession(Channel channel, UUID userId) {
		if (activeChannels.containsKey(channel.id())) {
			channel = activeChannels.remove(channel.id());
		}
		Session session = new Session(userId, channel);
		activeSessions.put(userId, session);
		return session;
	}

	public void disconnect(ChannelId id) {
		if (activeChannels.containsKey(id)) {
			activeChannels.remove(id).close();
		}
	}

	public void disconnect(Session session) {
		activeSessions.remove(session.getSessionId());
		session.close();
	}

	public Session getSession(UUID sessionId) {
		return activeSessions.get(sessionId);
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
		Session session = activeSessions.get(fileConnection.getSessionId());
		Connection connection = session.getConnection();
		File file = receivingFile.getFile();
		try (FileOutputStream fos = new FileOutputStream(file, true)) {
			FileChannel fileChannel = fos.getChannel();
			ByteBuffer nioBuffer = buffer.nioBuffer();
			while (nioBuffer.hasRemaining()) {
				receivingFile.receive(nioBuffer.remaining());
				fileChannel.write(nioBuffer);
				long received = receivingFile.getReceived();
				double progress = (double) received / receivingFile.getSize();
				connection.sendPacket(new FileProgressPacket(progress));
			}
			if (receivingFile.toReceive() == 0) {
				logger.debug("Received file: " + file);
				connection.sendPacket(new FilesListPacket());
				session.closeFileChannel(fileConnection);
			}
		}
	}

	@Override
	public void close() throws Exception {
		activeSessions.values().forEach(Session::close);
		activeChannels.values().forEach(Channel::close);
	}

	static {
		File dirFiles = new File("files");
		if (!dirFiles.exists() && !dirFiles.mkdirs()) {
			logger.error("Can't create files dir.");
		}
		filesDir = dirFiles.toPath();
	}
}

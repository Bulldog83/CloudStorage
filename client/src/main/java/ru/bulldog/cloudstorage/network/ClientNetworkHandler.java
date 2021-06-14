package ru.bulldog.cloudstorage.network;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.bulldog.cloudstorage.event.EventsHandler;
import ru.bulldog.cloudstorage.gui.controllers.MainController;
import ru.bulldog.cloudstorage.network.handlers.StringInboundHandler;
import ru.bulldog.cloudstorage.network.packet.AuthData;
import ru.bulldog.cloudstorage.network.packet.Packet;
import ru.bulldog.cloudstorage.network.packet.ReceivingFile;
import ru.bulldog.cloudstorage.tasks.NamedTask;
import ru.bulldog.cloudstorage.tasks.ThreadManager;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ClientNetworkHandler {

	private static final Logger logger = LogManager.getLogger(ClientNetworkHandler.class);
	private final static String host;
	private final static int port;

	private final ThreadManager threadManager;
	private final EventsHandler eventsHandler;
	private final MainController controller;
	private final ChannelPool channelPool;
	private final EventLoopGroup worker;
	private final Bootstrap bootstrap;

	private Connection connection;
	private AuthData authData;
	private Session session;

	public ClientNetworkHandler(MainController controller) {
		this.controller = controller;
		this.threadManager = new ThreadManager(8);
		this.eventsHandler = EventsHandler.getInstance();
		this.worker = new NioEventLoopGroup();
		this.bootstrap = new Bootstrap();
		bootstrap.group(worker)
				.remoteAddress(host, port)
				.channelFactory(NioSocketChannel::new)
				.option(ChannelOption.SO_KEEPALIVE, true)
				.handler(new ChannelInitializer<SocketChannel>() {
					@Override
					protected void initChannel(SocketChannel channel) throws Exception {
						ClientNetworkHandler networkHandler = ClientNetworkHandler.this;
						channel.pipeline().addLast(
							new ChunkedWriteHandler(),
							new ClientPacketOutboundHandler(networkHandler),
							new ClientInboundHandler(networkHandler),
							new ClientPacketInboundHandler(networkHandler),
							new ClientStringInboundHandler()
						);
					}
				});
		this.channelPool = new ChannelPool(threadManager, bootstrap, 5);
		threadManager.start();
	}

	public ThreadManager getThreadManager() {
		return threadManager;
	}

	public AuthData getAuthData() {
		return authData;
	}

	public void connect(AuthData authData) {
		this.authData = authData;
		connectInternal();
	}

	private ChannelFuture connectInternal() {
		CompletableFuture<ChannelFuture> futureChannel = new CompletableFuture<>();
		threadManager.execute(new NamedTask("MainChannel", () -> {
			try {
				ChannelFuture channelFuture = bootstrap.connect();
				futureChannel.complete(channelFuture);
				channelFuture.sync();
				Channel channel = channelFuture.channel();
				this.connection = new Connection(channel);
				channel.closeFuture().addListener(future -> {
					if (future.isDone() && session != null && !session.isClosed()) {
						logger.warn("Channel closed: " + channel, future.cause());
						this.session = null;
					}
					eventsHandler.onDisconnect();
				}).sync();
			} catch (Exception ex) {
				logger.error("Connection error.", ex);
				futureChannel.completeExceptionally(ex);
				eventsHandler.onHandleError(ex.getMessage());
			}
		}, true));
		return futureChannel.join();
	}

	public Channel openChannel() {
		Channel channel = channelPool.openChannel();
		while (!session.isRegistered(channel));
		return channel;
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
				eventsHandler.onFileProgress(progress);
			}
			if (receivingFile.toReceive() == 0) {
				logger.debug("Received file: " + file);
				session.closeFileChannel(fileConnection);
				eventsHandler.onFileReceived();
			}
		}
	}

	public void setSession(UUID sessionId) {
		this.session = new Session(sessionId, connection);
		logger.debug("Session started: " + sessionId);
		eventsHandler.onConnect();
	}

	public Session getSession() {
		return session;
	}

	public boolean hasSession() {
		return session != null;
	}

	public Path getFilesDir() {
		return controller.getFilesDir();
	}

	public void sendPacket(Packet packet) {
		connection.sendPacket(packet);
	}

	static {
		Properties properties = System.getProperties();
		host = properties.getProperty("server.host", "localhost");
		port = (int) properties.getOrDefault("server.port", 8072);
	}

	public boolean isConnected() {
		return session != null && session.isConnected();
	}

	public ChannelFuture close() throws Exception {
		if (isConnected()) {
			channelPool.close();
			return session.close().addListener(future -> {
				if (future.isDone()) {
					threadManager.close();
					worker.shutdownGracefully();
				}
			});
		}
		threadManager.close();
		worker.shutdownGracefully();
		return null;
	}
}

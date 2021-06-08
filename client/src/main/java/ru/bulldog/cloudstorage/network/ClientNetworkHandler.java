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
import ru.bulldog.cloudstorage.gui.controllers.MainController;
import ru.bulldog.cloudstorage.network.handlers.StringInboundHandler;
import ru.bulldog.cloudstorage.network.packet.Packet;
import ru.bulldog.cloudstorage.network.packet.ReceivingFile;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.Properties;
import java.util.UUID;

public class ClientNetworkHandler {

	private static final Logger logger = LogManager.getLogger(ClientNetworkHandler.class);
	private final static String host;
	private final static int port;

	private final MainController controller;
	private final ChannelPool channelPool;
	private Connection connection;
	private Session session;

	public ClientNetworkHandler(MainController controller) {
		this.controller = controller;
		EventLoopGroup worker = new NioEventLoopGroup();
		Bootstrap bootstrap = new Bootstrap();
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
							new StringInboundHandler()
						);
					}
				});
		this.channelPool = new ChannelPool(bootstrap, 5);
		Thread channelThread = new Thread(() -> {
			try {
				ChannelFuture channelFuture = bootstrap.connect().sync();
				Channel channel = channelFuture.channel();
				this.connection = new Connection(channel);
				channel.closeFuture().addListener(future -> {
					if (future.isDone() && session != null && !session.isClosed()) {
						logger.error("Channel closed: " + channel, future.cause());
					}
				}).sync();
			} catch (Exception ex) {
				logger.error("Connection error.", ex);
			} finally {
				worker.shutdownGracefully();
			}
		}, "MainChannel");
		channelThread.setDaemon(true);
		channelThread.start();
	}

	public Channel openChannel() {
		return channelPool.openChannel();
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
				controller.updateProgress(progress);
			}
			if (receivingFile.toReceive() == 0) {
				logger.debug("Received file: " + file);
				controller.stopTransfer();
				controller.refreshClientFiles();
				session.closeFileChannel(fileConnection);
			}
		}
	}

	public MainController getController() {
		return controller;
	}

	public void setSession(UUID sessionId) {
		this.session = new Session(sessionId, connection);
		logger.debug("Session started: " + sessionId);
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

	public ChannelFuture close() throws Exception {
		channelPool.close();
		return session.close();
	}
}

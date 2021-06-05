package ru.bulldog.cloudstorage.network;

import com.google.common.collect.Lists;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
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
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ClientNetworkHandler {

	private static final Logger logger = LogManager.getLogger(ClientNetworkHandler.class);
	private final static String host;
	private final static int port;

	private final List<Channel> activeChannels = Lists.newArrayList();
	private final MainController controller;
	private final Bootstrap bootstrap;
	private Connection connection;

	public ClientNetworkHandler(MainController controller) {
		this.controller = controller;
		this.bootstrap = new Bootstrap();
		EventLoopGroup worker = new NioEventLoopGroup();
		bootstrap.group(worker)
				.channel(NioSocketChannel.class)
				.remoteAddress(host, port)
				.handler(new ChannelInitializer<SocketChannel>() {
					@Override
					protected void initChannel(SocketChannel channel) throws Exception {
						ClientNetworkHandler networkHandler = ClientNetworkHandler.this;
						channel.pipeline().addLast(
								new ChunkedWriteHandler(),
								new ClientInboundHandler(networkHandler),
								new ClientPacketInboundHandler(networkHandler),
								new ClientPacketOutboundHandler(networkHandler),
								new StringInboundHandler()
						);
					}
				});
		Thread channelThread = new Thread(() -> {
			try {
				ChannelFuture channelFuture = bootstrap.connect().sync();
				SocketChannel channel = (SocketChannel) channelFuture.channel();
				this.connection = new Connection(channel);
				channel.closeFuture().sync();
			} catch (Exception ex) {
				logger.error("Connection error.", ex);
			} finally {
				worker.shutdownGracefully();
			}
		});
		channelThread.setDaemon(true);
		channelThread.start();
	}

	public Channel openChannel() {
		CompletableFuture<Channel> completableFuture = new CompletableFuture<>();
		Thread channelThread = new Thread(() -> {
			try {
				ChannelFuture channelFuture = bootstrap.connect().sync();
				Channel channel = channelFuture.channel();
				completableFuture.complete(channel);
				activeChannels.add(channel);
				channel.closeFuture().sync();
			} catch (Exception ex) {
				logger.error("Connection error.", ex);
			}
		});
		channelThread.setDaemon(true);
		channelThread.start();
		return completableFuture.join();
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
				fileConnection.close();
				activeChannels.remove(fileConnection.getChannel());
			}
		}
	}

	public MainController getController() {
		return controller;
	}

	public Connection getConnection() {
		return connection;
	}

	public UUID getSession() {
		return connection.getUUID();
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

	public ChannelFuture close() {
		activeChannels.forEach(Channel::close);
		return connection.close();
	}
}

package ru.bulldog.cloudstorage.network;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.stream.ChunkedWriteHandler;
import javafx.application.Platform;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.bulldog.cloudstorage.gui.controllers.MainController;
import ru.bulldog.cloudstorage.network.packet.FilePacket;
import ru.bulldog.cloudstorage.network.packet.FilesListPacket;
import ru.bulldog.cloudstorage.network.packet.ListRequest;
import ru.bulldog.cloudstorage.network.packet.Packet;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

public class ClientNetworkHandler {

	private final static Path filesDir;
	private static final Logger logger = LogManager.getLogger(ClientNetworkHandler.class);

	private final MainController controller;
	private Connection connection;

	public ClientNetworkHandler(MainController controller, int port) {
		this.controller = controller;

		Thread connectionThread = new Thread(() -> {
			EventLoopGroup worker = new NioEventLoopGroup();
			try {
				Bootstrap bootstrap = new Bootstrap();
				bootstrap.group(worker)
						.channel(NioSocketChannel.class)
						.handler(new ChannelInitializer<SocketChannel>() {
							@Override
							protected void initChannel(SocketChannel channel) throws Exception {
								connection = new Connection(channel);
								ClientNetworkHandler networkHandler = ClientNetworkHandler.this;
								channel.pipeline().addLast(
										new ChunkedWriteHandler(),
										new ClientInboundHandler(networkHandler),
										new ClientPacketOutboundHandler(),
										new ClientPacketInboundHandler(controller)
								);
							}
						});
				ChannelFuture channelFuture = bootstrap.connect("localhost", port).sync();
				channelFuture.addListener(future -> {
					if (future.isSuccess()) {
						sendPacket(new ListRequest());
					}
				});
				channelFuture.channel().closeFuture().sync();
			} catch (Exception ex) {
				logger.error("Connection error.", ex);
			} finally {
				worker.shutdownGracefully();
			}
		});
		connectionThread.setDaemon(true);
		connectionThread.start();
	}

	public MainController getController() {
		return controller;
	}

	public Connection getConnection() {
		return connection;
	}

	public void sendPacket(Packet packet) {
		connection.sendPacket(packet);
	}

	static {
		File dirFiles = new File(".");
		filesDir = dirFiles.toPath();
	}
}

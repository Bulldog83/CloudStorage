package ru.bulldog.cloudstorage.network;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
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
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class ClientNetworkHandler implements NetworkHandler {

	private final static Path filesDir;
	private static final Logger logger = LogManager.getLogger(ClientNetworkHandler.class);

	private final MainController controller;
	private SocketChannel socketChannel;
	private final int port;

	public ClientNetworkHandler(MainController controller, int port) {
		this.controller = controller;
		this.port = port;

		Thread connectionThread = new Thread(() -> {
			EventLoopGroup worker = new NioEventLoopGroup();
			try {
				Bootstrap bootstrap = new Bootstrap();
				bootstrap.group(worker)
						.channel(NioSocketChannel.class)
						.handler(new ChannelInitializer<SocketChannel>() {
							@Override
							protected void initChannel(SocketChannel channel) throws Exception {
								socketChannel = channel;
								channel.pipeline().addLast(
										new ChunkedWriteHandler(),
										new ClientInboundHandler(),
										new ClientPacketOutboundHandler(),
										new ClientPacketInboundHandler(controller)
								);
							}
						});
				ChannelFuture channelFuture = bootstrap.connect("localhost", port).sync();
				channelFuture.addListener(future -> {
					if (future.isSuccess()) {
						socketChannel.writeAndFlush(new ListRequest());
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

	public void sendFile(Path file) {
		try {
			FilePacket packet = new FilePacket(file);
			socketChannel.writeAndFlush(packet);
		} catch (Exception ex) {
			logger.error("Send file error: " + file, ex);
		}
	}

	@Override
	public void handlePacket(Connection connection, Packet packet) {
	}

	private void handleFilesList(FilesListPacket packet) {
		List<String> names = packet.getNames();
		Platform.runLater(() -> controller.serverFiles.getItems().setAll(names));
	}

	@Override
	public void close() throws Exception {

	}

	static {
		File dirFiles = new File(".");
		filesDir = dirFiles.toPath();
	}
}

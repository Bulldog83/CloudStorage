package ru.bulldog.cloudstorage.network;

import io.netty.bootstrap.Bootstrap;
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
import ru.bulldog.cloudstorage.network.packet.Packet;

import java.util.Properties;

public class ClientNetworkHandler {

	private static final Logger logger = LogManager.getLogger(ClientNetworkHandler.class);
	private final static String host;
	private final static int port;

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
						connection = new Connection(channel);
						ClientNetworkHandler networkHandler = ClientNetworkHandler.this;
						channel.pipeline().addLast(
								new ChunkedWriteHandler(),
								new ClientInboundHandler(networkHandler),
								new ClientPacketInboundHandler(networkHandler),
								new ClientPacketOutboundHandler()
						);
					}
				});
		Thread connectionThread = new Thread(() -> {
			try {
				ChannelFuture channelFuture = bootstrap.connect().sync();
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

	public void openFileChannel() {

	}

	public void setSession(Connection connection) {
		this.connection = connection;
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
		Properties properties = System.getProperties();
		host = properties.getProperty("server.host", "localhost");
		port = (int) properties.getOrDefault("server.port", 8072);
	}
}

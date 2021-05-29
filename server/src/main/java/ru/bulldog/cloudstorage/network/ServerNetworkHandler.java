package ru.bulldog.cloudstorage.network;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.bulldog.cloudstorage.command.ServerCommand;
import ru.bulldog.cloudstorage.command.ServerCommands;
import ru.bulldog.cloudstorage.network.packet.FilePacket;
import ru.bulldog.cloudstorage.network.packet.FileRequest;
import ru.bulldog.cloudstorage.network.packet.FilesListPacket;
import ru.bulldog.cloudstorage.network.packet.Packet;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class ServerNetworkHandler implements NetworkHandler {

	private final static Logger logger = LogManager.getLogger(ServerNetworkHandler.class);
	private final static byte[] WRONG_COMMAND_BYTES;
	private final static Path filesDir;

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
								channel.pipeline().addLast(
										new ServerInboundHandler(),
										new ServerPacketOutboundHandler(),
										new ServerPacketInboundHandler(),
										new StringInboundHandler()
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

	public boolean handleCommand(String data, OutputStream output) {
		Optional<ServerCommand> command = ServerCommand.of(data);
		if (command.isPresent()) {
			try {
				handleCommand(command.get(), output);
				return true;
			} catch (Exception ex) {
				logger.warn(ex.getMessage(), ex);
			}
		}
		return false;
	}

	public void handleCommand(ServerCommand command, OutputStream output) throws Exception {
		byte[] result = commands.execute(command);
		if (output != null) {
			output.write(result);
		}
	}

	@Override
	public void handlePacket(Connection client, Packet packet) {
		switch (packet.getType()) {
			case FILE:
				//handleFile(client, (FilePacket) packet);
				break;
			case FILE_REQUEST:
				handleFileRequest(client, (FileRequest) packet);
				break;
			case LIST_REQUEST:
				handleListRequest(client);
				break;
		}
	}

	private void handleListRequest(Connection client) {
		try {
			List<String> filesNames = Files.list(filesDir)
					.map(file -> file.getFileName().toString())
					.collect(Collectors.toList());
			FilesListPacket listPacket = new FilesListPacket();
			listPacket.addAll(filesNames);
			client.sendData(listPacket);
		} catch (Exception ex) {
			logger.error(ex.getLocalizedMessage(), ex);
		}
	}

	private void handleFileRequest(Connection client, FileRequest packet) {
		try {
			String fileName = packet.getName();
			Files.list(filesDir).forEach(file -> {
				try {
					if (fileName.equals(file.getFileName().toString())) {
						FilePacket filePacket = new FilePacket(file);
						client.sendData(filePacket);
					}
				} catch (IOException ex) {
					logger.error(ex.getLocalizedMessage(), ex);
				}
			});
		} catch (Exception ex) {
			logger.error(ex.getLocalizedMessage(), ex);
		}
	}

//	private void handleFile(Connection client, FilePacket packet) {
//		if (!packet.isEmpty()) {
//			try {
//				Path output = filesDir.resolve(packet.getName());
//				byte[] data = packet.getData();
//				Files.write(output, data, StandardOpenOption.CREATE);
//				handleListRequest(client);
//			} catch (Exception ex) {
//				logger.warn(ex.getLocalizedMessage(), ex);
//			}
//		}
//	}

	@Override
	public void close() throws Exception {

	}

	static {
		File dirFiles = new File("files");
		if (!dirFiles.exists() && !dirFiles.mkdirs()) {
			logger.error("Can't create files dir.");
		}
		filesDir = dirFiles.toPath();
		WRONG_COMMAND_BYTES = "Unknown command.\n\r".getBytes(StandardCharsets.UTF_8);
	}
}

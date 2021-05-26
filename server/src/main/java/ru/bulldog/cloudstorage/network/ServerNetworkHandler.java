package ru.bulldog.cloudstorage.network;

import com.google.common.collect.Lists;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.bulldog.cloudstorage.command.ServerCommand;
import ru.bulldog.cloudstorage.command.ServerCommands;
import ru.bulldog.cloudstorage.data.DataBuffer;
import ru.bulldog.cloudstorage.network.packet.FilePacket;
import ru.bulldog.cloudstorage.network.packet.FileRequest;
import ru.bulldog.cloudstorage.network.packet.FilesListPacket;
import ru.bulldog.cloudstorage.network.packet.Packet;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.stream.Collectors;

public class ServerNetworkHandler implements NetworkHandler {

	private final static Logger LOGGER = LogManager.getLogger(ServerNetworkHandler.class);
	private final static byte[] WRONG_COMMAND_BYTES;
	private final static Path filesDir;

	private final List<Connection> clients = Lists.newArrayList();

	private ServerSocketChannel server;
	private ServerSocket serverSocket;
	private Selector selector;
	private ByteBuffer buffer;
	private ServerCommands commands;

	public ServerNetworkHandler() {
		try {
			this.server = ServerSocketChannel.open();
			server.bind(new InetSocketAddress(8099));
			server.configureBlocking(false);
			this.selector = Selector.open();
			server.register(selector, SelectionKey.OP_ACCEPT);
			this.serverSocket = server.socket();
			this.buffer = ByteBuffer.allocate(1024);
			this.commands = new ServerCommands(this);
			Thread serverThread = new Thread(() -> {
				try {
					while (isAlive()) {
						selector.select();
						Set<SelectionKey> selectionKeys = selector.selectedKeys();
						Iterator<SelectionKey> keyIterator = selectionKeys.iterator();

						while (keyIterator.hasNext()) {
							SelectionKey selectionKey = keyIterator.next();
							if (selectionKey.isAcceptable()) {
								handleAccept(selectionKey);
							}
							if (selectionKey.isReadable()) {
								handleRead(selectionKey);
							}
							keyIterator.remove();
						}
					}
				} catch (SocketException sEx) {
					LOGGER.warn(sEx.getLocalizedMessage());
				} catch (Exception ex) {
					LOGGER.error(ex.getLocalizedMessage(), ex);
				}
			});
			serverThread.setDaemon(true);
			serverThread.start();
			LOGGER.info("Server started.");
		} catch (Exception ex) {
			LOGGER.error(ex.getLocalizedMessage(), ex);
		}
	}

	public Path getFilesDir() {
		return filesDir;
	}

	private void handleRead(SelectionKey selectionKey) {
		try {
			SocketChannel channel = (SocketChannel) selectionKey.channel();
			try {
				int read;
				DataBuffer dataBuffer = new DataBuffer();
				while (channel.isOpen()) {
					read = channel.read(buffer);
					if (read == -1) {
						channel.close();
						return;
					}
					if (read == 0) break;
					buffer.flip();
					dataBuffer.read(buffer);
					buffer.clear();
				}
				String data = new String(dataBuffer.getBytes(), StandardCharsets.UTF_8).trim();
				try(ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
					if (!handleCommand(data, outputStream)) {
						channel.write(ByteBuffer.wrap(WRONG_COMMAND_BYTES));
					} else {
						byte[] bytes = outputStream.toByteArray();
						channel.write(ByteBuffer.wrap(bytes));
					}
				}
				dataBuffer.reset();
			} catch (Exception ex) {
				LOGGER.warn(ex.getLocalizedMessage(), ex);
				channel.close();
			}
		} catch (Exception ex) {
			LOGGER.error(ex.getLocalizedMessage(), ex);
		}
	}

	public boolean handleCommand(String data, OutputStream output) {
		Optional<ServerCommand> command = ServerCommand.of(data);
		if (command.isPresent()) {
			try {
				handleCommand(command.get(), output);
				return true;
			} catch (Exception ex) {
				LOGGER.warn(ex.getMessage(), ex);
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

	private void handleAccept(SelectionKey selectionKey) {
		try {
			Socket connection = serverSocket.accept();
			try {
				ClientConnection client = new ClientConnection(this, connection);
				handleListRequest(client);
				client.listen();
				clients.add(client);
			} catch (Exception ex) {
				SocketChannel channel = connection.getChannel();
				channel.configureBlocking(false);
				channel.register(selector, SelectionKey.OP_READ);
			}
		} catch (Exception ex) {
			LOGGER.warn(ex.getLocalizedMessage(), ex);
		}
	}

	@Override
	public void handlePacket(Connection client, Packet packet) {
		switch (packet.getType()) {
			case FILE:
				handleFile(client, (FilePacket) packet);
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
		if (!isAlive() || !checkConnection(client)) return;
		try {
			List<String> filesNames = Files.list(filesDir)
					.map(file -> file.getFileName().toString())
					.collect(Collectors.toList());
			FilesListPacket listPacket = new FilesListPacket();
			listPacket.addAll(filesNames);
			client.sendData(listPacket);
		} catch (Exception ex) {
			LOGGER.error(ex.getLocalizedMessage(), ex);
		}
	}

	private void handleFileRequest(Connection client, FileRequest packet) {
		if (!isAlive() || !checkConnection(client)) return;
		try {
			String fileName = packet.getName();
			Files.list(filesDir).forEach(file -> {
				try {
					if (fileName.equals(file.getFileName().toString())) {
						FilePacket filePacket = new FilePacket(file);
						client.sendData(filePacket);
					}
				} catch (IOException ex) {
					LOGGER.error(ex.getLocalizedMessage(), ex);
				}
			});
		} catch (Exception ex) {
			LOGGER.error(ex.getLocalizedMessage(), ex);
		}
	}

	private void handleFile(Connection client, FilePacket packet) {
		if (!isAlive() || !checkConnection(client)) return;
		if (!packet.isEmpty()) {
			try {
				Path output = filesDir.resolve(packet.getName());
				byte[] data = packet.getData();
				Files.write(output, data, StandardOpenOption.CREATE);
				handleListRequest(client);
			} catch (Exception ex) {
				LOGGER.warn(ex.getLocalizedMessage(), ex);
			}
		}
	}

	public boolean isAlive() {
		return server != null && server.isOpen();
	}

	@Override
	public void close() throws Exception {
		Iterator<Connection> connections = clients.iterator();
		while (connections.hasNext()) {
			Connection client = connections.next();
			connections.remove();
			client.close();
		}
		server.close();
	}

	private boolean checkConnection(Connection connection) {
		if (connection.isConnected()) return true;
		clients.remove(connection);
		return false;
	}

	static {
		File dirFiles = new File("files");
		if (!dirFiles.exists() && !dirFiles.mkdirs()) {
			LOGGER.error("Can't create files dir.");
		}
		filesDir = dirFiles.toPath();
		WRONG_COMMAND_BYTES = "Unknown command.\n\r".getBytes(StandardCharsets.UTF_8);
	}
}

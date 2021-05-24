package ru.bulldog.cloudstorage.network;

import com.google.common.collect.Lists;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.*;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ServerNetworkHandler implements NetworkHandler {

	private final static Logger LOGGER = LogManager.getLogger(ServerNetworkHandler.class);
	private final static Path filesDir;

	private final List<Connection> clients = Lists.newArrayList();

	private ServerSocketChannel server;
	private ServerSocket serverSocket;
	private Selector selector;

	public ServerNetworkHandler() {
		try {
			this.server = ServerSocketChannel.open();
			server.bind(new InetSocketAddress(8099));
			server.configureBlocking(false);
			this.selector = Selector.open();
			server.register(selector, SelectionKey.OP_ACCEPT);
			this.serverSocket = server.socket();
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
							keyIterator.remove();
						}

						Socket connection = serverSocket.accept();
						ClientConnection client = new ClientConnection(this, connection);
						handleListRequest(client);
						client.listen();
						clients.add(client);
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

	private void handleAccept(SelectionKey selectionKey) throws IOException {
		SocketChannel channel = server.accept();
		channel.configureBlocking(false);
		channel.register(selector, SelectionKey.OP_READ);
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
	}
}

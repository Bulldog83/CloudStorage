package ru.bulldog.cloudstorage.network;

import com.google.common.collect.Lists;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class ServerNetworkHandler implements NetworkHandler {

	private final static Logger LOGGER = LogManager.getLogger(ServerNetworkHandler.class);
	private final static Path filesDir;

	private final List<Connection> clients = Lists.newArrayList();

	private ServerSocket server;

	public ServerNetworkHandler() {
		try {
			this.server = new ServerSocket(8099);
			Thread serverThread = new Thread(() -> {
				try {
					while (isAlive()) {
						Socket connection = server.accept();
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
		} catch (Exception ex) {
			LOGGER.error(ex.getLocalizedMessage(), ex);
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
		return server != null && !server.isClosed();
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

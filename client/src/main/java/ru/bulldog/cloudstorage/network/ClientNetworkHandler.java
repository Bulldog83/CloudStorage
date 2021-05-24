package ru.bulldog.cloudstorage.network;

import javafx.application.Platform;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.bulldog.cloudstorage.gui.controllers.MainController;

import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.stream.Collectors;

public class ClientNetworkHandler implements NetworkHandler {

	private final static Path filesDir;
	private static final Logger LOGGER = LogManager.getLogger(ClientNetworkHandler.class);

	private final MainController controller;
	private ServerConnection connection;

	public ClientNetworkHandler(MainController controller) {
		this.controller = controller;
	}

	public ServerConnection getConnection() throws IOException {
		if (connection != null && connection.isConnected()) {
			return connection;
		}
		Socket socket = new Socket("localhost", 8099);
		this.connection = new ServerConnection(this, socket);
		connection.listen();
		return connection;
	}

	@Override
	public void handlePacket(Connection connection, Packet packet) {
		switch (packet.getType()) {
			case FILES_LIST:
				handleFilesList((FilesListPacket) packet);
				break;
			case FILE:
				handleFile((FilePacket) packet);
				break;
		}
	}

	private void handleFile(FilePacket packet) {
		if (!packet.isEmpty()) {
			try {
				Path output = filesDir.resolve(packet.getName());
				byte[] data = packet.getData();
				Files.write(output, data, StandardOpenOption.CREATE);
				Platform.runLater(() -> {
					try {
						controller.refreshFiles(Files.list(filesDir).map(Path::toFile)
								.collect(Collectors.toList()));
					} catch (IOException ex) {
						LOGGER.warn(ex.getLocalizedMessage(), ex);
					}
				});
			} catch (Exception ex) {
				LOGGER.warn(ex.getLocalizedMessage(), ex);
			}
		}
	}

	private void handleFilesList(FilesListPacket packet) {
		List<String> names = packet.getNames();
		Platform.runLater(() -> controller.serverFiles.getItems().setAll(names));
	}

	@Override
	public void close() throws Exception {
		connection.close();
	}

	static {
		File dirFiles = new File(".");
		filesDir = dirFiles.toPath();
	}
}

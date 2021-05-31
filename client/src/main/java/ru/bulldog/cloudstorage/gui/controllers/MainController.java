package ru.bulldog.cloudstorage.gui.controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.bulldog.cloudstorage.network.ClientNetworkHandler;
import ru.bulldog.cloudstorage.network.packet.FilePacket;
import ru.bulldog.cloudstorage.network.packet.FileRequest;
import ru.bulldog.cloudstorage.network.packet.Packet;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class MainController implements Initializable {

	private final static Logger logger = LogManager.getLogger(MainController.class);

	@FXML
	public ListView<File> clientFiles;
	@FXML
	public ListView<String> serverFiles;

	public void sendFile(ActionEvent actionEvent) {
		File file = clientFiles.getSelectionModel().getSelectedItem();
		if (file != null) {
			try {
				FilePacket packet = new FilePacket(file.toPath());
				networkHandler.sendPacket(packet);
			} catch (Exception ex) {
				logger.error("Send file error: " + file, ex);
			}
		}
	}

	public void requestFile(ActionEvent actionEvent) {
		String name = serverFiles.getSelectionModel().getSelectedItem();
		if (name != null) {
			try {
				FileRequest packet = new FileRequest(name);
				networkHandler.sendPacket(packet);
			} catch (Exception ex) {
				logger.warn("Request file error: " + name, ex);
			}
		}
	}

	private ClientNetworkHandler networkHandler;
	private Path filesDir;

	public void refresh() {
		Platform.runLater(() -> {
			try {
				refreshFiles(Files.list(filesDir).map(Path::toFile)
						.collect(Collectors.toList()));
			} catch (IOException ex) {
				logger.error(ex.getMessage(), ex);
			}
		});
	}

	public void refreshFiles(Collection<File> files) {
		clientFiles.getItems().setAll(files);
	}

	public Path getFilesDir() {
		return filesDir;
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		this.filesDir = Paths.get(".");
		new Thread(() -> {
			try {
				networkHandler = new ClientNetworkHandler(this, 8072);
				refresh();
			} catch (Exception ex) {
				logger.error(ex.getMessage(), ex);
			}
		}).start();
	}
}

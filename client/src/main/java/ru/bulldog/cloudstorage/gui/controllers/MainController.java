package ru.bulldog.cloudstorage.gui.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.bulldog.cloudstorage.network.ClientNetworkHandler;
import ru.bulldog.cloudstorage.network.FilePacket;
import ru.bulldog.cloudstorage.network.FileRequest;
import ru.bulldog.cloudstorage.network.ServerConnection;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class MainController implements Initializable, AutoCloseable {

	private final static Logger LOGGER = LogManager.getLogger(MainController.class);

	@FXML
	public ListView<File> clientFiles;
	@FXML
	public ListView<String> serverFiles;

	public void sendFile(ActionEvent actionEvent) {
		File file = clientFiles.getSelectionModel().getSelectedItem();
		if (file != null) {
			try {
				if (!connection.isConnected()) {
					connect();
				}
				FilePacket packet = new FilePacket(file.toPath());
				connection.sendData(packet);
			} catch (Exception ex) {
				LOGGER.warn(ex.getLocalizedMessage(), ex);
			}
		}
	}

	public void requestFile(ActionEvent actionEvent) {
		String name = serverFiles.getSelectionModel().getSelectedItem();
		if (name != null) {
			try {
				FileRequest packet = new FileRequest(name);
				connection.sendData(packet);
			} catch (Exception ex) {
				LOGGER.warn(ex.getLocalizedMessage(), ex);
			}
		}
	}

	private ClientNetworkHandler networkHandler;
	private ServerConnection connection;

	private void connect() throws IOException {
		connection = networkHandler.getConnection();
	}

	public void refreshFiles(Collection<File> files) {
		clientFiles.getItems().setAll(files);
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		File runDir = new File(".");
		try {
			refreshFiles(Files.list(runDir.toPath()).map(Path::toFile)
					.collect(Collectors.toList()));
			networkHandler = new ClientNetworkHandler(this);
			connect();
		} catch (Exception ex) {
			LOGGER.error(ex.getLocalizedMessage(), ex);
		}
	}

	@Override
	public void close() throws Exception {
		networkHandler.close();
	}
}

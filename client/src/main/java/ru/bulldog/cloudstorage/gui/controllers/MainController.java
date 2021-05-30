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
			networkHandler.sendFile(file.toPath());
		}
	}

	public void requestFile(ActionEvent actionEvent) {
		String name = serverFiles.getSelectionModel().getSelectedItem();
		if (name != null) {
			try {
				//FileRequest packet = new FileRequest(name);
			} catch (Exception ex) {
				LOGGER.warn(ex.getLocalizedMessage(), ex);
			}
		}
	}

	private ClientNetworkHandler networkHandler;

	public void refreshFiles(Collection<File> files) {
		clientFiles.getItems().setAll(files);
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		File runDir = new File(".");
		new Thread(() -> {
			try {
				networkHandler = new ClientNetworkHandler(this, 8072);
				Platform.runLater(() -> {
					try {
						refreshFiles(Files.list(runDir.toPath()).map(Path::toFile)
								.collect(Collectors.toList()));
					} catch (IOException ex) {
						LOGGER.error(ex.getLocalizedMessage(), ex);
					}
				});
			} catch (Exception ex) {
				LOGGER.error(ex.getLocalizedMessage(), ex);
			}
		}).start();
	}

	@Override
	public void close() throws Exception {
		networkHandler.close();
	}
}

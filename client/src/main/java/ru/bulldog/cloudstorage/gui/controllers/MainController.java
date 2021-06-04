package ru.bulldog.cloudstorage.gui.controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.bulldog.cloudstorage.network.ClientNetworkHandler;
import ru.bulldog.cloudstorage.network.packet.FilePacket;
import ru.bulldog.cloudstorage.network.packet.FileRequest;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class MainController implements Initializable, AutoCloseable {

	private final static Logger logger = LogManager.getLogger(MainController.class);

	@FXML
	public ListView<File> clientFiles;
	@FXML
	public ListView<String> serverFiles;
	@FXML
	public TextField clientPath;
	@FXML
	public TextField serverPath;
	@FXML
	public AnchorPane transferPane;
	@FXML
	public ProgressBar transferProgress;
	@FXML
	public Label progressValue;
	@FXML
	public Label transferState;
	@FXML
	public Label transferFile;

	public void sendFile(ActionEvent actionEvent) {
		File file = clientFiles.getSelectionModel().getSelectedItem();
		if (file != null) {
			try {
				FilePacket packet = new FilePacket(networkHandler.getSession(), file.toPath());
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
				FileRequest packet = new FileRequest(networkHandler.getSession(), name);
				networkHandler.sendPacket(packet);
			} catch (Exception ex) {
				logger.warn("Request file error: " + name, ex);
			}
		}
	}

	public void switchFolder(ActionEvent actionEvent) {
		String directory = clientPath.getText();
		try {
			this.filesDir = Paths.get(directory);
			refreshClientFiles();
		} catch (Exception ex) {
			logger.warn("Can't open directory " + directory, ex);
		}
	}

	public void showFolderDialog(ActionEvent actionEvent) {
		directoryChooser.setInitialDirectory(filesDir.toFile());
		File dir = directoryChooser.showDialog(clientFiles.getScene().getWindow());
		if (dir != null) {
			filesDir = dir.toPath();
			clientPath.setText(filesDir.toString());
			refreshClientFiles();
		}
	}

	private DirectoryChooser directoryChooser;
	private ClientNetworkHandler networkHandler;
	private Path filesDir;

	public void refreshClientFiles() {
		Platform.runLater(() -> {
			try {
				refreshClientFiles(Files.list(filesDir).map(Path::toFile)
						.collect(Collectors.toList()));
			} catch (IOException ex) {
				logger.error(ex.getMessage(), ex);
			}
		});
	}

	public void refreshClientFiles(Collection<File> files) {
		Platform.runLater(() -> clientFiles.getItems().setAll(files));
	}

	public void refreshServerFiles(Collection<String> names) {
		Platform.runLater(() -> serverFiles.getItems().setAll(names));
	}

	public void startTransfer(String state, String fileName) {
		Platform.runLater(() -> {
			transferPane.setVisible(true);
			transferState.setText(state + ":");
			transferFile.setText(fileName);
		});
	}

	public void stopTransfer() {
		Platform.runLater(() -> {
			transferPane.setVisible(false);
			transferProgress.setProgress(0.0);
			progressValue.setText("0 %");
		});
	}

	public void updateProgress(double value) {
		Platform.runLater(() -> {
			transferProgress.setProgress(value);
			progressValue.setText(Math.round(value * 100.0) + " %");
		});
	}

	public Path getFilesDir() {
		return filesDir;
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		this.filesDir = Paths.get("").toAbsolutePath();
		this.directoryChooser = new DirectoryChooser();
		directoryChooser.setTitle("Choose directory");
		clientPath.setText(filesDir.toString());
		new Thread(() -> {
			try {
				networkHandler = new ClientNetworkHandler(this);
				refreshClientFiles();
			} catch (Exception ex) {
				logger.error(ex.getMessage(), ex);
			}
		}).start();
	}

	@Override
	public void close() throws Exception {
		networkHandler.close().addListener(future -> {
			if(future.isSuccess()) {
				Platform.exit();
				System.exit(0);
			}
		});
	}
}

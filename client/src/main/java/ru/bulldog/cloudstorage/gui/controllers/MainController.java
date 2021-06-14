package ru.bulldog.cloudstorage.gui.controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.bulldog.cloudstorage.Client;
import ru.bulldog.cloudstorage.event.ActionListener;
import ru.bulldog.cloudstorage.event.EventsHandler;
import ru.bulldog.cloudstorage.event.FilesListener;
import ru.bulldog.cloudstorage.network.ClientNetworkHandler;
import ru.bulldog.cloudstorage.network.Session;
import ru.bulldog.cloudstorage.network.packet.FilePacket;
import ru.bulldog.cloudstorage.network.packet.FileRequest;
import ru.bulldog.cloudstorage.network.packet.FilesListPacket;

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
	@FXML
	public AnchorPane mainWindow;

	private DirectoryChooser directoryChooser;
	private ClientNetworkHandler networkHandler;
	private AuthController authController;
	private EventsHandler eventsHandler;
	private Stage mainStage;
	private Stage authStage;
	private Path filesDir;
	private Alert alert;

	public void sendFile(ActionEvent actionEvent) {
		if (checkConnection()) {
			File file = clientFiles.getSelectionModel().getSelectedItem();
			if (file != null) {
				try {
					Session session = networkHandler.getSession();
					FilePacket packet = new FilePacket(session.getSessionId(), file.toPath());
					networkHandler.sendPacket(packet);
				} catch (Exception ex) {
					logger.error("Send file error: " + file, ex);
				}
			}
		}
	}

	public void requestFile(ActionEvent actionEvent) {
		if (checkConnection()) {
			String name = serverFiles.getSelectionModel().getSelectedItem();
			if (name != null) {
				try {
					Session session = networkHandler.getSession();
					FileRequest packet = new FileRequest(session.getSessionId(), name);
					networkHandler.sendPacket(packet);
				} catch (Exception ex) {
					logger.warn("Request file error: " + name, ex);
				}
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

	public void refreshClientFiles() {
		try {
			refreshClientFiles(Files.list(filesDir).map(Path::toFile)
					.collect(Collectors.toList()));
		} catch (IOException ex) {
			logger.error(ex.getMessage(), ex);
		}
	}

	public void refreshClientFiles(Collection<File> files) {
		clientFiles.getItems().setAll(files);
	}

	public void refreshServerFiles(Collection<String> names) {
		serverFiles.getItems().setAll(names);
	}

	public void startTransfer(String state, String fileName) {
		transferPane.setVisible(true);
		transferState.setText(state + ":");
		transferFile.setText(fileName);
	}

	public void stopTransfer() {
		transferPane.setVisible(false);
		transferProgress.setProgress(0.0);
		progressValue.setText("0 %");
	}

	public void updateProgress(double value) {
		transferProgress.setProgress(value);
		progressValue.setText(Math.round(value * 100.0) + " %");
	}

	public Path getFilesDir() {
		return filesDir;
	}

	private boolean checkConnection() {
		if (networkHandler.isConnected()) {
			return true;
		}
		authStage.show();
		return false;
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		this.filesDir = Paths.get("").toAbsolutePath();
		this.directoryChooser = new DirectoryChooser();
		directoryChooser.setTitle("Choose directory");
		this.alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("Message");
		clientPath.setText(filesDir.toString());
		networkHandler = new ClientNetworkHandler(this);
		eventsHandler = EventsHandler.getInstance();
		registerConnectionListener();
		refreshClientFiles();

		Platform.runLater(() -> {
			mainStage = (Stage) mainWindow.getScene().getWindow();
			try {
				URL authFXML = getClass().getResource("../../fxml/auth_window.fxml");
				FXMLLoader loader = new FXMLLoader(authFXML);
				Parent root = loader.load();
				Scene authWin = new Scene(root);
				authController = loader.getController();
				authController.setNetworkHandler(networkHandler);
				authController.setMainController(this);
				authStage = new Stage();
				authStage.setScene(authWin);
				authStage.initOwner(mainStage);
				authStage.initModality(Modality.WINDOW_MODAL);
				authStage.setResizable(false);
				authStage.show();
			} catch (Exception ex) {
				logger.error("Auth window initialization error.", ex);
			}
		});
	}

	private void registerConnectionListener() {
		eventsHandler.registerListener(new ActionListener() {
			@Override
			public void onMessageReceived(String message) {}

			@Override
			public void onHandleError(String message) {}

			@Override
			public void onDisconnect() {}

			@Override
			public void onConnect() {
				Platform.runLater(authStage::hide);
				registerFileListener();
			}
		});
	}

	private void registerFileListener() {
		eventsHandler.registerListener(new FilesListener() {
			@Override
			public void onFilesList(FilesListPacket filesList) {
				Platform.runLater(() -> refreshServerFiles(filesList.getNames()));
			}

			@Override
			public void onFileStart(String direction, String fileName) {
				Platform.runLater(() -> startTransfer(direction, fileName));
			}

			@Override
			public void onFileProgress(double progress) {
				Platform.runLater(() -> updateProgress(progress));
			}

			@Override
			public void onFileReceived() {
				Platform.runLater(() -> {
					refreshClientFiles();
					stopTransfer();
				});
			}

			@Override
			public void onMessageReceived(String message) {
				Platform.runLater(() -> {
					alert.setAlertType(AlertType.INFORMATION);
					alert.setContentText(message);
					alert.showAndWait();
				});
			}

			@Override
			public void onHandleError(String message) {
				Platform.runLater(() -> {
					alert.setAlertType(AlertType.ERROR);
					alert.setContentText(message);
					alert.showAndWait();
				});}

			@Override
			public void onDisconnect() {
				Platform.runLater(authStage::show);
				eventsHandler.removeListener(this);
			}

			@Override
			public void onConnect() {}
		});
	}

	@Override
	public void close() throws Exception {
		if (networkHandler.isConnected()) {
			networkHandler.close().addListener(future -> {
				if (future.isDone()) {
					Platform.runLater(() -> Client.shutdown(0));
				}
			});
		} else {
			networkHandler.close();
			Client.shutdown(0);
		}
	}
}

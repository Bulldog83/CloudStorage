package ru.bulldog.cloudstorage.gui.controllers;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.bulldog.cloudstorage.Client;
import ru.bulldog.cloudstorage.data.FileInfo;
import ru.bulldog.cloudstorage.data.FileSystem;
import ru.bulldog.cloudstorage.event.ActionListener;
import ru.bulldog.cloudstorage.event.EventsHandler;
import ru.bulldog.cloudstorage.network.ClientNetworkHandler;
import ru.bulldog.cloudstorage.network.Session;
import ru.bulldog.cloudstorage.network.packet.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

public class MainController implements Initializable, AutoCloseable {

	private final static Logger logger = LogManager.getLogger(MainController.class);

	@FXML
	public AnchorPane mainWindow;
	@FXML
	public AnchorPane transferPane;
	@FXML
	public TableView<FileInfo> clientFiles;
	@FXML
	public TableView<FileInfo> serverFiles;
	@FXML
	public TextField clientPath;
	@FXML
	public TextField serverPath;
	@FXML
	public ProgressBar transferProgress;
	@FXML
	public Label progressValue;
	@FXML
	public Label transferState;
	@FXML
	public Label transferFile;
	@FXML
	public Label labCopyTo;
	@FXML
	public TableColumn<FileInfo, String> clientFileName;
	@FXML
	public TableColumn<FileInfo, String> serverFileName;
	@FXML
	public TableColumn<FileInfo, String> clientFileSize;
	@FXML
	public TableColumn<FileInfo, String> serverFileSize;
	@FXML
	public Button btnClientFolderUp;
	@FXML
	public Button btnClientFolder;
	@FXML
	public Button btnServerFolderUp;
	@FXML
	public Button btnFileUpload;
	@FXML
	public Button btnFileDownload;
	@FXML
	public Button btnDoCopy;
	@FXML
	public Button btnDoRename;
	@FXML
	public Button btnCreateFolder;
	@FXML
	public Button btnDoDelete;

	private ObservableList<FileInfo> clientFilesList;
	private ObservableList<FileInfo> serverFilesList;

	private TableView<FileInfo> activeTable;

	private TextInputDialog inputDialog;
	private DirectoryChooser directoryChooser;
	private ClientNetworkHandler networkHandler;
	private AuthController authController;
	private EventsHandler eventsHandler;
	private Stage mainStage;
	private Stage authStage;
	private Path filesDir;
	private Alert alert;

	private void sendFile(FileInfo fileInfo) {
		if (checkConnection()) {
			File file = fileInfo.getSourceFile();
			try {
				Session session = networkHandler.getSession();
				FilePacket packet = new FilePacket(session.getSessionId(), file.toPath());
				networkHandler.sendPacket(packet);
			} catch (Exception ex) {
				logger.error("Send file error: " + file, ex);
			}
		}
	}

	public void requestFile(ActionEvent actionEvent) {
		FileInfo file = serverFiles.getSelectionModel().getSelectedItem();
		if (file != null) {
			requestFile(file);
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
			updateFolder(dir.toPath());
		}
	}

	public void onServerFilesClicked(MouseEvent mouseEvent) {
		activeTable = serverFiles;
		labCopyTo.setText("Download");
		onTableClicked(mouseEvent);
	}

	public void onClientFilesClicked(MouseEvent mouseEvent) {
		activeTable = clientFiles;
		labCopyTo.setText("Upload");
		onTableClicked(mouseEvent);
	}

	public void onTableClicked(MouseEvent mouseEvent) {
		FileInfo selected = activeTable.getSelectionModel().getSelectedItem();
		if (selected == null) {
			btnDoCopy.setDisable(true);
			btnDoDelete.setDisable(true);
			btnDoRename.setDisable(true);
		} else if (selected.isDirectory()) {
			if (mouseEvent.getClickCount() == 2) {
				if (activeTable == clientFiles) {
					updateFolder(filesDir.resolve(selected.getFileName()));
				} else {
					requestFile(selected);
				}
			}
			btnDoCopy.setDisable(true);
			btnDoDelete.setDisable(false);
			btnDoRename.setDisable(false);
		} else {
			btnDoCopy.setDisable(false);
			btnDoDelete.setDisable(false);
			btnDoRename.setDisable(false);
		}
	}

	public void clientFolderUp(ActionEvent actionEvent) {
		Path parent = filesDir.getParent();
		if (parent != null) {
			updateFolder(parent);
		}
	}

	public void serverFolderUp(ActionEvent actionEvent) {
		if (checkConnection()) {
			try {
				Packet packet = new ListRequest("", true);
				networkHandler.sendPacket(packet);
			} catch (Exception ex) {
				logger.warn("Parent folder request error", ex);
			}
		}
	}

	public void onKeyPressed(KeyEvent keyEvent) {
		switch (keyEvent.getCode()) {
			case F4:
				doRename(keyEvent);
				break;
			case F5:
				doCopy(keyEvent);
				break;
			case F7:
				createFolder(keyEvent);
				break;
			case F8:
				doDelete(keyEvent);
				break;
		}
	}

	public void doRename(Event actionEvent) {
		if (activeTable != null) {
			FileInfo fileInfo = activeTable.getSelectionModel().getSelectedItem();
			if (fileInfo != null) {
				inputDialog.setTitle("Rename File");
				inputDialog.setContentText("Enter new file/folder name");
				Optional<String> optionalName = inputDialog.showAndWait();
				optionalName.ifPresent(name -> {
					String newName = name.trim();
					if (newName.equals("")) {
						alert.setContentText("Name can't be empty.");
						alert.setAlertType(AlertType.WARNING);
						alert.showAndWait();
						return;
					}
					if (newName.equals(fileInfo.getFileName())) {
						alert.setContentText("New name the same as exist.");
						alert.setAlertType(AlertType.WARNING);
						alert.showAndWait();
						return;
					}
					if (activeTable == serverFiles) {
						if (checkConnection()) {
							ActionPacket packet = ActionPacket.renameFile(fileInfo.getFileName(), newName);
							networkHandler.sendPacket(packet);
						}
					} else {
						File file = fileInfo.getSourceFile();
						if (file.isDirectory()) {
							FileSystem.renameFile(file.toPath(), "", newName);
						} else {
							Path folder = file.toPath().getParent();
							FileSystem.renameFile(folder, fileInfo.getFileName(), newName);
						}
						refreshClientFiles();
					}
				});
			}
		}
	}

	public void doCopy(Event actionEvent) {
		if (activeTable != null && checkConnection()) {
			FileInfo fileInfo = activeTable.getSelectionModel().getSelectedItem();
			if (fileInfo != null && !fileInfo.isDirectory()) {
				if (activeTable == serverFiles) {
					requestFile(fileInfo);
				} else {
					sendFile(fileInfo);
				}
			}
		}
	}

	public void createFolder(Event actionEvent) {
		if (activeTable != null) {
			inputDialog.setTitle("New Directory");
			inputDialog.setContentText("Enter new file/folder name");
			Optional<String> optionalName = inputDialog.showAndWait();
			optionalName.ifPresent(name -> {
				String folderName = name.trim();
				if (folderName.equals("")) {
					alert.setContentText("Name can't be empty.");
					alert.setAlertType(AlertType.WARNING);
					alert.showAndWait();
					return;
				}
				if (activeTable == serverFiles) {
					if (checkConnection()) {
						ActionPacket packet = ActionPacket.createFolder(folderName);
						networkHandler.sendPacket(packet);
					}
				} else {
					FileSystem.createFolder(filesDir, folderName);
					refreshClientFiles();
				}
			});
		}
	}

	public void doDelete(Event actionEvent) {
	}

	private void requestFile(FileInfo fileInfo) {
		if (checkConnection()) {
			try {
				Session session = networkHandler.getSession();
				Packet packet;
				if (fileInfo.isDirectory()) {
					packet = new ListRequest(fileInfo.getFileName(), false);
				} else {
					packet = new FileRequest(session.getSessionId(), fileInfo.getFileName());
				}
				networkHandler.sendPacket(packet);
			} catch (Exception ex) {
				logger.warn("Request file error: " + fileInfo, ex);
			}
		}
	}

	public void sendFile(ActionEvent actionEvent) {
		FileInfo fileInfo = clientFiles.getSelectionModel().getSelectedItem();
		if (fileInfo != null && !fileInfo.isDirectory()) {
			sendFile(fileInfo);
		}
	}

	public void refreshClientFiles() {
		try {
			refreshClientFiles(Files.list(filesDir).map(FileInfo::new)
					.sorted(FILE_INFO_COMPARATOR).collect(Collectors.toList()));
		} catch (IOException ex) {
			logger.error(ex.getMessage(), ex);
		}
	}

	private void updateFolder(Path path) {
		filesDir = path;
		clientPath.setText(path.toAbsolutePath().toString());
		refreshClientFiles();
	}

	public void refreshClientFiles(Collection<FileInfo> files) {
		clientFilesList.setAll(files);
	}

	public void refreshServerFiles(Collection<FileInfo> files) {
		serverFilesList.setAll(files);
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
		this.networkHandler = new ClientNetworkHandler(this);
		this.eventsHandler = EventsHandler.getInstance();
		this.directoryChooser = new DirectoryChooser();
		this.inputDialog = new TextInputDialog();
		this.alert = new Alert(AlertType.INFORMATION);
		this.activeTable = clientFiles;
		directoryChooser.setTitle("Choose directory");
		clientPath.setText(filesDir.toString());
		alert.setTitle("Message");

		this.clientFilesList = FXCollections.observableArrayList();
		SortedList<FileInfo> clientSorted = new SortedList<>(clientFilesList);
		clientSorted.setComparator(FILE_INFO_COMPARATOR);
		clientFiles.setItems(clientSorted);
		this.serverFilesList = FXCollections.observableArrayList();
		SortedList<FileInfo> serverSorted = new SortedList<>(serverFilesList);
		serverSorted.setComparator(FILE_INFO_COMPARATOR);
		serverFiles.setItems(serverSorted);

		registerEventListeners();
		refreshClientFiles();
		initFileTables();
		loadIcons();

		Platform.runLater(() -> {
			mainStage = (Stage) mainWindow.getScene().getWindow();
			try {
				URL authFXML = getClass().getResource("../../fxml/auth_window.fxml");
				FXMLLoader loader = new FXMLLoader(authFXML);
				Parent root = loader.load();
				Scene authWin = new Scene(root);
				authController = loader.getController();
				authController.setNetworkHandler(networkHandler);
				authStage = new Stage();
				authStage.setScene(authWin);
				authStage.initOwner(mainStage);
				authStage.initModality(Modality.WINDOW_MODAL);
				authStage.setResizable(false);
				authStage.setTitle("Authorization");
				authStage.show();
			} catch (Exception ex) {
				logger.error("Auth window initialization error.", ex);
			}
		});
	}

	private void initFileTables() {
		NumberFormat decimalFormat = DecimalFormat.getInstance();
		decimalFormat.setGroupingUsed(true);
		ReadOnlyObjectWrapper<String> strDir = new ReadOnlyObjectWrapper<>("[DIR]");

		Callback<TableColumn.CellDataFeatures<FileInfo, String>, ObservableValue<String>> fileNameCell = column ->
				new ReadOnlyObjectWrapper<>(column.getValue().getFileName());
		Callback<TableColumn.CellDataFeatures<FileInfo, String>, ObservableValue<String>> fileSizeCell = column -> {
			FileInfo fileInfo = column.getValue();
			if (fileInfo.isDirectory()) {
				return strDir;
			}
			long size = fileInfo.getFileSize();
			return new ReadOnlyObjectWrapper<>(decimalFormat.format(size));
		};

		clientFileName.setCellValueFactory(fileNameCell);
		clientFileSize.setCellValueFactory(fileSizeCell);
		serverFileName.setCellValueFactory(fileNameCell);
		serverFileSize.setCellValueFactory(fileSizeCell);

		Callback<TableView<FileInfo>, TableRow<FileInfo>> fileInfoRowFactory = table -> {
			ContextMenu rowMenu = new ContextMenu();
			MenuItem copyItem = new MenuItem();
			MenuItem renameItem = new MenuItem("Rename");
			MenuItem deleteItem = new MenuItem("Delete");
			copyItem.setText(table == clientFiles ? "Upload" : "Download");
			copyItem.setOnAction(this::doCopy);
			renameItem.setOnAction(this::doRename);
			deleteItem.setOnAction(this::doDelete);

			TableRow<FileInfo> tableRow = new TableRow<FileInfo>() {
				@Override
				protected void updateItem(FileInfo item, boolean empty) {
					super.updateItem(item, empty);
					List<MenuItem> menuItems = rowMenu.getItems();
					menuItems.clear();
					if (!empty && item != null) {
						if (!item.isDirectory()) {
							menuItems.add(copyItem);
						}
						menuItems.add(renameItem);
						menuItems.add(deleteItem);
					}
				}
			};
			tableRow.setContextMenu(rowMenu);

			return tableRow;
		};
		clientFiles.setRowFactory(fileInfoRowFactory);
		serverFiles.setRowFactory(fileInfoRowFactory);
	}

	private void loadIcons() {
		URL openURL = getClass().getResource("/toolbarButtonGraphics/general/Open24.gif");
		if (openURL != null) {
			try (InputStream imageStream = openURL.openStream()) {
				ImageView openIcon = new ImageView(new Image(imageStream));
				openIcon.setFitHeight(20.0);
				openIcon.setFitWidth(20.0);
				btnClientFolder.setGraphic(openIcon);
				btnClientFolder.setText(null);
			} catch (IOException ex) {
				logger.warn("Load icon error: " + openURL);
			}
		}
		URL upURL = getClass().getResource("/toolbarButtonGraphics/navigation/Up24.gif");
		if (upURL != null) {
			try (InputStream imageStream = upURL.openStream()) {
				Image upImage = new Image(imageStream);
				ImageView upIconClient = new ImageView(upImage);
				upIconClient.setFitHeight(20.0);
				upIconClient.setFitWidth(20.0);
				btnClientFolderUp.setGraphic(upIconClient);
				btnClientFolderUp.setText(null);

				ImageView upIconServer = new ImageView(upImage);
				upIconServer.setFitHeight(20.0);
				upIconServer.setFitWidth(20.0);
				btnServerFolderUp.setGraphic(upIconServer);
				btnServerFolderUp.setText(null);
			} catch (IOException ex) {
				logger.warn("Load icon error: " + upURL);
			}
		}
		URL forwardURL = getClass().getResource("/toolbarButtonGraphics/navigation/Forward24.gif");
		if (forwardURL != null) {
			try (InputStream imageStream = forwardURL.openStream()) {
				ImageView forwardIcon = new ImageView(new Image(imageStream));
				forwardIcon.setFitHeight(20.0);
				forwardIcon.setFitWidth(20.0);
				btnFileUpload.setGraphic(forwardIcon);
				btnFileUpload.setText(null);
			} catch (IOException ex) {
				logger.warn("Load icon error: " + forwardURL);
			}
		}
		URL backURL = getClass().getResource("/toolbarButtonGraphics/navigation/Back24.gif");
		if (backURL != null) {
			try (InputStream imageStream = backURL.openStream()) {
				ImageView backIcon = new ImageView(new Image(imageStream));
				backIcon.setFitHeight(20.0);
				backIcon.setFitWidth(20.0);
				btnFileDownload.setGraphic(backIcon);
				btnFileDownload.setText(null);
			} catch (IOException ex) {
				logger.warn("Load icon error: " + backURL);
			}
		}
	}

	private void registerEventListeners() {
		ActionListener runtimeListener = new ActionListener() {
			@Override
			public void onFilesList(FilesListPacket filesList) {
				Platform.runLater(() -> {
					serverPath.setText(filesList.getFolder());
					refreshServerFiles(filesList.getFiles());
				});
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
				});
			}
		};
		ActionListener connectionListener = new ActionListener() {
			@Override
			public void onConnect() {
				Platform.runLater(authStage::hide);
				eventsHandler.registerListener(runtimeListener);
			}

			@Override
			public void onDisconnect() {
				Platform.runLater(authStage::show);
				eventsHandler.removeListener(runtimeListener);
			}
		};
		eventsHandler.registerListener(connectionListener);
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

	private final static Comparator<FileInfo> FILE_INFO_COMPARATOR = (file, other) -> {
		if (file.isDirectory()) return -1;
		if (other.isDirectory()) return 1;
		return file.getFileName().compareTo(other.getFileName());
	};
}

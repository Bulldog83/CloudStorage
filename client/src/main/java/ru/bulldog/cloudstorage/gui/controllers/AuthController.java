package ru.bulldog.cloudstorage.gui.controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import ru.bulldog.cloudstorage.network.ClientNetworkHandler;

import java.net.URL;
import java.util.ResourceBundle;

public class AuthController implements Initializable {

	@FXML
	public VBox registerPane;
	@FXML
	public VBox authPane;
	@FXML
	public TextField emailField;
	@FXML
	public PasswordField passwordField;
	@FXML
	public TextField passwordRepeat;
	@FXML
	public Label labStatus;
	@FXML
	public CheckBox rememberPassword;
	@FXML
	public TextField nicknameField;
	@FXML
	public AnchorPane authWindow;

	private MainController mainController;
	private ClientNetworkHandler networkHandler;
	private Stage authStage;

	public AuthController setController(MainController controller) {
		this.mainController = controller;
		return this;
	}

	public AuthController setNetworkHandler(ClientNetworkHandler networkHandler) {
		this.networkHandler = networkHandler;
		return this;
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		Platform.runLater(() -> authStage = (Stage) authWindow.getScene().getWindow());
	}

	public void doConnect(ActionEvent actionEvent) {
		networkHandler.connect(() -> Platform.runLater(authStage::hide));
	}

	public void openRegistration(ActionEvent actionEvent) {
		emailField.requestFocus();
		authPane.setVisible(false);
		registerPane.setVisible(true);
	}

	public void doRegistration(ActionEvent actionEvent) {
	}

	public void doCancel(ActionEvent actionEvent) {
		emailField.requestFocus();
		authPane.setVisible(true);
		registerPane.setVisible(false);
	}
}

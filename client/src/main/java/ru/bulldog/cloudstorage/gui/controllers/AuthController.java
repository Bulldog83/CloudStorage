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
import ru.bulldog.cloudstorage.event.ActionListener;
import ru.bulldog.cloudstorage.event.EventsHandler;
import ru.bulldog.cloudstorage.network.ClientNetworkHandler;
import ru.bulldog.cloudstorage.network.packet.AuthData;
import ru.bulldog.cloudstorage.network.packet.RegistrationData;

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

	private ClientNetworkHandler networkHandler;
	private Stage authStage;

	public void setNetworkHandler(ClientNetworkHandler networkHandler) {
		this.networkHandler = networkHandler;
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		Platform.runLater(() -> authStage = (Stage) authWindow.getScene().getWindow());
		registerListeners();
	}

	public void doConnect(ActionEvent actionEvent) {
		String email = emailField.getText().trim().toLowerCase();
		String password = passwordField.getText().trim();
		if (email.equals("")) {
			labStatus.setText("Email can't be empty.");
			return;
		}
		if (password.equals("")) {
			labStatus.setText("Password can't be empty.");
			return;
		}
		labStatus.setText("");
		networkHandler.connect(new AuthData(email, password));
	}

	public void doRegistration(ActionEvent actionEvent) {
		String email = emailField.getText().trim().toLowerCase();
		String password = passwordField.getText().trim();
		String nickname = nicknameField.getText().trim();
		if (email.equals("")) {
			labStatus.setText("Email can't be empty.");
			return;
		}
		if (password.equals("")) {
			labStatus.setText("Password can't be empty.");
			return;
		}
		if (nickname.equals("")) {
			labStatus.setText("Nickname can't be empty.");
			return;
		}
		labStatus.setText("");
		networkHandler.connect(new RegistrationData(email, password, nickname));
	}

	public void openRegistration(ActionEvent actionEvent) {
		emailField.requestFocus();
		authPane.setVisible(false);
		registerPane.setVisible(true);
		authStage.setTitle("Registration");
	}

	public void doCancel(ActionEvent actionEvent) {
		emailField.requestFocus();
		authPane.setVisible(true);
		registerPane.setVisible(false);
		authStage.setTitle("Authorization");
	}

	private void registerListeners() {
		EventsHandler.getInstance().registerListener(new ActionListener() {
			@Override
			public void onMessageReceived(String message) {
				Platform.runLater(() -> labStatus.setText(message));
			}

			@Override
			public void onHandleError(String message) {
				Platform.runLater(() -> labStatus.setText("Error: " + message));
			}

			@Override
			public void onConnect() {}

			@Override
			public void onDisconnect() {}
		});
	}
}

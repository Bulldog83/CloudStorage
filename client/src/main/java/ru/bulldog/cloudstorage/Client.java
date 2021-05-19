package ru.bulldog.cloudstorage;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.File;
import java.net.URI;
import java.net.URL;

public class Client extends Application {

	@Override
	public void start(Stage primaryStage) throws Exception {
		URL mainFXML = getClass().getResource("fxml/main_window.fxml");
		FXMLLoader loader = new FXMLLoader(mainFXML);
		Parent root = loader.load();
		Scene mainWin = new Scene(root);
		primaryStage.setScene(mainWin);
		primaryStage.show();
	}
}

package ru.bulldog.cloudstorage;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.bulldog.cloudstorage.gui.controllers.MainController;

import java.net.URL;

public class Client extends Application {

	public final static Logger LOGGER = LogManager.getLogger(Client.class);

	@Override
	public void start(Stage primaryStage) throws Exception {
		URL mainFXML = getClass().getResource("fxml/main_window.fxml");
		FXMLLoader loader = new FXMLLoader(mainFXML);
		try(MainController ignored = loader.getController()) {
			Parent root = loader.load();
			Scene mainWin = new Scene(root);
			primaryStage.setScene(mainWin);
			primaryStage.setOnCloseRequest(event -> {
				Platform.exit();
				System.exit(0);
			});
			primaryStage.show();
		}
	}
}

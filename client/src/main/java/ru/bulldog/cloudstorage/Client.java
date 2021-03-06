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
		Parent root = loader.load();
		Scene mainWin = new Scene(root);
		MainController controller = loader.getController();
		primaryStage.setScene(mainWin);
		primaryStage.setOnCloseRequest(event -> {
			try {
				controller.close();
			} catch (Exception ex) {
				LOGGER.error("Application close error.", ex);
				shutdown(1);
			}
		});
		primaryStage.setTitle("Cloud Storage");
		primaryStage.show();
	}

	public static void shutdown(int status) {
		Platform.exit();
		System.exit(status);
	}
}

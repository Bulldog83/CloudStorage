package ru.bulldog.cloudstorage.gui.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class MainController implements Initializable {

	@FXML
	public ListView<File> clientFiles;
	@FXML
	public ListView<File> serverFiles;

	public void sendFile(ActionEvent actionEvent) {
	}

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		File runDir = new File(".");
		clientFiles.getItems().addAll(runDir.listFiles());
	}
}

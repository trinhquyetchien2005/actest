package com.actest.server.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableView;
import javafx.scene.image.ImageView;

public class MainController {

    @FXML private Button btn1;
    @FXML private Button btn2;
    @FXML private Button btn3;
    @FXML private Button btnSettings;
    @FXML private TableView<?> tableView;
    @FXML private ImageView imageView;

    @FXML
    public void initialize() {
        System.out.println("Controller initialized");
    }
}

package com.actest.server;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Screen;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/actest/server/fxml/dashboard.fxml"));
        Pane root = loader.load();

        Scene scene = new Scene(root);

        // Lấy tỉ lệ DPI của màn hình
        double scale = Screen.getPrimary().getOutputScaleX();
        root.setScaleX(scale);
        root.setScaleY(scale);

        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

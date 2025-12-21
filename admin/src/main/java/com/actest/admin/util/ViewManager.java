package com.actest.admin.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class ViewManager {
    private static ViewManager instance;
    private Stage primaryStage;

    private ViewManager() {
    }

    public static ViewManager getInstance() {
        if (instance == null) {
            instance = new ViewManager();
        }
        return instance;
    }

    public void setPrimaryStage(Stage stage) {
        this.primaryStage = stage;
    }

    public Object switchView(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            Scene scene = new Scene(root, 800, 600);
            primaryStage.setScene(scene);
            primaryStage.show();
            return loader.getController();
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to load view: " + fxmlPath);
            return null;
        }
    }

    public Object switchView(String fxmlPath, String title) {
        Object controller = switchView(fxmlPath);
        if (primaryStage != null) {
            primaryStage.setTitle(title);
        }
        return controller;
    }
}

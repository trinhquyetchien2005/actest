package com.actest.client;

import javafx.application.Application;
import javafx.stage.Stage;

public class ClientApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        com.actest.client.util.ViewManager.getInstance().setPrimaryStage(stage);
        stage.setMaximized(true);
        com.actest.client.util.ViewManager.getInstance().switchView("/com/actest/client/view/login.fxml",
                "ACTEST Client - Login");
    }

    public static void main(String[] args) {
        // Lock removed to allow multiple clients

        // com.actest.client.repository.DatabaseConnection.initializeDatabase(); //
        // Removed
        launch();
    }
}

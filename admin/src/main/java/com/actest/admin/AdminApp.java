package com.actest.admin;

import javafx.application.Application;
import javafx.stage.Stage;

public class AdminApp extends Application {
    public static String currentUserEmail;

    @Override
    public void start(Stage stage) throws Exception {
        com.actest.admin.util.ViewManager.getInstance().setPrimaryStage(stage);
        stage.setMaximized(true);
        com.actest.admin.util.ViewManager.getInstance().switchView("/com/actest/admin/view/login.fxml",
                "ACTEST Admin - Login");
    }

    public static void main(String[] args) {
        // Single instance check removed for testing
        com.actest.admin.repository.DatabaseConnection.initializeDatabase();
        launch();
    }
}

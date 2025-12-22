package com.actest.client.controller;

import com.actest.client.network.DiscoveryClient;
import com.actest.client.service.AuthService;
import com.actest.client.util.ViewManager;
import javafx.fxml.FXML;
import java.util.List;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;

public class LoginController {

    @FXML
    private TextField nameField;

    @FXML
    private ListView<String> serverListView;

    private final AuthService authService = new AuthService();
    private final DiscoveryClient discoveryClient = new DiscoveryClient();

    @FXML
    public void initialize() {
        scanForServers();
    }

    @FXML
    private void handleRescan() {
        serverListView.getItems().clear();
        scanForServers();
    }

    private void scanForServers() {
        // Start discovery in a background thread to avoid blocking UI
        new Thread(() -> {
            List<String> servers = discoveryClient.discoverServers();
            javafx.application.Platform.runLater(() -> {
                serverListView.getItems().setAll(servers);
            });
        }).start();
    }

    @FXML
    private void handleConnect() {
        String name = nameField.getText();
        String selectedServer = serverListView.getSelectionModel().getSelectedItem();

        if (name.isEmpty()) {
            showAlert("Please enter name");
            return;
        }

        if (selectedServer != null) {
            // Parse IP from "ServerName (IP)"
            String ip = selectedServer.substring(selectedServer.lastIndexOf("(") + 1, selectedServer.lastIndexOf(")"));
            System.out.println("Connecting to " + ip);

            if (authService.loginOffline(ip, name)) {
                ViewManager.getInstance().switchView("/com/actest/client/view/waiting.fxml", "ACTEST Client - Waiting");

                AuthService.getTcpClient().setListener(message -> {
                    if ("ACCEPTED".equals(message)) {
                        javafx.application.Platform.runLater(() -> {
                            ViewManager.getInstance().switchView("/com/actest/client/view/dashboard.fxml",
                                    "ACTEST Client - Dashboard");
                        });
                    } else if ("REJECTED".equals(message)) {
                        javafx.application.Platform.runLater(() -> {
                            showAlert("Connection rejected by admin.");
                            ViewManager.getInstance().switchView("/com/actest/client/view/login.fxml",
                                    "ACTEST Client - Login");
                        });
                    }
                });
            }
        } else {
            showAlert("Please select a server");
        }
    }

    private void showAlert(String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
        alert.setTitle("Warning");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

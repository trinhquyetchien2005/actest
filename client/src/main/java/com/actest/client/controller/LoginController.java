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
    private final DiscoveryClient discoveryClient = new DiscoveryClient(); // Added field

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
        String selectedServer = (String) serverListView.getSelectionModel().getSelectedItem(); // Un-commented and
                                                                                               // modified

        if (name.isEmpty()) {
            // Show alert
            System.out.println("Please enter name");
            return;
        }

        if (selectedServer != null) {
            // Parse IP from "ServerName (IP)"
            String ip = selectedServer.substring(selectedServer.lastIndexOf("(") + 1, selectedServer.lastIndexOf(")"));
            System.out.println("Connecting to " + ip);

            if (authService.loginOffline(ip, name)) { // Modified to use parsed IP
                ViewManager.getInstance().switchView("/com/actest/client/view/waiting.fxml", "ACTEST Client - Waiting");

                AuthService.getTcpClient().setListener(message -> {
                    if ("ACCEPTED".equals(message)) {
                        javafx.application.Platform.runLater(() -> {
                            ViewManager.getInstance().switchView("/com/actest/client/view/dashboard.fxml",
                                    "ACTEST Client - Dashboard");
                        });
                    } else if ("REJECTED".equals(message)) {
                        javafx.application.Platform.runLater(() -> {
                            // Handle rejection (e.g., show alert and go back to login)
                            System.out.println("Connection rejected by admin.");
                            ViewManager.getInstance().switchView("/com/actest/client/view/login.fxml",
                                    "ACTEST Client - Login");
                        });
                    }
                });
            }
        } else {
            System.out.println("Please select a server"); // Added else block
        }
    }
}

package com.actest.admin.controller;

import com.actest.admin.network.DiscoveryClient;
import com.actest.admin.service.AuthService;
import com.actest.admin.util.ViewManager;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.util.List;

public class LoginController {

    @FXML
    private VBox loginForm;
    @FXML
    private VBox registerForm;

    // Login Fields
    @FXML
    private TextField emailField;
    @FXML
    private PasswordField passwordField;

    // Register Fields
    @FXML
    private TextField regNameField;
    @FXML
    private TextField regEmailField;
    @FXML
    private PasswordField regPasswordField;
    @FXML
    private PasswordField regConfirmPasswordField;

    // Offline Fields
    @FXML
    private TextField serverNameField;
    @FXML
    private Label availabilityLabel;
    @FXML
    private Button startServerBtn;

    // OTP Overlay
    @FXML
    private VBox otpOverlay;
    @FXML
    private TextField otpField;

    private final AuthService authService = new AuthService();
    private final DiscoveryClient discoveryClient = new DiscoveryClient();

    @FXML
    private void showRegisterForm() {
        loginForm.setVisible(false);
        registerForm.setVisible(true);
    }

    @FXML
    private void showLoginForm() {
        registerForm.setVisible(false);
        loginForm.setVisible(true);
    }

    @FXML
    private void handleOnlineLogin(ActionEvent event) {
        String email = emailField.getText();
        String password = passwordField.getText();

        if (email.isEmpty() || password.isEmpty()) {
            showAlert("Error", "Please enter email and password.");
            return;
        }

        try {
            String hashedPassword = hashPassword(password);
            if (authService.login(email, hashedPassword)) {
                System.out.println("Online login successful for: " + email);
                com.actest.admin.AdminApp.currentUserEmail = email;
                ViewManager.getInstance().switchView("/com/actest/admin/view/dashboard_online.fxml",
                        "Admin Dashboard - Online");
            } else {
                showAlert("Login Failed", "Invalid email or password.");
            }
        } catch (java.net.ConnectException e) {
            showAlert("Connection Error", "Could not connect to the server. Please check if the server is running.");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Network Error", "An unexpected error occurred: " + e.getMessage());
        }
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        String name = regNameField.getText();
        String email = regEmailField.getText();
        String password = regPasswordField.getText();
        String confirmPassword = regConfirmPasswordField.getText();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showAlert("Error", "Please fill in all fields.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showAlert("Error", "Passwords do not match.");
            return;
        }

        String hashedPassword = hashPassword(password);

        try {
            if (authService.register(email, hashedPassword, name)) {
                otpOverlay.setVisible(true);
                otpField.clear();
                otpField.requestFocus();
            } else {
                showAlert("Registration Failed", "Could not initiate registration. Email might be taken.");
            }
        } catch (java.net.ConnectException e) {
            showAlert("Connection Error", "Could not connect to the server. Please check if the server is running.");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Network Error", "An unexpected error occurred: " + e.getMessage());
        }
    }

    private String hashPassword(String password) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (int i = 0; i < encodedhash.length; i++) {
                String hex = Integer.toHexString(0xff & encodedhash[i]);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @FXML
    private void handleVerifyOtp(ActionEvent event) {
        String email = regEmailField.getText(); // Use register email
        String otp = otpField.getText();

        if (otp.isEmpty()) {
            showAlert("Error", "Please enter OTP.");
            return;
        }

        try {
            if (authService.verifyOtp(email, otp)) {
                otpOverlay.setVisible(false);
                showAlert("Success", "Registration successful! You can now login.");
                showLoginForm();
            } else {
                showAlert("Verification Failed", "Invalid OTP.");
            }
        } catch (java.net.ConnectException e) {
            showAlert("Connection Error", "Could not connect to the server. Please check if the server is running.");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Network Error", "An unexpected error occurred: " + e.getMessage());
        }
    }

    @FXML
    private void handleCancelOtp(ActionEvent event) {
        otpOverlay.setVisible(false);
    }

    @FXML
    private void handleCheckAvailability(ActionEvent event) {
        String serverName = serverNameField.getText();
        if (serverName.isEmpty()) {
            showAlert("Error", "Please enter a server name.");
            return;
        }

        availabilityLabel.setText("Checking...");
        availabilityLabel.setStyle("-fx-text-fill: black;");
        startServerBtn.setDisable(true);

        new Thread(() -> {
            List<String> servers = discoveryClient.discoverServers();
            boolean exists = servers.contains(serverName);

            Platform.runLater(() -> {
                if (exists) {
                    availabilityLabel.setText("Name '" + serverName + "' is already taken.");
                    availabilityLabel.setStyle("-fx-text-fill: red;");
                    startServerBtn.setDisable(true);
                } else {
                    availabilityLabel.setText("Name '" + serverName + "' is available.");
                    availabilityLabel.setStyle("-fx-text-fill: green;");
                    startServerBtn.setDisable(false);
                }
            });
        }).start();
    }

    @FXML
    private void handleStartServer(ActionEvent event) {
        String serverName = serverNameField.getText();
        if (serverName.isEmpty()) {
            showAlert("Error", "Please enter a server name.");
            return;
        }

        com.actest.admin.offline.controller.DashboardController.pendingServerName = serverName;

        System.out.println("Starting local server: " + serverName);
        ViewManager.getInstance().switchView("/com/actest/admin/view/dashboard_offline.fxml",
                "Admin Dashboard - Offline");
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}

package com.actest.admin.online.controller;

import com.actest.admin.service.AuthService;
import com.actest.admin.service.ExamService;
import com.actest.admin.util.ViewManager;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class DashboardController {

    @FXML
    private VBox serverExamsView;
    @FXML
    private VBox myExamsView;
    @FXML
    private VBox profileView;

    @FXML
    private javafx.scene.layout.FlowPane serverExamCardsContainer;
    @FXML
    private javafx.scene.layout.FlowPane myExamCardsContainer;

    @FXML
    private TextField profileNameField;
    @FXML
    private PasswordField profilePasswordField;
    @FXML
    private TextField profileEmailField;

    private final ExamService examService = new ExamService();
    private final AuthService authService = new AuthService();

    @FXML
    public void initialize() {
        refreshExamLists();
    }

    @FXML
    private TextField searchField;

    @FXML
    private void handleSearch() {
        String query = searchField.getText() != null ? searchField.getText().toLowerCase() : "";
        serverExamCardsContainer.getChildren().clear();
        for (com.actest.admin.model.Exam exam : examService.getServerExams()) {
            if (exam.getName().toLowerCase().contains(query)) {
                serverExamCardsContainer.getChildren().add(createServerExamCard(exam));
            }
        }
    }

    private void refreshExamLists() {
        serverExamCardsContainer.getChildren().clear();
        for (com.actest.admin.model.Exam exam : examService.getServerExams()) {
            serverExamCardsContainer.getChildren().add(createServerExamCard(exam));
        }

        myExamCardsContainer.getChildren().clear();
        for (com.actest.admin.model.Exam exam : examService.getLocalExams()) {
            myExamCardsContainer.getChildren().add(createOnlineExamCard(exam));
        }
    }

    @FXML
    private void showServerExams() {
        serverExamsView.setVisible(true);
        myExamsView.setVisible(false);
        profileView.setVisible(false);
        refreshExamLists();
    }

    @FXML
    private void showMyExams() {
        serverExamsView.setVisible(false);
        myExamsView.setVisible(true);
        profileView.setVisible(false);
        refreshExamLists();
    }

    @FXML
    private void showProfile() {
        serverExamsView.setVisible(false);
        myExamsView.setVisible(false);
        profileView.setVisible(true);
        profileEmailField.setText(com.actest.admin.AdminApp.currentUserEmail);
    }

    @FXML
    private void handleLogout() {
        ViewManager.getInstance().switchView("/com/actest/admin/view/login.fxml", "ACTEST Admin - Login");
    }

    @FXML
    private void handleChat() {
        ViewManager.getInstance().switchView("/com/actest/admin/view/chat.fxml", "ACTEST Admin - Chat");
    }

    @FXML
    private void handleCreateExam() {
        Object controller = ViewManager.getInstance().switchView("/com/actest/admin/view/exam_detail.fxml",
                "ACTEST Admin - Create Exam");
        if (controller instanceof com.actest.admin.controller.ExamDetailController) {
            ((com.actest.admin.controller.ExamDetailController) controller)
                    .setPreviousView("/com/actest/admin/view/dashboard_online.fxml", "Admin Dashboard - Online");
        }
    }

    @FXML
    private void handleUpdateProfile() {
        String name = profileNameField.getText();
        String password = profilePasswordField.getText();
        String newEmail = profileEmailField.getText();
        String currentEmail = com.actest.admin.AdminApp.currentUserEmail;

        if (newEmail == null || newEmail.isEmpty()) {
            newEmail = currentEmail;
        }

        try {
            if (currentEmail != null && !newEmail.equals(currentEmail)) {
                // Email change flow
                if (authService.checkEmail(newEmail)) {
                    if (authService.initiateEmailChange(currentEmail, newEmail)) {
                        // Show OTP Dialog
                        javafx.scene.control.TextInputDialog dialog = new javafx.scene.control.TextInputDialog();
                        dialog.setTitle("Email Verification");
                        dialog.setHeaderText("Enter OTP sent to " + newEmail);
                        dialog.setContentText("OTP:");

                        java.util.Optional<String> result = dialog.showAndWait();
                        if (result.isPresent()) {
                            String otp = result.get();
                            if (authService.verifyEmailChange(currentEmail, newEmail, otp)) {
                                // Email verified and changed
                                com.actest.admin.AdminApp.currentUserEmail = newEmail; // Update local session
                                // Now update other profile info
                                updateProfileInfo(newEmail, name, password);
                            } else {
                                showAlert("Error", "Invalid OTP.");
                            }
                        }
                    } else {
                        showAlert("Error", "Failed to initiate email change.");
                    }
                } else {
                    showAlert("Error", "Email already in use.");
                }
            } else {
                // Just update name/password
                updateProfileInfo(currentEmail, name, password);
            }
        } catch (java.net.ConnectException e) {
            showAlert("Connection Error", "Could not connect to the server.");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "An unexpected error occurred: " + e.getMessage());
        }
    }

    private void updateProfileInfo(String email, String name, String password) {
        try {
            if (authService.updateProfile(email, name, password)) {
                showAlert("Success", "Profile updated successfully!");
            } else {
                showAlert("Error", "Failed to update profile.");
            }
        } catch (java.net.ConnectException e) {
            showAlert("Connection Error", "Could not connect to the server.");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "An unexpected error occurred: " + e.getMessage());
        }
    }

    private void showAlert(String title, String content) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private javafx.scene.layout.VBox createOnlineExamCard(com.actest.admin.model.Exam exam) {
        javafx.scene.layout.VBox card = new javafx.scene.layout.VBox(10);
        card.setStyle(
                "-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 5; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 0); -fx-min-width: 200; -fx-pref-width: 200; -fx-min-height: 200; -fx-pref-height: 200;");

        javafx.scene.control.Label nameLabel = new javafx.scene.control.Label(exam.getName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        nameLabel.setWrapText(true);

        javafx.scene.control.Label durationLabel = new javafx.scene.control.Label(
                "Duration: " + exam.getDuration() + " mins");
        durationLabel.setStyle("-fx-text-fill: grey; -fx-font-size: 12px;");

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.VBox.setVgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        javafx.scene.control.Button uploadBtn = new javafx.scene.control.Button("Upload");
        uploadBtn.getStyleClass().add("button-secondary");
        uploadBtn.setMaxWidth(Double.MAX_VALUE);

        // Check if exam exists on server
        boolean existsOnServer = false;
        for (com.actest.admin.model.Exam serverExam : examService.getServerExams()) {
            if (serverExam.getName().equals(exam.getName())) {
                existsOnServer = true;
                break;
            }
        }

        if (existsOnServer) {
            uploadBtn.setVisible(false);
            uploadBtn.setManaged(false);
        }

        uploadBtn.setOnAction(e -> {
            if (examService.uploadExam(exam)) {
                showAlert("Success", "Exam uploaded successfully!");
                refreshExamLists(); // Refresh to hide upload button
            } else {
                showAlert("Error", "Failed to upload exam.");
            }
        });

        card.getChildren().addAll(nameLabel, durationLabel, spacer, uploadBtn);
        return card;
    }

    private javafx.scene.layout.VBox createServerExamCard(com.actest.admin.model.Exam exam) {
        javafx.scene.layout.VBox card = new javafx.scene.layout.VBox(10);
        card.setStyle(
                "-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 5; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 0); -fx-min-width: 200; -fx-pref-width: 200; -fx-min-height: 200; -fx-pref-height: 200;");

        javafx.scene.control.Label nameLabel = new javafx.scene.control.Label(exam.getName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        nameLabel.setWrapText(true);

        javafx.scene.control.Label durationLabel = new javafx.scene.control.Label(
                "Duration: " + exam.getDuration() + " mins");
        durationLabel.setStyle("-fx-text-fill: grey; -fx-font-size: 12px;");

        javafx.scene.control.Label creatorLabel = new javafx.scene.control.Label(
                "Created by: " + (exam.getCreatorName() != null ? exam.getCreatorName() : "Unknown"));
        creatorLabel.setStyle("-fx-text-fill: grey; -fx-font-size: 12px; -fx-font-style: italic;");

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.VBox.setVgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        javafx.scene.control.Button viewDetailsBtn = new javafx.scene.control.Button("View Details");
        viewDetailsBtn.getStyleClass().add("button-secondary");
        viewDetailsBtn.setMaxWidth(Double.MAX_VALUE);
        viewDetailsBtn.setOnAction(e -> {
            Object controller = ViewManager.getInstance().switchView("/com/actest/admin/view/exam_detail.fxml",
                    "Exam Details - " + exam.getName());
            if (controller instanceof com.actest.admin.controller.ExamDetailController) {
                com.actest.admin.controller.ExamDetailController detailController = (com.actest.admin.controller.ExamDetailController) controller;
                detailController.setPreviousView("/com/actest/admin/view/dashboard_online.fxml",
                        "Admin Dashboard - Online");
                detailController.setReadOnly(true);
                detailController.setLoadFromServer(true);
                detailController.setExam(exam);
            }
        });

        javafx.scene.control.Button downloadBtn = new javafx.scene.control.Button("Download");
        downloadBtn.getStyleClass().add("button");
        downloadBtn.setMaxWidth(Double.MAX_VALUE);

        // Hide download button if current user is the creator OR if exam already exists
        // locally
        boolean existsLocally = false;
        for (com.actest.admin.model.Exam localExam : examService.getLocalExams()) {
            if (localExam.getName().equals(exam.getName())) {
                existsLocally = true;
                break;
            }
        }

        if (existsLocally) {
            downloadBtn.setVisible(false);
            downloadBtn.setManaged(false);
        }

        downloadBtn.setOnAction(e -> {
            if (examService.downloadExam(exam.getId())) {
                showAlert("Success", "Exam downloaded successfully!");
                refreshExamLists();
            } else {
                showAlert("Error", "Failed to download exam.");
            }
        });

        card.getChildren().addAll(nameLabel, durationLabel, creatorLabel, spacer, viewDetailsBtn, downloadBtn);
        return card;
    }
}

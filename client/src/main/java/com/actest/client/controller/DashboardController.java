package com.actest.client.controller;

import com.actest.client.service.AuthService;
// import com.actest.client.service.ExamService;
import com.actest.client.util.ViewManager;
import javafx.fxml.FXML;
import javafx.scene.control.ListView;

public class DashboardController {

    // private final ExamService examService = new ExamService(); // Removed
    private final AuthService authService = new AuthService(); // Added AuthService instance
    @FXML
    private javafx.scene.layout.VBox examContainer;

    @FXML
    public void initialize() {
        com.actest.client.network.Client client = authService.getTcpClient();
        if (client != null) {
            client.setListener(message -> {
                if (message.startsWith("START_EXAM:")) {
                    String base64Exam = message.substring(11);
                    String examJson = new String(java.util.Base64.getDecoder().decode(base64Exam));
                    try {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        com.actest.client.model.Exam exam = mapper.readValue(examJson,
                                com.actest.client.model.Exam.class);

                        javafx.application.Platform.runLater(() -> addExamCard(exam));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                } else if (message.startsWith("RESUME_EXAM:")) {
                    String content = message.substring(12);
                    int splitIndex = content.indexOf("|||"); // Separator
                    if (splitIndex != -1) {
                        String base64Exam = content.substring(0, splitIndex);
                        String base64Answers = content.substring(splitIndex + 3);
                        String examJson = new String(java.util.Base64.getDecoder().decode(base64Exam));
                        String answersJson = new String(java.util.Base64.getDecoder().decode(base64Answers));

                        try {
                            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                            com.actest.client.model.Exam exam = mapper.readValue(examJson,
                                    com.actest.client.model.Exam.class);
                            java.util.Map<Integer, String> answers = mapper.readValue(answersJson,
                                    new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<Integer, String>>() {
                                    });

                            javafx.application.Platform.runLater(() -> {
                                com.actest.client.controller.ExamTakingController.setPendingExam(exam);
                                com.actest.client.controller.ExamTakingController.setPendingAnswers(answers);
                                // Answers are managed by server in real-time mode
                                ViewManager.getInstance().switchView("/com/actest/client/view/exam_taking.fxml",
                                        "ACTEST Client - Exam");
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else if (message.startsWith("RESULT:")) {
                    double score = Double.parseDouble(message.substring(7));
                    // TODO: Save result to local DB if needed, or just rely on server
                    // For now, we can just show an alert or update UI
                    javafx.application.Platform.runLater(() -> {
                        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                                javafx.scene.control.Alert.AlertType.INFORMATION);
                        alert.setTitle("Exam Result");
                        alert.setHeaderText(null);
                        alert.setContentText("Your score: " + score);
                        alert.showAndWait();
                    });
                } else if (message.equals("STOP_EXAM")) {
                    javafx.application.Platform.runLater(() -> examContainer.getChildren().clear());
                } else if (message.equals("REMOVED")) {
                    javafx.application.Platform.runLater(() -> handleDisconnect());
                }
            });
        }

    }

    @FXML
    private void handleDisconnect() {
        authService.disconnect();
        ViewManager.getInstance().switchView("/com/actest/client/view/login.fxml", "ACTEST Client - Login");
    }

    private void addExamCard(com.actest.client.model.Exam exam) {
        javafx.scene.layout.HBox card = new javafx.scene.layout.HBox(20);
        card.setStyle(
                "-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 5; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 0);");
        card.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        javafx.scene.layout.VBox infoBox = new javafx.scene.layout.VBox(5);
        javafx.scene.control.Label titleLabel = new javafx.scene.control.Label(exam.getName());
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        javafx.scene.control.Label durationLabel = new javafx.scene.control.Label(
                "Duration: " + exam.getDuration() + " mins");
        infoBox.getChildren().addAll(titleLabel, durationLabel);

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        javafx.scene.control.Button joinBtn = new javafx.scene.control.Button("Join Exam");
        joinBtn.getStyleClass().add("button");
        joinBtn.setOnAction(e -> {
            com.actest.client.controller.ExamTakingController.setPendingExam(exam);
            ViewManager.getInstance().switchView("/com/actest/client/view/exam_taking.fxml", "ACTEST Client - Exam");
        });

        // Removed View Results button as per requirement
        card.getChildren().addAll(infoBox, spacer, joinBtn);
        examContainer.getChildren().add(card);
    }
}

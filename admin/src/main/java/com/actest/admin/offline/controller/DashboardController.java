package com.actest.admin.offline.controller;

import com.actest.admin.model.Exam;
import com.actest.admin.network.ClientHandler;
import com.actest.admin.service.ExamService;
import com.actest.admin.util.ViewManager;
import com.actest.admin.controller.ExamResultsController;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class DashboardController {

    @FXML
    private VBox examsView;
    @FXML
    private VBox clientsView;

    @FXML
    private VBox examCardsContainer;
    @FXML
    private FlowPane pendingStudentsContainer;
    @FXML
    private FlowPane connectedStudentsContainer;

    private final ExamService examService = new ExamService();
    private static com.actest.admin.network.Server tcpServer;
    private static com.actest.admin.network.DiscoveryServer discoveryServer;
    public static String pendingServerName;

    private final Map<Integer, Timer> examTimers = new HashMap<>();
    private final Map<Integer, Integer> examTimeRemaining = new HashMap<>();
    private final Map<Integer, Label> examTimerLabels = new HashMap<>();

    private void updateTimerLabelText(int examId) {
        if (examTimerLabels.containsKey(examId) && examTimeRemaining.containsKey(examId)) {
            int seconds = examTimeRemaining.get(examId);
            int minutes = seconds / 60;
            int remSeconds = seconds % 60;
            examTimerLabels.get(examId).setText(String.format("%02d:%02d", minutes, remSeconds));
        }
    }

    @FXML
    public void initialize() {
        if (tcpServer == null && pendingServerName != null && !pendingServerName.isEmpty()) {
            startServers(pendingServerName);
        } else if (tcpServer != null) {
            // Re-attach listener if needed, or just refresh
            // Since listener is attached to instance, and instance is static, it should
            // persist.
            // But the listener calls 'refreshClients' which is instance method of THIS
            // controller.
            // So we MUST update the listener to point to THIS controller instance.
            updateServerListener();
        }
        refreshLists();
    }

    private void updateServerListener() {
        if (tcpServer != null) {
            tcpServer.setListener(new com.actest.admin.network.Server.ClientListener() {
                @Override
                public void onClientConnected(ClientHandler client) {
                    Platform.runLater(() -> {
                        refreshClients();
                        refreshExams(); // Update student count
                    });
                }

                @Override
                public void onClientDisconnected(ClientHandler client) {
                    Platform.runLater(() -> {
                        if (client.getName() != null && clientStates.containsKey(client.getName())) {
                            clientStates.get(client.getName()).isDisconnected = true;
                        }
                        refreshClients();
                        refreshExams(); // Update student count
                    });
                }

                @Override
                public void onMessageReceived(ClientHandler client, String message) {
                    handleMessage(client, message);
                }
            });
        }
    }

    private void startServers(String serverName) {
        tcpServer = new com.actest.admin.network.Server();
        updateServerListener();
        tcpServer.start();

        discoveryServer = new com.actest.admin.network.DiscoveryServer(serverName);
        discoveryServer.start();
        System.out.println("Servers started for: " + serverName);
    }

    private void handleMessage(ClientHandler client, String message) {
        if (message.startsWith("JOIN:")) {
            String name = message.substring(5);
            client.setName(name);
            Platform.runLater(() -> refreshClients());
        } else if (message.startsWith("SUBMIT_EXAM:")) {
            // Format: SUBMIT_EXAM:examId:JSON_ANSWERS
            String[] parts = message.split(":", 3);
            if (parts.length == 3) {
                int examId = Integer.parseInt(parts[1]);
                String resultJson = parts[2];
                System.out.println("Received result from " + client.getName() + " for exam " + examId);
                handleExamSubmission(client, examId, resultJson);
            }
        }
    }

    private void handleExamSubmission(ClientHandler client, int examId, String resultJson) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            java.util.Map<Integer, String> answers = mapper.readValue(resultJson,
                    new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<Integer, String>>() {
                    });

            // Find the exam to check allowReview
            Exam exam = null;
            if (localExams != null) {
                for (Exam e : localExams) {
                    if (e.getId() == examId) {
                        exam = e;
                        break;
                    }
                }
            }
            // If not found in local cache (rare), try fetching (optional, but cache should
            // have it)

            List<com.actest.admin.model.Question> questions = examService.getQuestionsByExamId(examId);
            int correctCount = 0;
            int wrongCount = 0;

            for (com.actest.admin.model.Question q : questions) {
                if (answers.containsKey(q.getId())) {
                    String clientAnswer = answers.get(q.getId());
                    String correctAnswer = "";
                    if (q.getOptions() != null && q.getCorrectOptionIndex() >= 0
                            && q.getCorrectOptionIndex() < q.getOptions().size()) {
                        correctAnswer = q.getOptions().get(q.getCorrectOptionIndex());
                    }

                    if (correctAnswer.equals(clientAnswer)) {
                        correctCount++;
                    } else {
                        wrongCount++;
                    }
                } else {
                    wrongCount++; // Unanswered counts as wrong
                }
            }

            double score = questions.isEmpty() ? 0 : (double) correctCount / questions.size() * 10.0;
            examService.saveResult(client.getName(), examId, score);

            boolean allowReview = (exam != null) && exam.isAllowReview();

            // Send result back to client
            // Format: RESULT:score:correctCount:wrongCount:allowReview
            String resultMsg = String.format("RESULT:%.2f:%d:%d:%b", score, correctCount, wrongCount, allowReview);
            client.sendMessage(resultMsg);
            System.out.println("Sent result to " + client.getName() + ": " + resultMsg);

        } catch (Exception e) {
            e.printStackTrace();
            client.sendMessage("ERROR:Submission processing failed");
        }
    }

    private void refreshLists() {
        refreshExams();
        refreshClients();
    }

    private List<Exam> localExams;

    private void refreshExams() {
        examCardsContainer.getChildren().clear();
        localExams = examService.getLocalExams();
        handleSearch();
    }

    @FXML
    private javafx.scene.control.TextField searchField;

    @FXML
    private void handleSearch() {
        if (localExams == null)
            return;
        String query = searchField.getText() != null ? searchField.getText().toLowerCase() : "";
        examCardsContainer.getChildren().clear();
        for (Exam exam : localExams) {
            if (exam.getName().toLowerCase().contains(query)) {
                examCardsContainer.getChildren().add(createExamCard(exam));
            }
        }
    }

    private HBox createExamCard(Exam exam) {
        HBox card = new HBox(20);
        card.setStyle(
                "-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 5; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 0);");
        card.setAlignment(Pos.CENTER_LEFT);

        VBox infoBox = new VBox(5);
        Label titleLabel = new Label(exam.getName());
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        Label statusLabel = new Label("Status: " + (exam.getStatus() == null ? "Not Started" : exam.getStatus()));
        statusLabel.setStyle("-fx-text-fill: grey;");

        Label countLabel = new Label();
        if ("IN_PROGRESS".equals(exam.getStatus())) {
            int connectedCount = tcpServer != null ? tcpServer.getAcceptedClients().size() : 0;
            countLabel.setText("Students: " + connectedCount);
            countLabel.setStyle("-fx-text-fill: #0077cc; -fx-font-weight: bold;");
        }

        infoBox.getChildren().addAll(titleLabel, statusLabel, countLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox buttonsBox = new HBox(10);
        buttonsBox.setAlignment(Pos.CENTER_RIGHT);

        String status = exam.getStatus();
        if (status == null || "NOT_STARTED".equals(status)) {
            Button startBtn = new Button("Start");
            startBtn.getStyleClass().add("button");
            startBtn.setOnAction(e -> startExam(exam));

            Button editBtn = new Button("Edit");
            editBtn.getStyleClass().add("button-secondary");
            editBtn.setOnAction(e -> editExam(exam));

            Button deleteBtn = new Button("Delete");
            deleteBtn.getStyleClass().add("button-danger");
            deleteBtn.setOnAction(e -> deleteExam(exam));

            buttonsBox.getChildren().addAll(startBtn, editBtn, deleteBtn);
        } else if ("IN_PROGRESS".equals(status)) {
            Label timerLabel = new Label();
            timerLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0077cc;");
            examTimerLabels.put(exam.getId(), timerLabel);
            updateTimerLabel(exam, timerLabel);

            Button addTimeBtn = new Button("+5 Min");
            addTimeBtn.getStyleClass().add("button-secondary");
            addTimeBtn.setOnAction(e -> addTime(exam, 5));

            Button endBtn = new Button("End");
            endBtn.getStyleClass().add("button-danger");
            endBtn.setOnAction(e -> endExam(exam));

            buttonsBox.getChildren().addAll(timerLabel, addTimeBtn, endBtn);
        } else if ("FINISHED".equals(status)) {
            Button resultsBtn = new Button("View Results");
            resultsBtn.getStyleClass().add("button-secondary");
            resultsBtn.setOnAction(e -> viewResults(exam));

            Button deleteBtn = new Button("Delete");
            deleteBtn.getStyleClass().add("button-danger");
            deleteBtn.setOnAction(e -> deleteExam(exam));

            buttonsBox.getChildren().addAll(resultsBtn, deleteBtn);
        }

        card.getChildren().addAll(infoBox, spacer, buttonsBox);
        return card;
    }

    private void viewResults(Exam exam) {
        Object controller = ViewManager.getInstance().switchView("/com/actest/admin/view/exam_results.fxml",
                "ACTEST Admin - Exam Results");
        if (controller instanceof com.actest.admin.controller.ExamResultsController) {
            ((com.actest.admin.controller.ExamResultsController) controller).setExam(exam);
        }
    }

    private void updateTimerLabel(Exam exam, Label label) {
        if (examTimeRemaining.containsKey(exam.getId())) {
            int seconds = examTimeRemaining.get(exam.getId());
            int minutes = seconds / 60;
            int remSeconds = seconds % 60;
            label.setText(String.format("%02d:%02d", minutes, remSeconds));
        } else {
            label.setText("--:--");
        }
    }

    private com.actest.admin.network.TimeBroadcastServer timeBroadcastServer;

    private void startExam(Exam exam) {
        System.out.println("Attempting to start exam: " + exam.getName());
        try {
            if (tcpServer != null) {
                System.out.println("TCP Server is running. Broadcasting start.");

                // Load questions if not present
                if (exam.getQuestions() == null || exam.getQuestions().isEmpty()) {
                    System.out.println("Loading questions for exam...");
                    exam.setQuestions(examService.getQuestionsByExamId(exam.getId()));
                }

                // Start UDP Time Broadcast
                if (timeBroadcastServer == null) {
                    timeBroadcastServer = new com.actest.admin.network.TimeBroadcastServer();
                }
                int durationSeconds = exam.getDuration() * 60;
                timeBroadcastServer.startBroadcast(durationSeconds);

                examService.updateStatus(exam.getId(), "IN_PROGRESS");
                exam.setStatus("IN_PROGRESS");

                // Start Timer (Local Admin View)
                examTimeRemaining.put(exam.getId(), durationSeconds);
                Timer timer = new Timer();
                timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        int remaining = examTimeRemaining.get(exam.getId());
                        if (remaining > 0) {
                            examTimeRemaining.put(exam.getId(), remaining - 1);
                            Platform.runLater(() -> updateTimerLabelText(exam.getId()));
                        } else {
                            timer.cancel();
                            Platform.runLater(() -> endExam(exam));
                        }
                    }
                }, 1000, 1000);
                examTimers.put(exam.getId(), timer);

                // Initialize Client States
                for (ClientHandler client : tcpServer.getAcceptedClients()) {
                    if (client.getName() != null) {
                        clientStates.put(client.getName(),
                                new ClientState(client.getName(), exam.getId(), durationSeconds));
                        client.setCurrentExam(exam);
                        client.startExam(); // Send START_EXAM to client
                        // client.sendNextQuestion(); // Wait for READY signal
                    }
                }

                Platform.runLater(() -> refreshExams());
                System.out.println("Exam started successfully.");
            } else {
                System.err.println("TCP Server is NULL. Cannot start exam.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void endExam(Exam exam) {
        if (tcpServer != null) {
            tcpServer.broadcast("FORCE_SUBMIT");
            tcpServer.broadcast("STOP_EXAM"); // Keep for backward compatibility or other logic
        }
        examService.updateStatus(exam.getId(), "FINISHED");
        exam.setStatus("FINISHED");
        if (examTimers.containsKey(exam.getId())) {
            examTimers.get(exam.getId()).cancel();
            examTimers.remove(exam.getId());
        }
        refreshExams();
    }

    private void addTime(Exam exam, int minutes) {
        if (examTimeRemaining.containsKey(exam.getId())) {
            int addedSeconds = minutes * 60;
            examTimeRemaining.put(exam.getId(), examTimeRemaining.get(exam.getId()) + addedSeconds);

            if (timeBroadcastServer != null) {
                timeBroadcastServer.addTime(addedSeconds);
            }
        }
    }

    private void refreshClients() {
        if (tcpServer == null)
            return;

        pendingStudentsContainer.getChildren().clear();
        connectedStudentsContainer.getChildren().clear();

        for (ClientHandler client : tcpServer.getPendingClients()) {
            pendingStudentsContainer.getChildren().add(createStudentCard(client, true));
        }

        for (ClientHandler client : tcpServer.getAcceptedClients()) {
            connectedStudentsContainer.getChildren().add(createStudentCard(client, false));
        }
    }

    // Client State Tracking
    private static class ClientState {
        String name;
        int examId;
        long startTime; // System.currentTimeMillis()
        int durationSeconds;
        boolean isDisconnected;

        public ClientState(String name, int examId, int durationSeconds) {
            this.name = name;
            this.examId = examId;
            this.durationSeconds = durationSeconds;
            this.startTime = System.currentTimeMillis();
            this.isDisconnected = false;
        }
    }

    private final Map<String, ClientState> clientStates = new HashMap<>();

    private VBox createStudentCard(ClientHandler client, boolean isPending) {
        VBox card = new VBox(10);
        card.setStyle(
                "-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 5; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 0); -fx-min-width: 150; -fx-alignment: center;");

        Label nameLabel = new Label(client.getName() != null ? client.getName() : "Unknown");
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #2d3748;");

        Label ipLabel = new Label("IP: " + client.getIpAddress());
        ipLabel.setStyle("-fx-text-fill: #718096; -fx-font-size: 12px;");

        HBox buttonsBox = new HBox(5);
        buttonsBox.setAlignment(Pos.CENTER);

        if (isPending) {
            Button acceptBtn = new Button("Accept");
            acceptBtn.getStyleClass().add("button");
            acceptBtn.setStyle("-fx-font-size: 10px; -fx-padding: 5 10;");
            acceptBtn.setOnAction(e -> {
                client.accept();
                // Check for resume
                if (client.getName() != null && clientStates.containsKey(client.getName())) {
                    ClientState state = clientStates.get(client.getName());
                    if (state.isDisconnected) {
                        state.isDisconnected = false;
                        // Offer resume
                        Platform.runLater(() -> {
                            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                                    javafx.scene.control.Alert.AlertType.CONFIRMATION);
                            alert.setTitle("Resume Exam");
                            alert.setHeaderText("Client " + client.getName() + " was in an exam.");
                            alert.setContentText("Do you want to resume their exam?");

                            alert.showAndWait().ifPresent(response -> {
                                if (response == javafx.scene.control.ButtonType.OK) {
                                    resumeExam(client, state);
                                } else {
                                    clientStates.remove(client.getName()); // Clear state if not resuming
                                }
                            });
                        });
                    }
                }
                refreshClients();
            });

            Button rejectBtn = new Button("Reject");
            rejectBtn.getStyleClass().add("button-danger");
            rejectBtn.setStyle("-fx-font-size: 10px; -fx-padding: 5 10;");
            rejectBtn.setOnAction(e -> {
                client.reject();
                refreshClients();
            });

            buttonsBox.getChildren().addAll(acceptBtn, rejectBtn);
        } else {
            Button removeBtn = new Button("Remove");
            removeBtn.getStyleClass().add("button-secondary");
            removeBtn.setStyle("-fx-font-size: 10px; -fx-padding: 5 10;");
            removeBtn.setOnAction(e -> {
                client.sendMessage("REMOVED");
                client.close(); // Or specific remove logic
                refreshClients();
            });
            buttonsBox.getChildren().add(removeBtn);

            // Show status if in exam
            if (client.getName() != null && clientStates.containsKey(client.getName())) {
                ClientState state = clientStates.get(client.getName());
                long elapsed = (System.currentTimeMillis() - state.startTime) / 1000;
                long remaining = state.durationSeconds - elapsed;
                Label statusLabel = new Label(remaining > 0 ? "Time: " + remaining / 60 + "m" : "Finished");
                statusLabel.setStyle("-fx-text-fill: green; -fx-font-size: 10px;");
                card.getChildren().add(statusLabel);
            }
        }

        card.getChildren().addAll(nameLabel, ipLabel, buttonsBox);
        return card;
    }

    private void resumeExam(ClientHandler client, ClientState state) {
        try {
            // Find the exam
            Exam exam = null;
            for (Exam e : localExams) {
                if (e.getId() == state.examId) {
                    exam = e;
                    break;
                }
            }

            if (exam != null) {
                if (exam.getQuestions() == null || exam.getQuestions().isEmpty()) {
                    exam.setQuestions(examService.getQuestionsByExamId(exam.getId()));
                }

                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                String examJson = mapper.writeValueAsString(exam);

                // Calculate remaining time
                long elapsed = (System.currentTimeMillis() - state.startTime) / 1000;
                long remaining = state.durationSeconds - elapsed;
                if (remaining < 0)
                    remaining = 0;

                // We need to send remaining time to client.
                // The current START_EXAM sends the whole exam object which has 'duration'.
                // We should update the duration in the sent object to be the remaining time.
                // Or send a separate RESUME packet.
                // Let's modify the exam object copy to have new duration.
                // Actually, modifying the exam object might be tricky if it affects other
                // things.
                // Let's send RESUME_EXAM:examJson:answersJson (answers empty for now)

                // Hack: Modify duration in JSON or Object
                // Better: Send START_EXAM but with modified duration.
                // But client handles START_EXAM by just showing it.
                // Let's use RESUME_EXAM as planned in Client DashboardController.

                // Construct RESUME_EXAM message
                // Format: RESUME_EXAM:examJson|||answersJson
                // We don't have answers here unless we tracked them. For now send empty map.
                String answersJson = "{}";

                // We should update the duration in the examJson to reflect remaining time?
                // Or client calculates? Client usually takes duration from exam object.
                // So let's create a temporary exam object with updated duration.
                Exam tempExam = new Exam(exam.getId(), exam.getName(), (int) (remaining / 60), exam.getCreatorId());
                tempExam.setQuestions(exam.getQuestions());
                String tempExamJson = mapper.writeValueAsString(tempExam);

                client.sendMessage("RESUME_EXAM:" + tempExamJson + "|||" + answersJson);
                System.out.println("Resumed exam for " + client.getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void showExams() {
        examsView.setVisible(true);
        clientsView.setVisible(false);
        refreshExams();
    }

    @FXML
    private void showClients() {
        examsView.setVisible(false);
        clientsView.setVisible(true);
        refreshClients();
    }

    @FXML
    private void handleStopServer() {
        if (tcpServer != null) {
            tcpServer.stop();
            tcpServer = null;
        }
        if (discoveryServer != null) {
            discoveryServer.stop();
            discoveryServer = null;
        }
        pendingServerName = null;
        ViewManager.getInstance().switchView("/com/actest/admin/view/login.fxml", "ACTEST Admin - Login");
    }

    @FXML
    private void handleCreateExam() {
        Object controller = ViewManager.getInstance().switchView("/com/actest/admin/view/exam_detail.fxml",
                "ACTEST Admin - Create Exam");
        if (controller instanceof com.actest.admin.controller.ExamDetailController) {
            ((com.actest.admin.controller.ExamDetailController) controller)
                    .setPreviousView("/com/actest/admin/view/dashboard_offline.fxml", "Admin Dashboard - Offline");
        }
    }

    private void editExam(Exam exam) {
        Object controller = ViewManager.getInstance().switchView("/com/actest/admin/view/exam_detail.fxml",
                "ACTEST Admin - Edit Exam");
        if (controller instanceof com.actest.admin.controller.ExamDetailController) {
            com.actest.admin.controller.ExamDetailController detailController = (com.actest.admin.controller.ExamDetailController) controller;
            detailController.setPreviousView("/com/actest/admin/view/dashboard_offline.fxml",
                    "Admin Dashboard - Offline");
            detailController.setExam(exam);
        }
    }

    private void deleteExam(Exam exam) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Exam");
        alert.setHeaderText("Are you sure you want to delete this exam?");
        alert.setContentText("This will delete the exam and all its questions permanently.");

        java.util.Optional<javafx.scene.control.ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == javafx.scene.control.ButtonType.OK) {
            examService.deleteExam(exam.getId());
            refreshExams();
        }
    }
}

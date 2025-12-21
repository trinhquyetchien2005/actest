package com.actest.client.controller;

import com.actest.client.network.TimeListener;
import com.actest.client.util.ViewManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ExamTakingController {

    @FXML
    private Label examNameLabel;

    @FXML
    private Label timerLabel;

    @FXML
    private VBox questionArea; // Renamed from questionsContainer
    @FXML
    private javafx.scene.layout.FlowPane questionGrid; // New Grid

    private com.actest.client.model.Exam currentExam;
    private TimeListener timeListener;
    private Map<Integer, String> answers = new HashMap<>();
    private Map<Integer, Boolean> markedQuestions = new HashMap<>(); // Track marked questions
    private int currentQuestionIndex = 0;
    private ToggleGroup currentGroup;
    private List<Button> gridButtons = new ArrayList<>();

    private static com.actest.client.model.Exam pendingExam;
    private static Map<Integer, String> pendingAnswers;

    public static void setPendingExam(com.actest.client.model.Exam exam) {
        pendingExam = exam;
    }

    public static void setPendingAnswers(Map<Integer, String> answers) {
        pendingAnswers = answers;
    }

    @FXML
    public void initialize() {
        // Enforce Security
        Platform.runLater(() -> {
            javafx.stage.Stage stage = (javafx.stage.Stage) examNameLabel.getScene().getWindow();
            stage.setFullScreen(true);
            stage.setFullScreenExitKeyCombination(javafx.scene.input.KeyCombination.NO_MATCH); // Disable ESC
            // stage.setAlwaysOnTop(true); // Optional, can be annoying during dev

            setupFocusListener();
        });

        if (pendingExam != null) {
            this.currentExam = pendingExam;
            examNameLabel.setText(pendingExam.getName());
            pendingExam = null;

            if (pendingAnswers != null) {
                this.answers.putAll(pendingAnswers);
                pendingAnswers = null;
            }

            startExam(); // Start immediately if we have the exam
        }

        // Setup Network Listener
        setupNetworkListener();

        // Send READY signal
        com.actest.client.network.Client client = com.actest.client.service.AuthService.getTcpClient();
        if (client != null) {
            client.sendMessage("READY");
        }
    }

    private void setupNetworkListener() {
        com.actest.client.network.Client client = com.actest.client.service.AuthService.getTcpClient();
        if (client != null) {
            client.setListener(message -> {
                if (message.startsWith("START_EXAM:")) {
                    String examJson = message.substring(11);
                    try {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        currentExam = mapper.readValue(examJson, com.actest.client.model.Exam.class);
                        Platform.runLater(this::startExam);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if (message.startsWith("RESUME_EXAM:")) {
                    // Format: RESUME_EXAM:examJson|||answersJson
                    String payload = message.substring(12);
                    String[] parts = payload.split("\\|\\|\\|");
                    if (parts.length == 2) {
                        String examJson = parts[0];
                        String answersJson = parts[1];
                        try {
                            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                            currentExam = mapper.readValue(examJson, com.actest.client.model.Exam.class);

                            java.util.Map<Integer, String> restoredAnswers = mapper.readValue(answersJson,
                                    new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<Integer, String>>() {
                                    });

                            Platform.runLater(() -> {
                                startExam();
                                // Restore answers
                                answers.putAll(restoredAnswers);
                                updateGridStyles();
                                // Refresh current question view if needed
                                if (currentQuestion != null) {
                                    displayQuestion(currentQuestionIndex);
                                }
                            });
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else if (message.equals("EXAM_FINISHED") || message.equals("FORCE_SUBMIT")) {
                    Platform.runLater(this::handleSubmit);
                } else if (message.startsWith("RESULT:")) {
                    // Format: RESULT:score:correct:wrong:allowReview
                    String[] parts = message.split(":");
                    if (parts.length >= 5) {
                        double score = Double.parseDouble(parts[1]);
                        int correct = Integer.parseInt(parts[2]);
                        int wrong = Integer.parseInt(parts[3]);
                        boolean allowReview = Boolean.parseBoolean(parts[4]);
                        Platform.runLater(() -> showResult(score, correct, wrong, allowReview));
                    } else if (parts.length == 2) {
                        // Legacy format: RESULT:score
                        double score = Double.parseDouble(parts[1]);
                        Platform.runLater(() -> showResult(score, 0, 0, false));
                    }
                } else if (message.startsWith("ERROR:")) {
                    String errorMsg = message.substring(6);
                    Platform.runLater(() -> {
                        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                                javafx.scene.control.Alert.AlertType.ERROR);
                        alert.setTitle("Error");
                        alert.setHeaderText("Submission Error");
                        alert.setContentText(errorMsg);
                        alert.showAndWait();
                    });
                }
            });
        }
    }

    private void startExam() {
        examNameLabel.setText(currentExam.getName());

        // Start Timer
        timeListener = new TimeListener(timerLabel, currentExam.getId());
        new Thread(timeListener).start();

        // Render Grid
        renderGrid();

        // Show first question
        currentQuestionIndex = 0;
        displayQuestion(currentQuestionIndex);
    }

    private void renderGrid() {
        questionGrid.getChildren().clear();
        gridButtons.clear();
        if (currentExam != null && currentExam.getQuestions() != null) {
            for (int i = 0; i < currentExam.getQuestions().size(); i++) {
                final int index = i;
                Button btn = new Button(String.valueOf(i + 1));
                btn.setPrefSize(50, 50);
                btn.setMinSize(50, 50); // Ensure it doesn't shrink
                btn.setStyle(
                        "-fx-background-color: #edf2f7; -fx-text-fill: #2d3748; -fx-border-color: #cbd5e0; -fx-border-radius: 5; -fx-background-radius: 5; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 0;");
                btn.setOnAction(e -> displayQuestion(index));
                questionGrid.getChildren().add(btn);
                gridButtons.add(btn);
            }
        }
    }

    private com.actest.client.model.Question currentQuestion;

    private void displayQuestion(int index) {
        if (currentExam == null || currentExam.getQuestions() == null || index < 0
                || index >= currentExam.getQuestions().size()) {
            questionArea.getChildren().clear();
            questionArea.getChildren().add(new Label("No question to display or invalid index."));
            return;
        }

        currentQuestionIndex = index;
        com.actest.client.model.Question q = currentExam.getQuestions().get(index);
        this.currentQuestion = q;

        questionArea.getChildren().clear();

        // Update Grid Styles
        updateGridStyles();

        VBox card = new VBox(15);
        card.setStyle(
                "-fx-background-color: white; -fx-padding: 20; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 0);");

        // Question Header with Mark Checkbox
        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        Label qLabel = new Label("Question " + (index + 1) + ":");
        qLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 18px; -fx-text-fill: #2d3748;");

        javafx.scene.control.CheckBox markCb = new javafx.scene.control.CheckBox("Mark for Review");
        markCb.setSelected(markedQuestions.getOrDefault(q.getId(), false));
        markCb.setOnAction(e -> {
            markedQuestions.put(q.getId(), markCb.isSelected());
            updateGridStyles();
        });

        javafx.scene.layout.Region spacer = new javafx.scene.layout.Region();
        javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        header.getChildren().addAll(qLabel, spacer, markCb);

        Label contentLabel = new Label(q.getContent());
        contentLabel.setWrapText(true);
        contentLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #4a5568;");

        currentGroup = new ToggleGroup();
        VBox optionsBox = new VBox(10);

        if (q.getOptions() != null) {
            List<String> options = q.getOptions();
            for (String option : options) {
                // Use ToggleButton behavior but styled as a card
                // Or use HBox with RadioButton inside, and handle click on HBox

                HBox optionCard = new HBox(10);
                optionCard.getStyleClass().add("answer-option");
                optionCard.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

                RadioButton rb = new RadioButton(option);
                rb.setToggleGroup(currentGroup);
                rb.setUserData(option);
                rb.setMouseTransparent(true); // Let HBox handle click

                // Check if selected
                boolean isSelected = answers.containsKey(q.getId()) && answers.get(q.getId()).equals(option);
                if (isSelected) {
                    rb.setSelected(true);
                    optionCard.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("selected"), true);
                }

                optionCard.setOnMouseClicked(e -> {
                    rb.setSelected(true);
                    answers.put(q.getId(), option);
                    sendAnswer(q.getId(), option);
                    updateGridStyles();

                    // Update visual state of all options
                    for (javafx.scene.Node node : optionsBox.getChildren()) {
                        if (node instanceof HBox) {
                            node.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("selected"),
                                    node == optionCard);
                            // Also update the radio button inside? The ToggleGroup handles the logic, but
                            // visual sync might need help if we blocked mouse
                            // Actually, since we set rb.setSelected(true), ToggleGroup updates other RBs.
                        }
                    }
                });

                optionCard.getChildren().add(rb);
                optionsBox.getChildren().add(optionCard);
            }
        }

        HBox navBox = new HBox(10);
        navBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        Button prevBtn = new Button("Previous");
        prevBtn.setDisable(index == 0);
        prevBtn.setOnAction(e -> displayQuestion(index - 1));

        Button nextBtn = new Button("Next");
        nextBtn.getStyleClass().add("button-primary");
        nextBtn.setDisable(index == currentExam.getQuestions().size() - 1);
        nextBtn.setOnAction(e -> displayQuestion(index + 1));

        navBox.getChildren().addAll(prevBtn, nextBtn);
        card.getChildren().addAll(header, contentLabel, optionsBox, navBox);
        questionArea.getChildren().add(card);
    }

    private void updateGridStyles() {
        for (int i = 0; i < gridButtons.size(); i++) {
            Button btn = gridButtons.get(i);
            int qId = currentExam.getQuestions().get(i).getId();

            String style = "-fx-border-radius: 5; -fx-background-radius: 5; -fx-border-color: #cbd5e0; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 0; ";

            if (i == currentQuestionIndex) {
                style += "-fx-border-color: #3182ce; -fx-border-width: 2; ";
            }

            if (markedQuestions.getOrDefault(qId, false)) {
                style += "-fx-background-color: #f6e05e; -fx-text-fill: #744210;"; // Yellow for marked
            } else if (answers.containsKey(qId)) {
                style += "-fx-background-color: #48bb78; -fx-text-fill: white;"; // Green for answered
            } else {
                style += "-fx-background-color: #edf2f7; -fx-text-fill: #2d3748;"; // Grey for default
            }

            btn.setStyle(style);
        }
    }

    private void sendAnswer(int questionId, String answer) {
        com.actest.client.network.Client client = com.actest.client.service.AuthService.getTcpClient();
        if (client != null) {
            // Send UPDATE_ANSWER:examId:questionId:answer
            client.sendMessage("UPDATE_ANSWER:" + currentExam.getId() + ":" + questionId + ":" + answer);
        }
    }

    @FXML
    private void handleSubmit() {
        if (timeListener != null) {
            timeListener.stop();
        }

        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String answersJson = mapper.writeValueAsString(answers);

            com.actest.client.network.Client client = com.actest.client.service.AuthService.getTcpClient();
            if (client != null) {
                client.sendMessage("SUBMIT_EXAM:" + currentExam.getId() + ":" + answersJson);
            }

            // Exit Full Screen
            Platform.runLater(() -> {
                javafx.stage.Stage stage = (javafx.stage.Stage) examNameLabel.getScene().getWindow();
                stage.setFullScreen(false);
            });

            Label finishingLabel = new Label("Exam Submitted. Waiting for results...");
            finishingLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
            questionArea.getChildren().clear();
            questionArea.getChildren().add(finishingLabel);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showResult(double score, int correct, int wrong, boolean allowReview) {
        com.actest.client.controller.ExamResultController.setResult(score, correct, wrong, allowReview);
        com.actest.client.controller.ExamResultController.setReviewData(currentExam, answers);
        ViewManager.getInstance().switchView("/com/actest/client/view/exam_result.fxml",
                "ACTEST Client - Exam Result");
    }

    // handleExit removed

    private final java.util.concurrent.ScheduledExecutorService violationExecutor = java.util.concurrent.Executors
            .newSingleThreadScheduledExecutor();

    private void setupFocusListener() {
        javafx.stage.Stage stage = (javafx.stage.Stage) examNameLabel.getScene().getWindow();
        stage.focusedProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal && currentExam != null) {
                // Focus Lost
                System.out.println("Focus Lost! Scheduling screen capture in 2 seconds...");

                violationExecutor.schedule(() -> {
                    // Optional: Check if still lost focus?
                    // User request: "capture after 2 seconds".
                    // If they switch back quickly, maybe we still want to capture what they did?
                    // Or maybe we only capture if they are STILL away?
                    // Usually "after 2 seconds" implies a grace period or just a delay to catch the
                    // cheat app opening.
                    // Let's just capture.

                    System.out.println("Executing screen capture task...");
                    String base64Image = captureScreenToBase64();
                    if (base64Image != null) {
                        sendViolation("FOCUS_LOST", base64Image);
                    }
                }, 2, java.util.concurrent.TimeUnit.SECONDS);
            }
        });
    }

    private String captureScreenToBase64() {
        try {
            java.awt.Robot robot = new java.awt.Robot();
            java.awt.Rectangle screenRect = new java.awt.Rectangle(
                    java.awt.Toolkit.getDefaultToolkit().getScreenSize());
            java.awt.image.BufferedImage screenFullImage = robot.createScreenCapture(screenRect);

            // Convert to Base64
            java.io.ByteArrayOutputStream os = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(screenFullImage, "png", os);
            byte[] imageBytes = os.toByteArray();
            return java.util.Base64.getEncoder().encodeToString(imageBytes);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void sendViolation(String type, String data) {
        com.actest.client.network.Client client = com.actest.client.service.AuthService.getTcpClient();
        if (client != null) {
            // VIOLATION:type:examId:data
            client.sendMessage("VIOLATION:" + type + ":" + currentExam.getId() + ":" + data);
        }
    }

    @FXML
    private void handleMarkQuestion() {
    }
}

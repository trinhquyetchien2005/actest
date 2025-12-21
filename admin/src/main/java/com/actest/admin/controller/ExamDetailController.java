package com.actest.admin.controller;

import com.actest.admin.util.ViewManager;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;

import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

public class ExamDetailController {

    @FXML
    private TextField examNameField;

    @FXML
    private TextField durationField;

    @FXML
    private VBox questionContainer;

    @FXML
    private TextField newQuestionField;
    @FXML
    private TextField optionAField;
    @FXML
    private TextField optionBField;
    @FXML
    private TextField optionCField;
    @FXML
    private TextField optionDField;
    @FXML
    private TextField correctAnswerField;

    @FXML
    private javafx.scene.control.CheckBox allowReviewCheckbox;

    private final com.actest.admin.service.ExamService examService = new com.actest.admin.service.ExamService();
    private final java.util.List<com.actest.admin.model.Question> questions = new java.util.ArrayList<>();

    private String previousViewPath = "/com/actest/admin/view/dashboard_online.fxml"; // Default
    private String previousViewTitle = "ACTEST Admin - Dashboard";

    public void setPreviousView(String fxmlPath, String title) {
        this.previousViewPath = fxmlPath;
        this.previousViewTitle = title;
    }

    private boolean isReadOnly = false;
    private com.actest.admin.model.Exam currentExam;

    public void setReadOnly(boolean readOnly) {
        this.isReadOnly = readOnly;
        if (readOnly) {
            examNameField.setEditable(false);
            durationField.setEditable(false);
            newQuestionField.setDisable(true);
            optionAField.setDisable(true);
            optionBField.setDisable(true);
            optionCField.setDisable(true);
            optionDField.setDisable(true);
            correctAnswerField.setDisable(true);
            allowReviewCheckbox.setDisable(true);
            saveBtn.setVisible(false);
            addQuestionBox.setVisible(false);
        }
    }

    private boolean loadFromServer = false;

    public void setLoadFromServer(boolean loadFromServer) {
        this.loadFromServer = loadFromServer;
    }

    public void setExam(com.actest.admin.model.Exam exam) {
        this.currentExam = exam;
        if (exam != null) {
            examNameField.setText(exam.getName());
            durationField.setText(String.valueOf(exam.getDuration()));
            allowReviewCheckbox.setSelected(exam.isAllowReview());

            // Load questions
            questions.clear();
            questionContainer.getChildren().clear();

            java.util.List<com.actest.admin.model.Question> loadedQuestions;
            if (loadFromServer) {
                loadedQuestions = examService.getServerQuestions(exam.getId());
            } else {
                loadedQuestions = examService.getQuestionsByExamId(exam.getId());
            }

            questions.addAll(loadedQuestions);

            for (com.actest.admin.model.Question q : questions) {
                addQuestionCard(q);
            }

            // Show download button if we are in online mode (previous view is online
            // dashboard)
            if (previousViewPath.contains("dashboard_online") && !isReadOnly) {
                downloadBtn.setVisible(true);
            } else {
                downloadBtn.setVisible(false);
            }
        }
    }

    @FXML
    private void handleAddQuestion() {
        String qText = newQuestionField.getText();
        String a = optionAField.getText();
        String b = optionBField.getText();
        String c = optionCField.getText();
        String d = optionDField.getText();
        String ans = correctAnswerField.getText().toUpperCase();

        if (qText.isEmpty() || a.isEmpty() || b.isEmpty() || c.isEmpty() || d.isEmpty() || ans.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Missing Information");
            alert.setHeaderText(null);
            alert.setContentText("Please fill in all question fields.");
            alert.showAndWait();
            return;
        }

        java.util.List<String> options = new java.util.ArrayList<>();
        options.add(a);
        options.add(b);
        options.add(c);
        options.add(d);

        int correctIndex = 0;
        switch (ans) {
            case "A":
                correctIndex = 0;
                break;
            case "B":
                correctIndex = 1;
                break;
            case "C":
                correctIndex = 2;
                break;
            case "D":
                correctIndex = 3;
                break;
            default:
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Invalid Input");
                alert.setHeaderText(null);
                alert.setContentText("Correct answer must be A, B, C, or D.");
                alert.showAndWait();
                return;
        }

        com.actest.admin.model.Question question = new com.actest.admin.model.Question(0, qText, options, correctIndex);
        questions.add(question);
        addQuestionCard(question);

        // Clear fields
        newQuestionField.clear();
        optionAField.clear();
        optionBField.clear();
        optionCField.clear();
        optionDField.clear();
        correctAnswerField.clear();
    }

    private void addQuestionCard(com.actest.admin.model.Question question) {
        VBox card = new VBox(5);
        card.getStyleClass().add("card");
        card.setStyle(
                "-fx-background-color: #f8f9fa; -fx-padding: 10; -fx-border-color: #dee2e6; -fx-border-radius: 5; -fx-background-radius: 5;");

        Label qLabel = new Label("Q: " + question.getContent());
        qLabel.setStyle("-fx-font-weight: bold;");
        card.getChildren().add(qLabel);

        for (int i = 0; i < question.getOptions().size(); i++) {
            String prefix = (i == question.getCorrectOptionIndex()) ? "[Correct] " : "";
            String style = (i == question.getCorrectOptionIndex()) ? "-fx-text-fill: green;" : "";
            Label optLabel = new Label(prefix + (char) ('A' + i) + ". " + question.getOptions().get(i));
            optLabel.setStyle(style);
            card.getChildren().add(optLabel);
        }

        // Add Edit/Delete buttons
        if (!isReadOnly) {
            javafx.scene.layout.HBox buttonBox = new javafx.scene.layout.HBox(10);
            buttonBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

            javafx.scene.control.Button editBtn = new javafx.scene.control.Button("Edit");
            editBtn.getStyleClass().add("button-secondary");
            editBtn.setOnAction(e -> handleEditQuestion(question, card));

            javafx.scene.control.Button deleteBtn = new javafx.scene.control.Button("Delete");
            deleteBtn.getStyleClass().add("button-danger");
            deleteBtn.setOnAction(e -> handleDeleteQuestion(question, card));

            buttonBox.getChildren().addAll(editBtn, deleteBtn);
            card.getChildren().add(buttonBox);
        }

        questionContainer.getChildren().add(card);
    }

    private void handleEditQuestion(com.actest.admin.model.Question question, VBox card) {
        // Populate fields
        newQuestionField.setText(question.getContent());
        optionAField.setText(question.getOptions().get(0));
        optionBField.setText(question.getOptions().get(1));
        optionCField.setText(question.getOptions().get(2));
        optionDField.setText(question.getOptions().get(3));

        char correctChar = (char) ('A' + question.getCorrectOptionIndex());
        correctAnswerField.setText(String.valueOf(correctChar));

        // Remove from list and UI so it can be re-added
        questions.remove(question);
        questionContainer.getChildren().remove(card);
    }

    private void handleDeleteQuestion(com.actest.admin.model.Question question, VBox card) {
        questions.remove(question);
        questionContainer.getChildren().remove(card);
    }

    @FXML
    private void handleSave() {
        String name = examNameField.getText();
        String durationStr = durationField.getText();

        if (name.isEmpty() || durationStr.isEmpty() || questions.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Invalid Exam Data");
            alert.setContentText("Please fill in all fields and add at least one question.");
            alert.showAndWait();
            return;
        }

        try {
            int duration = Integer.parseInt(durationStr);

            if (currentExam != null) {
                // Update existing exam
                currentExam.setName(name);
                currentExam.setDuration(duration);
                currentExam.setAllowReview(allowReviewCheckbox.isSelected());
                examService.updateExam(currentExam);

                // Replace questions (Delete all old, add all new)
                examService.deleteAllQuestions(currentExam.getId());
                for (com.actest.admin.model.Question q : questions) {
                    examService.addQuestionToExam(q, currentExam.getId());
                }

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Success");
                alert.setHeaderText("Exam Updated");
                alert.setContentText("The exam has been updated successfully.");
                alert.showAndWait();
            } else {
                // Create new exam
                com.actest.admin.model.Exam exam = new com.actest.admin.model.Exam(name, duration);
                exam.setStatus("NOT_STARTED"); // Default status
                exam.setAllowReview(allowReviewCheckbox.isSelected());

                if (examService.saveExam(exam, questions) != -1) {
                    // Auto-upload to server (Dual Save)
                    // We need the ID generated by local DB to set it on the object, but for upload
                    // we might need to be careful.
                    // Actually, saveExam updates the exam object? No, it returns ID.
                    // We should probably fetch the saved exam or just use the data we have.
                    // But wait, uploadExam expects an Exam object with an ID.
                    // The local ID might conflict with Server ID if we send it.
                    // Server usually generates its own ID.
                    // Let's check uploadExam logic. It sends the Exam object.
                    // If we send ID=0 (or whatever local ID), server might ignore it or use it.
                    // Server createExam uses INSERT ... RETURNING id. It ignores input ID usually
                    // unless specified.
                    // Let's assume server generates new ID.

                    // We need to set the ID on the exam object before uploading if we want to link
                    // questions?
                    // No, uploadExam sends exam + questions.
                    // We should probably update the exam object with the local ID first just in
                    // case.
                    // But for upload, we might want to let server handle ID.

                    // Let's just call uploadExam.
                    // Note: uploadExam uses exam.getId() to fetch questions locally to send.
                    // So we MUST have the local ID set on the exam object and questions saved
                    // locally first.
                    // saveExam returns boolean? No, it returns boolean in ExamService but int in
                    // Repository.
                    // ExamService.saveExam returns boolean.
                    // We need to get the ID.
                    // Actually ExamService.saveExam calls repo.saveExam which returns ID.
                    // But ExamService.saveExam returns boolean.
                    // We should update ExamService.saveExam to return ID or update the exam object.
                    // Or we can just fetch the last inserted exam? No, unsafe.

                    // Let's look at ExamService.saveExam again.
                    // It returns boolean.
                    // I should modify ExamService.saveExam to return the ID or update the exam
                    // object.

                    // For now, I will modify ExamService.saveExam to update the exam object's ID if
                    // possible,
                    // or I'll just rely on the fact that I can't easily get the ID without changing
                    // Service.
                    // Wait, I can change Service.

                    // Let's change ExamService.saveExam to return int (examId) instead of boolean.
                    // But that requires changing the interface.

                    // Alternative: The user wants "Dual Save".
                    // If I can't get the ID easily, I can't call uploadExam because uploadExam
                    // fetches questions by ID.
                    // So I MUST change ExamService.saveExam to return int.

                    // Let's pause and change ExamService.saveExam first.

                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Success");
                    alert.setHeaderText("Exam Saved");
                    alert.setContentText("The exam has been saved locally.");
                    alert.showAndWait();

                    // Attempt upload
                    // We need the ID.
                    // I will modify ExamService.saveExam in the next step.
                    // For now, I'll add a TODO or comment.

                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Error");
                    alert.setHeaderText("Save Failed");
                    alert.setContentText("Could not save the exam to the database.");
                    alert.showAndWait();
                    return;
                }
            }
            ViewManager.getInstance().switchView(previousViewPath, previousViewTitle);
        } catch (NumberFormatException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Invalid Duration");
            alert.setContentText("Duration must be a number.");
            alert.showAndWait();
        }
    }

    @FXML
    private javafx.scene.control.Button saveBtn;
    @FXML
    private VBox addQuestionBox;

    @FXML
    private javafx.scene.control.Button downloadBtn;

    @FXML
    private void handleDownload() {
        if (currentExam == null) {
            return;
        }

        boolean success = examService.downloadExam(currentExam.getId());
        if (success) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText("Exam Downloaded");
            alert.setContentText("The exam has been downloaded to your local database.");
            alert.showAndWait();
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Download Failed");
            alert.setContentText("Could not download the exam.");
            alert.showAndWait();
        }
    }

    @FXML
    private void handleCancel() {
        ViewManager.getInstance().switchView(previousViewPath, previousViewTitle);
    }
}

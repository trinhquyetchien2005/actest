package com.actest.client.controller;

import com.actest.client.util.ViewManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class ExamResultController {

    @FXML
    private Label scoreLabel;
    @FXML
    private Label correctLabel;
    @FXML
    private Label wrongLabel;
    @FXML
    private javafx.scene.control.Button reviewBtn;

    private static double score;
    private static int correctCount;
    private static int wrongCount;
    private static boolean allowReview;

    public static void setResult(double s, int correct, int wrong, boolean review) {
        score = s;
        correctCount = correct;
        wrongCount = wrong;
        allowReview = review;
    }

    private static com.actest.client.model.Exam exam;
    private static java.util.Map<Integer, String> userAnswers;

    public static void setReviewData(com.actest.client.model.Exam e, java.util.Map<Integer, String> answers) {
        exam = e;
        userAnswers = answers;
    }

    public static void setScore(double s) {
        score = s;
        // Default values for legacy calls
        correctCount = 0;
        wrongCount = 0;
        allowReview = false;
    }

    @FXML
    public void initialize() {
        scoreLabel.setText(String.format("Your Score: %.1f/10", score));
        correctLabel.setText("Correct: " + correctCount);
        wrongLabel.setText("Wrong: " + wrongCount);

        if (allowReview) {
            reviewBtn.setVisible(true);
            reviewBtn.setManaged(true);
        } else {
            reviewBtn.setVisible(false);
            reviewBtn.setManaged(false);
        }
    }

    @FXML
    private void handleReview() {
        if (exam != null && userAnswers != null) {
            com.actest.client.controller.ReviewAnswersController.setData(exam, userAnswers);
            ViewManager.getInstance().switchView("/com/actest/client/view/review_answers.fxml",
                    "ACTEST Client - Review Answers");
        } else {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.ERROR);
            alert.setTitle("Error");
            alert.setHeaderText("Review Data Missing");
            alert.setContentText("Could not load review data.");
            alert.showAndWait();
        }
    }

    @FXML
    private void handleBackToDashboard() {
        ViewManager.getInstance().switchView("/com/actest/client/view/dashboard.fxml", "ACTEST Client - Dashboard");
    }
}

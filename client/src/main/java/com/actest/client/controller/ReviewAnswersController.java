package com.actest.client.controller;

import com.actest.client.model.Exam;
import com.actest.client.model.Question;
import com.actest.client.util.ViewManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import java.util.Map;
import java.util.List;

public class ReviewAnswersController {

    @FXML
    private VBox questionsContainer;

    private static Exam exam;
    private static Map<Integer, String> userAnswers;

    public static void setData(Exam e, Map<Integer, String> answers) {
        exam = e;
        userAnswers = answers;
    }

    @FXML
    public void initialize() {
        if (exam != null && exam.getQuestions() != null) {
            int index = 1;
            for (Question q : exam.getQuestions()) {
                addQuestionCard(q, index++);
            }
        }
    }

    private void addQuestionCard(Question q, int index) {
        VBox card = new VBox(10);
        card.setStyle(
                "-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 5; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 0);");

        Label qLabel = new Label("Question " + index + ": " + q.getContent());
        qLabel.setWrapText(true);
        qLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        VBox optionsBox = new VBox(5);
        String userAnswer = userAnswers.getOrDefault(q.getId(), null);
        String correctAnswer = "";
        if (q.getOptions() != null && q.getCorrectOptionIndex() >= 0
                && q.getCorrectOptionIndex() < q.getOptions().size()) {
            correctAnswer = q.getOptions().get(q.getCorrectOptionIndex());
        }

        if (q.getOptions() != null) {
            for (String option : q.getOptions()) {
                Label optionLabel = new Label(option);
                optionLabel.setWrapText(true);
                optionLabel.setStyle("-fx-padding: 5; -fx-background-radius: 3;");

                boolean isSelected = option.equals(userAnswer);
                boolean isCorrect = option.equals(correctAnswer);

                if (isCorrect) {
                    optionLabel.setStyle(optionLabel.getStyle()
                            + "-fx-background-color: #c6f6d5; -fx-text-fill: #22543d; -fx-border-color: #48bb78; -fx-border-width: 1;");
                    if (isSelected) {
                        optionLabel.setText(option + " (Your Answer - Correct)");
                    } else {
                        optionLabel.setText(option + " (Correct Answer)");
                    }
                } else if (isSelected) {
                    optionLabel.setStyle(optionLabel.getStyle()
                            + "-fx-background-color: #fed7d7; -fx-text-fill: #822727; -fx-border-color: #f56565; -fx-border-width: 1;");
                    optionLabel.setText(option + " (Your Answer - Incorrect)");
                } else {
                    optionLabel.setStyle(optionLabel.getStyle() + "-fx-background-color: #edf2f7;");
                }

                optionsBox.getChildren().add(optionLabel);
            }
        }

        if (userAnswer == null) {
            Label noAnswerLabel = new Label("You did not answer this question.");
            noAnswerLabel.setStyle("-fx-text-fill: #e53e3e; -fx-font-style: italic;");
            optionsBox.getChildren().add(noAnswerLabel);
        }

        card.getChildren().addAll(qLabel, optionsBox);
        questionsContainer.getChildren().add(card);
    }

    @FXML
    private void handleBack() {
        ViewManager.getInstance().switchView("/com/actest/client/view/exam_result.fxml", "ACTEST Client - Exam Result");
    }
}

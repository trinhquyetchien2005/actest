package com.actest.admin.controller;

import com.actest.admin.model.Exam;
import com.actest.admin.model.Result;
import com.actest.admin.service.ExamService;
import com.actest.admin.util.ViewManager;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;
import java.io.File;
import java.io.FileOutputStream;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import javafx.stage.FileChooser;

public class ExamResultsController {

    @FXML
    private Label examNameLabel;

    @FXML
    private javafx.scene.layout.FlowPane resultsContainer;

    private final ExamService examService = new ExamService();
    private final com.actest.admin.service.StudentProgressService studentProgressService = new com.actest.admin.service.StudentProgressService();
    private Exam currentExam;
    private List<Result> currentResults; // Keep for export

    @FXML
    public void initialize() {
    }

    public void setExam(Exam exam) {
        this.currentExam = exam;
        examNameLabel.setText("Results for: " + exam.getName());
        loadResults();
    }

    private void loadResults() {
        currentResults = examService.getResultsByExamId(currentExam.getId());
        resultsContainer.getChildren().clear();

        if (currentResults.isEmpty()) {
            Label noResults = new Label("No results found.");
            noResults.setStyle("-fx-text-fill: #718096; -fx-font-size: 16px;");
            resultsContainer.getChildren().add(noResults);
            return;
        }

        for (Result result : currentResults) {
            resultsContainer.getChildren().add(createResultCard(result));
        }
    }

    private javafx.scene.layout.VBox createResultCard(Result result) {
        javafx.scene.layout.VBox card = new javafx.scene.layout.VBox(10);
        card.setPrefWidth(300);
        card.setStyle(
                "-fx-background-color: white; -fx-padding: 20; -fx-background-radius: 8; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 8, 0, 0, 0);");

        Label nameLabel = new Label(result.getStudentName());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 18px; -fx-text-fill: #2d3748;");

        Label scoreLabel = new Label(String.format("Score: %.2f", result.getScore()));
        scoreLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #4a5568;");

        // Fetch violation count from DB to be sure (or use result if it's populated
        // correctly)
        // Result object might not have the latest DB count if it was just saved from
        // memory.
        // Let's rely on StudentProgressService for violations.
        int violationCount = studentProgressService.getViolationCount(result.getStudentName(), currentExam.getId());

        Label violationLabel = new Label("Violations: " + violationCount);
        if (violationCount > 0) {
            violationLabel.setStyle("-fx-text-fill: #e53e3e; -fx-font-weight: bold;");
        } else {
            violationLabel.setStyle("-fx-text-fill: #38a169;");
        }

        card.getChildren().addAll(nameLabel, scoreLabel, violationLabel);

        if (violationCount > 0) {
            javafx.scene.control.Button viewViolationsBtn = new javafx.scene.control.Button("View Violations");
            viewViolationsBtn.getStyleClass().add("button-danger");
            viewViolationsBtn.setOnAction(e -> showViolations(result.getStudentName()));
            card.getChildren().add(viewViolationsBtn);
        }

        return card;
    }

    private void showViolations(String studentName) {
        List<com.actest.admin.service.StudentProgressService.Violation> violations = studentProgressService
                .getViolations(studentName, currentExam.getId());

        if (violations.isEmpty()) {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                    javafx.scene.control.Alert.AlertType.INFORMATION);
            alert.setTitle("Violations");
            alert.setHeaderText("No violations found for " + studentName);
            alert.showAndWait();
            return;
        }

        // Show in a new window/dialog
        javafx.scene.control.Dialog<Void> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Violations - " + studentName);
        dialog.getDialogPane().getButtonTypes().add(javafx.scene.control.ButtonType.CLOSE);

        javafx.scene.layout.VBox container = new javafx.scene.layout.VBox(20);
        container.setPadding(new javafx.geometry.Insets(20));

        javafx.scene.control.ScrollPane scrollPane = new javafx.scene.control.ScrollPane(container);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefSize(600, 500);

        for (com.actest.admin.service.StudentProgressService.Violation v : violations) {
            javafx.scene.layout.VBox vBox = new javafx.scene.layout.VBox(5);
            vBox.setStyle("-fx-border-color: #cbd5e0; -fx-border-width: 1; -fx-padding: 10; -fx-background-radius: 5;");

            Label typeLabel = new Label("Type: " + v.type);
            typeLabel.setStyle("-fx-font-weight: bold;");
            Label timeLabel = new Label("Time: " + v.violationTime);

            vBox.getChildren().addAll(typeLabel, timeLabel);

            if (v.imageData != null && !v.imageData.isEmpty()) {
                try {
                    byte[] imageBytes = java.util.Base64.getDecoder().decode(v.imageData);
                    javafx.scene.image.Image image = new javafx.scene.image.Image(
                            new java.io.ByteArrayInputStream(imageBytes));
                    javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView(image);
                    imageView.setPreserveRatio(true);
                    imageView.setFitWidth(550); // Fit within scrollpane
                    vBox.getChildren().add(imageView);
                } catch (Exception e) {
                    vBox.getChildren().add(new Label("Error loading image"));
                }
            }

            container.getChildren().add(vBox);
        }

        dialog.getDialogPane().setContent(scrollPane);
        dialog.showAndWait();
    }

    @FXML
    private void handleBack() {
        ViewManager.getInstance().switchView("/com/actest/admin/view/dashboard_offline.fxml",
                "Admin Dashboard - Offline");
    }

    @FXML
    private void handleExportExcel() {
        if (currentResults == null || currentResults.isEmpty())
            return;

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Excel File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        fileChooser.setInitialFileName("Results_" + currentExam.getName().replaceAll("\\s+", "_") + ".xlsx");
        File file = fileChooser.showSaveDialog(resultsContainer.getScene().getWindow());

        if (file != null) {
            try (Workbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("Results");

                // Header row
                Row headerRow = sheet.createRow(0);
                headerRow.createCell(0).setCellValue("Student Name");
                headerRow.createCell(1).setCellValue("Score");
                headerRow.createCell(2).setCellValue("Focus Lost");

                // Data rows
                for (int i = 0; i < currentResults.size(); i++) {
                    Result r = currentResults.get(i);
                    Row row = sheet.createRow(i + 1);
                    row.createCell(0).setCellValue(r.getStudentName());
                    row.createCell(1).setCellValue(r.getScore());
                    row.createCell(2).setCellValue(r.getFocusLostCount());
                }

                try (FileOutputStream fileOut = new FileOutputStream(file)) {
                    workbook.write(fileOut);
                }
                System.out.println("Excel file exported: " + file.getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

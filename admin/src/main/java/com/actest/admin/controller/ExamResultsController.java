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
    private TableView<Result> resultsTable;

    @FXML
    private TableColumn<Result, String> studentNameCol;

    @FXML
    private TableColumn<Result, Double> scoreCol;

    @FXML
    private TableColumn<Result, Integer> focusLostCol;

    private final ExamService examService = new ExamService();
    private Exam currentExam;

    @FXML
    public void initialize() {
        studentNameCol.setCellValueFactory(new PropertyValueFactory<>("studentName"));
        scoreCol.setCellValueFactory(new PropertyValueFactory<>("score"));
        focusLostCol.setCellValueFactory(new PropertyValueFactory<>("focusLostCount"));
    }

    public void setExam(Exam exam) {
        this.currentExam = exam;
        examNameLabel.setText("Results for: " + exam.getName());
        loadResults();
    }

    private void loadResults() {
        List<Result> results = examService.getResultsByExamId(currentExam.getId());
        resultsTable.getItems().setAll(results);
    }

    @FXML
    private void handleBack() {
        ViewManager.getInstance().switchView("/com/actest/admin/view/dashboard_offline.fxml",
                "Admin Dashboard - Offline");
    }

    @FXML
    private void handleExportExcel() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Excel File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        fileChooser.setInitialFileName("Results_" + currentExam.getName().replaceAll("\\s+", "_") + ".xlsx");
        File file = fileChooser.showSaveDialog(resultsTable.getScene().getWindow());

        if (file != null) {
            try (Workbook workbook = new XSSFWorkbook()) {
                Sheet sheet = workbook.createSheet("Results");

                // Header row
                Row headerRow = sheet.createRow(0);
                headerRow.createCell(0).setCellValue("Student Name");
                headerRow.createCell(1).setCellValue("Score");
                headerRow.createCell(2).setCellValue("Focus Lost");

                // Data rows
                List<Result> results = resultsTable.getItems();
                for (int i = 0; i < results.size(); i++) {
                    Result r = results.get(i);
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

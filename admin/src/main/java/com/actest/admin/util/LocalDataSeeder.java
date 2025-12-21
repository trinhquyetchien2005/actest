package com.actest.admin.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class LocalDataSeeder {

    private static final String URL = "jdbc:sqlite:actest_admin.db";

    public static void main(String[] args) {
        System.out.println("Starting local database seeding...");
        try (Connection conn = DriverManager.getConnection(URL)) {
            if (conn != null) {
                com.actest.admin.repository.DatabaseConnection.initializeDatabase(); // Ensure tables exist
                seedExams(conn);
                seedQuestions(conn);
                System.out.println("Seeding completed successfully.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void seedExams(Connection conn) throws SQLException {
        String sql = "INSERT INTO exams (name, duration, status, creator_id) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            // Check how many exams exist first to avoid duplicates if run multiple times
            int existingExams = 0;
            try (java.sql.Statement stmt = conn.createStatement();
                    java.sql.ResultSet rs = stmt
                            .executeQuery("SELECT COUNT(*) FROM exams WHERE name LIKE 'Local Programming Exam %'")) {
                if (rs.next()) {
                    existingExams = rs.getInt(1);
                }
            }

            if (existingExams < 10) {
                for (int i = existingExams + 1; i <= 10; i++) {
                    pstmt.setString(1, "Local Programming Exam " + i);
                    pstmt.setInt(2, 60); // 60 minutes
                    pstmt.setString(3, "NOT_STARTED");
                    pstmt.setObject(4, null); // No creator for local exams usually
                    pstmt.addBatch();
                }
                pstmt.executeBatch();
                System.out.println("Inserted " + (10 - existingExams) + " exams.");
            } else {
                System.out.println("Exams already exist. Skipping exam creation.");
            }
        }
    }

    private static void seedQuestions(Connection conn) throws SQLException {
        // Fetch all exam IDs
        java.util.List<Integer> examIds = new java.util.ArrayList<>();
        try (java.sql.Statement stmt = conn.createStatement();
                java.sql.ResultSet rs = stmt.executeQuery("SELECT id FROM exams")) {
            while (rs.next()) {
                examIds.add(rs.getInt("id"));
            }
        }

        String countSql = "SELECT COUNT(*) FROM questions WHERE exam_id = ?";
        // Use legacy schema compatible INSERT
        String insertSql = "INSERT INTO questions (exam_id, content, options, correct_option_index, correct_answer, wrong_answer1, wrong_answer2, wrong_answer3) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement countStmt = conn.prepareStatement(countSql);
                PreparedStatement insertStmt = conn.prepareStatement(insertSql,
                        java.sql.Statement.RETURN_GENERATED_KEYS)) {

            for (int examId : examIds) {
                countStmt.setInt(1, examId);
                int currentCount = 0;
                try (java.sql.ResultSet rs = countStmt.executeQuery()) {
                    if (rs.next()) {
                        currentCount = rs.getInt(1);
                    }
                }

                if (currentCount < 20) { // Ensure at least 20 questions
                    System.out.println(
                            "Exam " + examId + " has " + currentCount + " questions. Adding more to reach 20...");
                    int needed = 20 - currentCount;
                    for (int q = 1; q <= needed; q++) {
                        insertStmt.setInt(1, examId);
                        insertStmt.setString(2, "Programming Question " + (currentCount + q) + " for Exam " + examId);

                        String[] options = { "Option A", "Option B", "Option C", "Option D" };
                        String optionsJson = "[\"" + options[0] + "\", \"" + options[1] + "\", \"" + options[2]
                                + "\", \"" + options[3] + "\"]";

                        insertStmt.setString(3, optionsJson);
                        int correctIndex = (q % 4);
                        insertStmt.setInt(4, correctIndex);

                        // Legacy columns
                        String correctAnswer = options[correctIndex];
                        String wrongAnswer1 = "";
                        String wrongAnswer2 = "";
                        String wrongAnswer3 = "";

                        int wrongCount = 0;
                        for (int i = 0; i < options.length; i++) {
                            if (i == correctIndex)
                                continue;
                            if (wrongCount == 0)
                                wrongAnswer1 = options[i];
                            else if (wrongCount == 1)
                                wrongAnswer2 = options[i];
                            else if (wrongCount == 2)
                                wrongAnswer3 = options[i];
                            wrongCount++;
                        }

                        insertStmt.setString(5, correctAnswer);
                        insertStmt.setString(6, wrongAnswer1);
                        insertStmt.setString(7, wrongAnswer2);
                        insertStmt.setString(8, wrongAnswer3);

                        insertStmt.executeUpdate();

                    }
                }
            }
            System.out.println("Question seeding check completed.");
        }
    }
}

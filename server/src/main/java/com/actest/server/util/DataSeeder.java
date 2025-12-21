package com.actest.server.util;

import com.actest.server.repository.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

public class DataSeeder {

    public static void seed() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            if (isDatabaseEmpty(conn)) {
                System.out.println("Seeding database with dummy data...");
                // seedUsers(conn); // Removed
                seedExams(conn);
                seedQuestions(conn);
                // seedMessages(conn); // Removed
                System.out.println("Database seeding completed.");
            } else {
                System.out.println("Database already contains data. Skipping seed.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static boolean isDatabaseEmpty(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM exams")) {
            if (rs.next()) {
                return rs.getInt(1) == 0;
            }
        }
        return true;
    }

    private static void seedExams(Connection conn) throws Exception {
        String sql = "INSERT INTO exams (name, duration, status, creator_id) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            // Existing Exams
            pstmt.setString(1, "Java Basics");
            pstmt.setInt(2, 30);
            pstmt.setString(3, "NOT_STARTED");
            pstmt.setObject(4, null); // No creator
            pstmt.addBatch();

            pstmt.setString(1, "Advanced Java");
            pstmt.setInt(2, 60);
            pstmt.setString(3, "NOT_STARTED");
            pstmt.setObject(4, null); // No creator
            pstmt.addBatch();

            // New Exams
            for (int i = 1; i <= 5; i++) {
                pstmt.setString(1, "Mock Exam " + i);
                pstmt.setInt(2, 45);
                pstmt.setString(3, "NOT_STARTED");
                pstmt.setObject(4, null); // No creator
                pstmt.addBatch();
            }

            pstmt.executeBatch();
        }
    }

    private static void seedQuestions(Connection conn) throws Exception {
        // Get Exam IDs - Assuming serials 1 and 2 exist, and new ones are 3, 4, 5, 6, 7
        // We will seed questions for all 7 exams.

        String sql = "INSERT INTO questions (exam_id, content, options, correct_option_index) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            // Questions for Exam 1 (Java Basics)
            addQuestion(pstmt, 1, "What is the size of int in Java?", "[\"16 bit\", \"32 bit\", \"64 bit\", \"8 bit\"]",
                    1);
            addQuestion(pstmt, 1, "Which keyword is used to define a class?",
                    "[\"class\", \"struct\", \"define\", \"interface\"]", 0);
            // Add 28 more dummy questions for Exam 1
            for (int i = 3; i <= 30; i++) {
                addQuestion(pstmt, 1, "Java Basics Question " + i,
                        "[\"Option A\", \"Option B\", \"Option C\", \"Option D\"]", 0);
            }

            // Questions for Exam 2 (Advanced Java)
            addQuestion(pstmt, 2, "What is a marker interface?",
                    "[\"Interface with no methods\", \"Interface with one method\", \"Interface with fields\", \"None of the above\"]",
                    0);
            // Add 29 more dummy questions for Exam 2
            for (int i = 2; i <= 30; i++) {
                addQuestion(pstmt, 2, "Advanced Java Question " + i,
                        "[\"Option A\", \"Option B\", \"Option C\", \"Option D\"]", 1);
            }

            // Questions for New Exams 3-7
            for (int examId = 3; examId <= 7; examId++) {
                for (int q = 1; q <= 30; q++) {
                    addQuestion(pstmt, examId, "Mock Exam " + (examId - 2) + " Question " + q,
                            "[\"Answer A\", \"Answer B\", \"Answer C\", \"Answer D\"]", (q % 4));
                }
            }

            pstmt.executeBatch();
        }
    }

    private static void addQuestion(PreparedStatement pstmt, int examId, String content, String options,
            int correctIndex) throws Exception {
        pstmt.setInt(1, examId);
        pstmt.setString(2, content);
        pstmt.setString(3, options);
        pstmt.setInt(4, correctIndex);
        pstmt.addBatch();
    }
}

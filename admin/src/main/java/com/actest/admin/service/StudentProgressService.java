package com.actest.admin.service;

import com.actest.admin.repository.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class StudentProgressService {

    public void saveAnswer(String studentName, int examId, int questionId, String answer) {
        String sql = "INSERT OR REPLACE INTO student_answers (student_name, exam_id, question_id, answer) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentName);
            pstmt.setInt(2, examId);
            pstmt.setInt(3, questionId);
            pstmt.setString(4, answer);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Map<Integer, String> getAnswers(String studentName, int examId) {
        Map<Integer, String> answers = new HashMap<>();
        String sql = "SELECT question_id, answer FROM student_answers WHERE student_name = ? AND exam_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentName);
            pstmt.setInt(2, examId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    answers.put(rs.getInt("question_id"), rs.getString("answer"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return answers;
    }

    public void saveViolation(String studentName, int examId, String type, String base64Image) {
        String sql = "INSERT INTO exam_violations (student_name, exam_id, violation_type, violation_time, image_data) VALUES (?, ?, ?, datetime('now'), ?)";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentName);
            pstmt.setInt(2, examId);
            pstmt.setString(3, type);
            pstmt.setString(4, base64Image);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int getViolationCount(String studentName, int examId) {
        String sql = "SELECT COUNT(*) FROM exam_violations WHERE student_name = ? AND exam_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentName);
            pstmt.setInt(2, examId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public static class Violation {
        public String type;
        public String violationTime;
        public String imageData;

        public Violation(String type, String violationTime, String imageData) {
            this.type = type;
            this.violationTime = violationTime;
            this.imageData = imageData;
        }
    }

    public java.util.List<Violation> getViolations(String studentName, int examId) {
        java.util.List<Violation> violations = new java.util.ArrayList<>();
        String sql = "SELECT violation_type, violation_time, image_data FROM exam_violations WHERE student_name = ? AND exam_id = ? ORDER BY violation_time DESC";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentName);
            pstmt.setInt(2, examId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    violations.add(new Violation(
                            rs.getString("violation_type"),
                            rs.getString("violation_time"),
                            rs.getString("image_data")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return violations;
    }
}

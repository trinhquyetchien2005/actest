package com.actest.server.repository;

import com.actest.server.model.Exam;
import com.actest.server.model.Question;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ExamRepository {

    public int createExam(Exam exam) {
        String sql = "INSERT INTO exams (name, duration, creator_id, status, allow_review) VALUES (?, ?, ?, ?, ?) RETURNING id";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, exam.getName());
            pstmt.setInt(2, exam.getDuration());
            pstmt.setInt(3, exam.getCreatorId());
            pstmt.setString(4, exam.getStatus() != null ? exam.getStatus() : "NOT_STARTED");
            pstmt.setBoolean(5, exam.isAllowReview());
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error creating exam: " + e.getMessage());
        }
        return -1;
    }

    public List<Exam> getAllExams() {
        List<Exam> exams = new ArrayList<>();
        String sql = "SELECT e.*, u.name as creator_name FROM exams e JOIN users u ON e.creator_id = u.id";
        try (Connection conn = DatabaseConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Exam exam = new Exam(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getInt("duration"),
                        rs.getInt("creator_id"));
                exam.setStatus(rs.getString("status"));
                exam.setCreatorName(rs.getString("creator_name"));
                exam.setAllowReview(rs.getBoolean("allow_review"));
                exams.add(exam);
            }
        } catch (SQLException e) {
            System.err.println("Error fetching exams: " + e.getMessage());
        }
        return exams;
    }

    public Exam getExamById(int id) {
        String sql = "SELECT * FROM exams WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Exam exam = new Exam(
                            rs.getInt("id"),
                            rs.getString("name"),
                            rs.getInt("duration"),
                            rs.getInt("creator_id"));
                    exam.setStatus(rs.getString("status"));
                    exam.setAllowReview(rs.getBoolean("allow_review"));
                    return exam;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching exam: " + e.getMessage());
        }
        return null;
    }
}

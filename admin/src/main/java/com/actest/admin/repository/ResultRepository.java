package com.actest.admin.repository;

import com.actest.admin.model.Result;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ResultRepository {

    public boolean saveResult(Result result) {
        String sql = "INSERT INTO results (student_name, focus_lost_count, score, exam_id) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, result.getStudentName());
            pstmt.setInt(2, result.getFocusLostCount());
            pstmt.setDouble(3, result.getScore());
            pstmt.setInt(4, result.getExamId());
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error saving result: " + e.getMessage());
            return false;
        }
    }

    public List<Result> getResultsByExamId(int examId) {
        List<Result> results = new ArrayList<>();
        String sql = "SELECT * FROM results WHERE exam_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, examId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    results.add(new Result(
                            rs.getInt("id"),
                            rs.getString("student_name"),
                            rs.getInt("focus_lost_count"),
                            rs.getDouble("score"),
                            rs.getInt("exam_id")));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching results: " + e.getMessage());
        }
        return results;
    }
}

package com.actest.server.repository;

import com.actest.server.model.Result;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ResultRepository {

    public boolean saveResult(Result result) {
        // Check if result already exists
        String checkSql = "SELECT COUNT(*) FROM results WHERE user_id = ? AND exam_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setInt(1, result.getUserId());
            checkStmt.setInt(2, result.getExamId());
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    System.out.println(
                            "Result already exists for user " + result.getUserId() + " in exam " + result.getExamId());
                    return true; // Treat as success to avoid errors on client
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking existing result: " + e.getMessage());
            return false;
        }

        String sql = "INSERT INTO results (user_id, exam_id, score, date_taken) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, result.getUserId());
            pstmt.setInt(2, result.getExamId());
            pstmt.setInt(3, result.getScore());
            pstmt.setString(4, result.getDateTaken());
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error saving result: " + e.getMessage());
            return false;
        }
    }

    public List<Result> getResultsByUserId(int userId) {
        List<Result> results = new ArrayList<>();
        String sql = "SELECT * FROM results WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    results.add(new Result(
                            rs.getInt("id"),
                            rs.getInt("user_id"),
                            rs.getInt("exam_id"),
                            rs.getInt("score"),
                            rs.getString("date_taken")));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching results: " + e.getMessage());
        }
        return results;
    }
}

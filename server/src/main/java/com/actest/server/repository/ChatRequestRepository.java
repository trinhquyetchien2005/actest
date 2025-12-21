package com.actest.server.repository;

import com.actest.server.model.ChatRequest;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ChatRequestRepository {

    public ChatRequestRepository() {
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        String sql = "CREATE TABLE IF NOT EXISTS chat_requests (" +
                "id SERIAL PRIMARY KEY," +
                "sender_id INTEGER NOT NULL," +
                "receiver_id INTEGER NOT NULL," +
                "status TEXT NOT NULL," +
                "UNIQUE(sender_id, receiver_id)" +
                ")";
        try (Connection conn = DatabaseConnection.getConnection();
                Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean sendRequest(int senderId, int receiverId) {
        String sql = "INSERT INTO chat_requests (sender_id, receiver_id, status) VALUES (?, ?, 'PENDING')";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, senderId);
            pstmt.setInt(2, receiverId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean acceptRequest(int requestId) {
        String sql = "UPDATE chat_requests SET status = 'ACCEPTED' WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, requestId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<ChatRequest> getPendingRequests(int userId) {
        List<ChatRequest> requests = new ArrayList<>();
        String sql = "SELECT cr.*, u.name as sender_name FROM chat_requests cr " +
                "JOIN users u ON cr.sender_id = u.id " +
                "WHERE cr.receiver_id = ? AND cr.status = 'PENDING'";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ChatRequest req = new ChatRequest(
                            rs.getInt("id"),
                            rs.getInt("sender_id"),
                            rs.getInt("receiver_id"),
                            rs.getString("status"));
                    req.setSenderName(rs.getString("sender_name"));
                    requests.add(req);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return requests;
    }

    public List<Integer> getFriendIds(int userId) {
        List<Integer> friendIds = new ArrayList<>();
        String sql = "SELECT sender_id, receiver_id FROM chat_requests " +
                "WHERE (sender_id = ? OR receiver_id = ?) AND status = 'ACCEPTED'";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    int s = rs.getInt("sender_id");
                    int r = rs.getInt("receiver_id");
                    friendIds.add(s == userId ? r : s);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return friendIds;
    }

    public boolean areFriends(int userId1, int userId2) {
        String sql = "SELECT 1 FROM chat_requests " +
                "WHERE ((sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?)) " +
                "AND status = 'ACCEPTED'";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId1);
            pstmt.setInt(2, userId2);
            pstmt.setInt(3, userId2);
            pstmt.setInt(4, userId1);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}

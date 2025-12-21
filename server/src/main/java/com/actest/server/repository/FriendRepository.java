package com.actest.server.repository;

import com.actest.server.model.FriendRequest;
import com.actest.server.model.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FriendRepository {

    public boolean sendFriendRequest(int senderId, int receiverId) {
        String sql = "INSERT INTO friend_requests (sender_id, receiver_id, status) VALUES (?, ?, 'PENDING')";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, senderId);
            pstmt.setInt(2, receiverId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error sending friend request: " + e.getMessage());
            return false;
        }
    }

    public boolean updateFriendRequestStatus(int requestId, String status) {
        String sql = "UPDATE friend_requests SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, requestId);
            return pstmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error updating friend request: " + e.getMessage());
            return false;
        }
    }

    public List<FriendRequest> getPendingRequests(int userId) {
        List<FriendRequest> requests = new ArrayList<>();
        String sql = "SELECT fr.*, u.name as sender_name, u.email as sender_email FROM friend_requests fr " +
                "JOIN users u ON fr.sender_id = u.id " +
                "WHERE fr.receiver_id = ? AND fr.status = 'PENDING'";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    FriendRequest req = new FriendRequest(
                            rs.getInt("id"),
                            rs.getInt("sender_id"),
                            rs.getInt("receiver_id"),
                            rs.getString("status"));
                    req.setSenderName(rs.getString("sender_name"));
                    req.setSenderEmail(rs.getString("sender_email"));
                    requests.add(req);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching pending requests: " + e.getMessage());
        }
        return requests;
    }

    public List<User> getFriends(int userId) {
        List<User> friends = new ArrayList<>();
        String sql = "SELECT u.id, u.name, u.email FROM users u " +
                "JOIN friend_requests fr ON (u.id = fr.sender_id OR u.id = fr.receiver_id) " +
                "WHERE (fr.sender_id = ? OR fr.receiver_id = ?) " +
                "AND fr.status = 'ACCEPTED' AND u.id != ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.setInt(2, userId);
            pstmt.setInt(3, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    User user = new User();
                    user.setId(rs.getInt("id"));
                    user.setName(rs.getString("name"));
                    user.setEmail(rs.getString("email"));
                    friends.add(user);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error fetching friends: " + e.getMessage());
        }
        return friends;
    }

    public List<User> searchUsers(String query, int currentUserId) {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, name, email FROM users WHERE (name ILIKE ? OR email ILIKE ?) AND id != ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            String searchPattern = "%" + query + "%";
            pstmt.setString(1, searchPattern);
            pstmt.setString(2, searchPattern);
            pstmt.setInt(3, currentUserId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    User user = new User();
                    user.setId(rs.getInt("id"));
                    user.setName(rs.getString("name"));
                    user.setEmail(rs.getString("email"));
                    users.add(user);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error searching users: " + e.getMessage());
        }
        return users;
    }

    public boolean areFriends(int userId1, int userId2) {
        String sql = "SELECT 1 FROM friend_requests WHERE " +
                "((sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?)) " +
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

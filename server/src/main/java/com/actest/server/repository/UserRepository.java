package com.actest.server.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class UserRepository {

    public boolean createUser(String email, String password, String name) {
        String sql = "INSERT INTO users (email, password, name) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            pstmt.setString(2, password);
            pstmt.setString(3, name);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error creating user: " + e.getMessage());
            return false;
        }
    }

    public boolean userExists(String email) {
        String sql = "SELECT 1 FROM users WHERE email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Error checking user existence: " + e.getMessage());
            return false;
        }
    }

    public boolean validateUser(String email, String password) {
        String sql = "SELECT 1 FROM users WHERE email = ? AND password = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            pstmt.setString(2, password);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("Error validating user: " + e.getMessage());
            return false;
        }
    }

    public boolean updateEmail(String currentEmail, String newEmail) {
        String sql = "UPDATE users SET email = ? WHERE email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, newEmail);
            pstmt.setString(2, currentEmail);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error updating email: " + e.getMessage());
            return false;
        }
    }

    public boolean updateProfile(String email, String name, String password) {
        String sql = "UPDATE users SET name = ?, password = ? WHERE email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, password);
            pstmt.setString(3, email);
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            System.err.println("Error updating profile: " + e.getMessage());
            return false;
        }
    }

    public int getUserIdByEmail(String email) {
        String sql = "SELECT id FROM users WHERE email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, email);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting user ID by email: " + e.getMessage());
        }
        return -1;
    }

    public java.util.List<com.actest.server.model.User> getAllUsers() {
        java.util.List<com.actest.server.model.User> users = new java.util.ArrayList<>();
        String sql = "SELECT id, email, name FROM users";
        try (Connection conn = DatabaseConnection.getConnection();
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                com.actest.server.model.User user = new com.actest.server.model.User();
                user.setId(rs.getInt("id"));
                user.setEmail(rs.getString("email"));
                user.setName(rs.getString("name"));
                users.add(user);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }
}

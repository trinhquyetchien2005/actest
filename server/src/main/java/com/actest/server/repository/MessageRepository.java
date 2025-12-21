package com.actest.server.repository;

import com.actest.server.model.Message;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageRepository {

    public void save(Message message) throws SQLException {
        String sql = "INSERT INTO messages (sender_id, receiver_id, content) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, message.getSenderId());
            pstmt.setInt(2, message.getReceiverId());
            pstmt.setString(3, message.getContent());
            pstmt.executeUpdate();
        }
    }

    public List<Message> getMessagesBetween(int userId1, int userId2) throws SQLException {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT m.*, u.name as sender_name FROM messages m " +
                "JOIN users u ON m.sender_id = u.id " +
                "WHERE (m.sender_id = ? AND m.receiver_id = ?) OR (m.sender_id = ? AND m.receiver_id = ?) " +
                "ORDER BY m.timestamp ASC";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId1);
            pstmt.setInt(2, userId2);
            pstmt.setInt(3, userId2);
            pstmt.setInt(4, userId1);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Message msg = new Message();
                    msg.setId(rs.getInt("id"));
                    msg.setSenderId(rs.getInt("sender_id"));
                    msg.setReceiverId(rs.getInt("receiver_id"));
                    msg.setContent(rs.getString("content"));
                    msg.setTimestamp(rs.getTimestamp("timestamp"));
                    msg.setSenderName(rs.getString("sender_name"));
                    messages.add(msg);
                }
            }
        }
        return messages;
    }
}

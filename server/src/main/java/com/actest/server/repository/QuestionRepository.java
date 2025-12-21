package com.actest.server.repository;

import com.actest.server.model.Question;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class QuestionRepository {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean addQuestionsToExam(int examId, List<Question> questions) {
        String sql = "INSERT INTO questions (exam_id, content, options, correct_option_index) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (Question q : questions) {
                pstmt.setInt(1, examId);
                pstmt.setString(2, q.getContent());
                pstmt.setString(3, objectMapper.writeValueAsString(q.getOptions()));
                pstmt.setInt(4, q.getCorrectOptionIndex());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
            return true;
        } catch (SQLException | JsonProcessingException e) {
            System.err.println("Error adding questions: " + e.getMessage());
            return false;
        }
    }

    public List<Question> getQuestionsByExamId(int examId) {
        List<Question> questions = new ArrayList<>();
        String sql = "SELECT * FROM questions WHERE exam_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, examId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    List<String> options = objectMapper.readValue(rs.getString("options"),
                            new TypeReference<List<String>>() {
                            });
                    questions.add(new Question(
                            rs.getInt("id"),
                            rs.getString("content"),
                            options,
                            rs.getInt("correct_option_index")));
                }
            }
        } catch (SQLException | JsonProcessingException e) {
            System.err.println("Error fetching questions: " + e.getMessage());
        }
        return questions;
    }
}

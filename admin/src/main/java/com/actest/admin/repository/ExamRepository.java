package com.actest.admin.repository;

import com.actest.admin.model.Exam;
import com.actest.admin.model.Question;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ExamRepository {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ExamRepository() {
        ensureSchemaUpdates();
    }

    private void ensureSchemaUpdates() {
        String sql = "ALTER TABLE exams ADD COLUMN allow_review BOOLEAN DEFAULT 0";
        try (Connection conn = DatabaseConnection.getConnection();
                Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            // Column likely already exists, ignore
            if (!e.getMessage().contains("duplicate column name")) {
                // e.printStackTrace(); // Optional: print if it's not the expected error
            }
        }
    }

    public int saveExam(Exam exam) {
        String sql = "INSERT INTO exams (name, duration, creator_id, status, allow_review) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, exam.getName());
            pstmt.setInt(2, exam.getDuration());
            pstmt.setInt(3, exam.getCreatorId());
            pstmt.setString(4, exam.getStatus());
            pstmt.setBoolean(5, exam.isAllowReview());
            int affectedRows = pstmt.executeUpdate();

            if (affectedRows == 0) {
                throw new SQLException("Creating exam failed, no rows affected.");
            }

            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Creating exam failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public void saveQuestion(Question question, int examId) {
        String sql = "INSERT INTO questions (content, options, correct_option_index, correct_answer, wrong_answer1, wrong_answer2, wrong_answer3, exam_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection()) {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, question.getContent());

                String optionsJson = "[]";
                try {
                    optionsJson = objectMapper.writeValueAsString(question.getOptions());
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
                pstmt.setString(2, optionsJson);
                pstmt.setInt(3, question.getCorrectOptionIndex());

                // Legacy columns support
                String correctAnswer = "";
                String wrong1 = "";
                String wrong2 = "";
                String wrong3 = "";

                if (question.getOptions() != null && !question.getOptions().isEmpty()) {
                    if (question.getCorrectOptionIndex() >= 0
                            && question.getCorrectOptionIndex() < question.getOptions().size()) {
                        correctAnswer = question.getOptions().get(question.getCorrectOptionIndex());
                    }

                    List<String> wrongs = new ArrayList<>();
                    for (int i = 0; i < question.getOptions().size(); i++) {
                        if (i != question.getCorrectOptionIndex()) {
                            wrongs.add(question.getOptions().get(i));
                        }
                    }
                    if (wrongs.size() > 0)
                        wrong1 = wrongs.get(0);
                    if (wrongs.size() > 1)
                        wrong2 = wrongs.get(1);
                    if (wrongs.size() > 2)
                        wrong3 = wrongs.get(2);
                }

                pstmt.setString(4, correctAnswer);
                pstmt.setString(5, wrong1);
                pstmt.setString(6, wrong2);
                pstmt.setString(7, wrong3);
                pstmt.setInt(8, examId);

                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Exam> getAllExams() {
        List<Exam> exams = new ArrayList<>();
        String sql = "SELECT * FROM exams";
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
                // Check if column exists (for backward compatibility if migration script fails)
                try {
                    exam.setAllowReview(rs.getBoolean("allow_review"));
                } catch (SQLException e) {
                    // Column might not exist yet
                    exam.setAllowReview(false);
                }
                exams.add(exam);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return exams;
    }

    public void updateStatus(int examId, String status) {
        String sql = "UPDATE exams SET status = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, status);
            pstmt.setInt(2, examId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
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
                    int id = rs.getInt("id");
                    String content = rs.getString("content");
                    String optionsJson = rs.getString("options");
                    int correctIndex = rs.getInt("correct_option_index");

                    List<String> options = new ArrayList<>();
                    if (optionsJson != null && !optionsJson.isEmpty()) {
                        options = objectMapper.readValue(optionsJson, new TypeReference<List<String>>() {
                        });
                    }

                    questions.add(new Question(id, content, options, correctIndex));
                }
            }
        } catch (SQLException | JsonProcessingException e) {
            e.printStackTrace();
        }
        return questions;
    }

    public void deleteQuestion(int questionId) {
        String sqlQ = "DELETE FROM questions WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection()) {

            try (PreparedStatement pstmt = conn.prepareStatement(sqlQ)) {
                pstmt.setInt(1, questionId);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void updateExam(Exam exam) {
        String sql = "UPDATE exams SET name = ?, duration = ?, allow_review = ? WHERE id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, exam.getName());
            pstmt.setInt(2, exam.getDuration());
            pstmt.setBoolean(3, exam.isAllowReview());
            pstmt.setInt(4, exam.getId());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteAllQuestions(int examId) {
        String sqlDeleteQ = "DELETE FROM questions WHERE exam_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sqlDeleteQ)) {
            pstmt.setInt(1, examId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void saveResult(String studentName, int examId, double score) {
        // Check if result already exists
        String checkSql = "SELECT COUNT(*) FROM results WHERE student_name = ? AND exam_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setString(1, studentName);
            checkStmt.setInt(2, examId);
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    System.out.println("Result already exists for student " + studentName + " in exam " + examId);
                    return;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return;
        }

        String sql = "INSERT INTO results (student_name, exam_id, score) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentName);
            pstmt.setInt(2, examId);
            pstmt.setDouble(3, score);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<com.actest.admin.model.Result> getResultsByExamId(int examId) {
        List<com.actest.admin.model.Result> results = new ArrayList<>();
        String sql = "SELECT * FROM results WHERE exam_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, examId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    results.add(new com.actest.admin.model.Result(
                            rs.getInt("id"),
                            rs.getString("student_name"),
                            rs.getInt("focus_lost_count"),
                            rs.getDouble("score"),
                            rs.getInt("exam_id")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return results;
    }

    public com.actest.admin.model.Result getResult(String studentName, int examId) {
        String sql = "SELECT * FROM results WHERE student_name = ? AND exam_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, studentName);
            pstmt.setInt(2, examId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new com.actest.admin.model.Result(
                            rs.getInt("id"),
                            rs.getString("student_name"),
                            rs.getInt("focus_lost_count"),
                            rs.getDouble("score"),
                            rs.getInt("exam_id"));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void deleteExam(int examId) {
        String sqlDeleteQ = "DELETE FROM questions WHERE exam_id = ?";
        String sqlDeleteExam = "DELETE FROM exams WHERE id = ?";

        try (Connection conn = DatabaseConnection.getConnection()) {
            // Delete questions
            try (PreparedStatement pstmt = conn.prepareStatement(sqlDeleteQ)) {
                pstmt.setInt(1, examId);
                pstmt.executeUpdate();
            }
            // Delete exam
            try (PreparedStatement pstmt = conn.prepareStatement(sqlDeleteExam)) {
                pstmt.setInt(1, examId);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

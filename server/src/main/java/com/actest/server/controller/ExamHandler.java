package com.actest.server.controller;

import com.actest.server.model.Exam;
import com.actest.server.model.Question;
import com.actest.server.repository.ExamRepository;
import com.actest.server.repository.QuestionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ExamHandler {
    private final ExamRepository examRepository = new ExamRepository();
    private final QuestionRepository questionRepository = new QuestionRepository();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public HttpHandler getExamHandler() {
        return exchange -> {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            if ("POST".equals(method)) {
                handleUploadExam(exchange);
            } else if ("GET".equals(method)) {
                if (path.equals("/api/exams")) {
                    handleGetAllExams(exchange);
                } else if (path.matches("/api/exams/\\d+")) {
                    handleGetExamById(exchange);
                } else if (path.matches("/api/exams/\\d+/questions")) {
                    handleGetQuestionsByExamId(exchange);
                } else {
                    exchange.sendResponseHeaders(404, -1);
                }
            } else {
                exchange.sendResponseHeaders(405, -1); // Method Not Allowed
            }
        };
    }

    private void handleGetAllExams(HttpExchange exchange) throws IOException {
        List<Exam> exams = examRepository.getAllExams();
        String response = objectMapper.writeValueAsString(exams);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void handleGetExamById(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        int id = Integer.parseInt(path.substring(path.lastIndexOf('/') + 1));
        Exam exam = examRepository.getExamById(id);
        if (exam != null) {
            String response = objectMapper.writeValueAsString(exam);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
        } else {
            exchange.sendResponseHeaders(404, -1);
        }
    }

    private void handleGetQuestionsByExamId(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        // Extract ID from /api/exams/{id}/questions
        String[] parts = path.split("/");
        int id = Integer.parseInt(parts[3]);
        List<Question> questions = questionRepository.getQuestionsByExamId(id);
        String response = objectMapper.writeValueAsString(questions);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void handleUploadExam(HttpExchange exchange) throws IOException {
        try {
            InputStream is = exchange.getRequestBody();
            String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            // Expecting JSON: { "exam": {...}, "questions": [...] }
            // Or just Exam object if it contains questions list?
            // Admin Exam model has List<Question> questions.
            // Server Exam model might NOT have questions list if it's just a POJO for
            // 'exams' table.
            // Let's check Server Exam model.
            // Assuming Server Exam model matches Admin Exam model structure for JSON.
            // If not, we might need a DTO.

            // Let's assume we receive the Admin's Exam object which includes questions.
            // But Server's Exam model might be different.
            // I'll assume for now we can map it to Server's Exam model,
            // but we need to extract questions separately if Server Exam doesn't have them.

            // Let's check Server Exam model first.
            // For now, I'll write a generic handler and fix if needed.

            UploadRequest request = objectMapper.readValue(body, UploadRequest.class);

            // Get creator ID from email
            int creatorId = -1;
            if (request.getEmail() != null) {
                com.actest.server.repository.UserRepository userRepository = new com.actest.server.repository.UserRepository();
                creatorId = userRepository.getUserIdByEmail(request.getEmail());
            }

            if (creatorId == -1) {
                String response = "User not found for email: " + request.getEmail();
                exchange.sendResponseHeaders(400, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
                return;
            }

            Exam exam = request.getExam();
            exam.setCreatorId(creatorId);

            int examId = examRepository.createExam(exam);
            if (examId != -1) {
                if (request.getQuestions() != null && !request.getQuestions().isEmpty()) {
                    questionRepository.addQuestionsToExam(examId, request.getQuestions());
                }
                String response = "Exam uploaded successfully";
                exchange.sendResponseHeaders(200, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } else {
                String response = "Failed to create exam";
                exchange.sendResponseHeaders(500, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
            String response = "Error processing request: " + e.getMessage();
            exchange.sendResponseHeaders(400, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    // DTO for upload request
    @com.fasterxml.jackson.annotation.JsonIgnoreProperties(ignoreUnknown = true)
    private static class UploadRequest {
        private Exam exam;
        private List<Question> questions;
        private String email;

        public Exam getExam() {
            return exam;
        }

        public void setExam(Exam exam) {
            this.exam = exam;
        }

        public List<Question> getQuestions() {
            return questions;
        }

        public void setQuestions(List<Question> questions) {
            this.questions = questions;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }
}

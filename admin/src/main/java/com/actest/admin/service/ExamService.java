package com.actest.admin.service;

import com.actest.admin.model.Exam;
import com.actest.admin.model.Question;
import com.actest.admin.repository.ExamRepository;
import java.util.List;
import java.util.Map;

public class ExamService {
    private final ExamRepository examRepository;

    public ExamService() {
        this.examRepository = new ExamRepository();
    }

    public int saveExam(Exam exam, List<Question> questions) {
        int examId = examRepository.saveExam(exam);
        if (examId != -1) {
            for (Question q : questions) {
                examRepository.saveQuestion(q, examId);
            }
            return examId;
        }
        return -1;
    }

    public List<Exam> getAllExams() {
        return examRepository.getAllExams();
    }

    public List<Exam> getLocalExams() {
        return getAllExams();
    }

    public void updateStatus(int examId, String status) {
        examRepository.updateStatus(examId, status);
    }

    public List<Question> getQuestionsByExamId(int examId) {
        return examRepository.getQuestionsByExamId(examId);
    }

    public void deleteQuestion(int questionId) {
        examRepository.deleteQuestion(questionId);
    }

    public void updateExam(Exam exam) {
        examRepository.updateExam(exam);
    }

    public void addQuestionToExam(Question question, int examId) {
        examRepository.saveQuestion(question, examId);
    }

    public void deleteAllQuestions(int examId) {
        examRepository.deleteAllQuestions(examId);
    }

    public double calculateScore(int examId, Map<Integer, String> answers) {
        List<Question> questions = getQuestionsByExamId(examId);
        int correctCount = 0;
        for (Question q : questions) {
            if (answers.containsKey(q.getId())) {
                String clientAnswer = answers.get(q.getId());
                // Get correct answer text
                String correctAnswer = q.getOptions().get(q.getCorrectOptionIndex());

                if (correctAnswer != null && correctAnswer.equals(clientAnswer)) {
                    correctCount++;
                }
            }
        }
        return questions.isEmpty() ? 0 : (double) correctCount / questions.size() * 10.0;
    }

    public void saveResult(String studentName, int examId, double score) {
        examRepository.saveResult(studentName, examId, score);
    }

    public boolean uploadExam(Exam exam) {
        try {
            List<Question> questions = getQuestionsByExamId(exam.getId());
            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("exam", exam);
            payload.put("questions", questions);
            payload.put("email", com.actest.admin.AdminApp.currentUserEmail);

            java.net.http.HttpResponse<String> response = com.actest.admin.util.HttpClientUtil.post("/exams", payload);
            if (response.statusCode() == 200) {
                System.out.println("Exam uploaded successfully");
                return true;
            } else {
                System.err.println("Failed to upload exam: " + response.body());
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<com.actest.admin.model.Result> getResultsByExamId(int examId) {
        return examRepository.getResultsByExamId(examId);
    }

    public com.actest.admin.model.Result getResult(String studentName, int examId) {
        return examRepository.getResult(studentName, examId);
    }

    public List<Exam> getServerExams() {
        try {
            java.net.http.HttpResponse<String> response = com.actest.admin.util.HttpClientUtil.get("/exams");
            if (response.statusCode() == 200) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                return mapper.readValue(response.body(),
                        new com.fasterxml.jackson.core.type.TypeReference<List<Exam>>() {
                        });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new java.util.ArrayList<>();
    }

    public boolean downloadExam(int examId) {
        try {
            java.net.http.HttpResponse<String> response = com.actest.admin.util.HttpClientUtil.get("/exams/" + examId);
            if (response.statusCode() == 200) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Exam exam = mapper.readValue(response.body(), Exam.class);

                // Fetch questions for this exam
                java.net.http.HttpResponse<String> qResponse = com.actest.admin.util.HttpClientUtil
                        .get("/exams/" + examId + "/questions");
                if (qResponse.statusCode() == 200) {
                    List<Question> questions = mapper.readValue(qResponse.body(),
                            new com.fasterxml.jackson.core.type.TypeReference<List<Question>>() {
                            });

                    // Save to local DB
                    int localExamId = examRepository.saveExam(exam);
                    if (localExamId != -1) {
                        for (Question q : questions) {
                            examRepository.saveQuestion(q, localExamId);
                        }
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<Question> getServerQuestions(int examId) {
        try {
            java.net.http.HttpResponse<String> response = com.actest.admin.util.HttpClientUtil
                    .get("/exams/" + examId + "/questions");
            if (response.statusCode() == 200) {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                return mapper.readValue(response.body(),
                        new com.fasterxml.jackson.core.type.TypeReference<List<Question>>() {
                        });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new java.util.ArrayList<>();
    }

    public void deleteExam(int examId) {
        examRepository.deleteExam(examId);
    }
}

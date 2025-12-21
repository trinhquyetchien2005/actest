package com.actest.server.service;

import com.actest.server.model.Exam;
import com.actest.server.model.Question;
import com.actest.server.repository.ExamRepository;
import com.actest.server.repository.QuestionRepository;

import java.util.List;

public class ExamService {
    private final ExamRepository examRepository;
    private final QuestionRepository questionRepository;

    public ExamService() {
        this.examRepository = new ExamRepository();
        this.questionRepository = new QuestionRepository();
    }

    public boolean createExam(Exam exam) {
        int examId = examRepository.createExam(exam);
        if (examId != -1) {
            if (exam.getQuestions() != null && !exam.getQuestions().isEmpty()) {
                return questionRepository.addQuestionsToExam(examId, exam.getQuestions());
            }
            return true;
        }
        return false;
    }

    public List<Exam> getAllExams() {
        return examRepository.getAllExams();
    }

    public Exam getExamById(int id) {
        Exam exam = examRepository.getExamById(id);
        if (exam != null) {
            List<Question> questions = questionRepository.getQuestionsByExamId(id);
            exam.setQuestions(questions);
        }
        return exam;
    }
}

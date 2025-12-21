package com.actest.admin.model;

public class Result {
    private int id;
    private String studentName;
    private int focusLostCount;
    private double score;
    private int examId;

    public Result() {
    }

    public Result(int id, String studentName, int focusLostCount, double score, int examId) {
        this.id = id;
        this.studentName = studentName;
        this.focusLostCount = focusLostCount;
        this.score = score;
        this.examId = examId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public int getFocusLostCount() {
        return focusLostCount;
    }

    public void setFocusLostCount(int focusLostCount) {
        this.focusLostCount = focusLostCount;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public int getExamId() {
        return examId;
    }

    public void setExamId(int examId) {
        this.examId = examId;
    }
}

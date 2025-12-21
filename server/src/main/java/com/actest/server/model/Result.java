package com.actest.server.model;

public class Result {
    private int id;
    private int userId;
    private int examId;
    private int score;
    private String dateTaken;

    public Result() {
    }

    public Result(int id, int userId, int examId, int score, String dateTaken) {
        this.id = id;
        this.userId = userId;
        this.examId = examId;
        this.score = score;
        this.dateTaken = dateTaken;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getExamId() {
        return examId;
    }

    public void setExamId(int examId) {
        this.examId = examId;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public String getDateTaken() {
        return dateTaken;
    }

    public void setDateTaken(String dateTaken) {
        this.dateTaken = dateTaken;
    }
}

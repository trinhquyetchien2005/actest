package com.actest.admin.model;

import java.util.List;

public class Exam {
    private int id;
    private String name;
    private int duration; // in minutes
    private int creatorId;
    private List<Question> questions;
    private String status; // "NOT_STARTED", "IN_PROGRESS", "FINISHED"
    private String creatorName;

    public Exam() {
    }

    public Exam(String name, int duration) {
        this.name = name;
        this.duration = duration;
        this.status = "NOT_STARTED";
    }

    public Exam(int id, String name, int duration, int creatorId) {
        this.id = id;
        this.name = name;
        this.duration = duration;
        this.creatorId = creatorId;
        this.status = "NOT_STARTED";
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(int creatorId) {
        this.creatorId = creatorId;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatorName() {
        return creatorName;
    }

    public void setCreatorName(String creatorName) {
        this.creatorName = creatorName;
    }

    private boolean allowReview;

    public boolean isAllowReview() {
        return allowReview;
    }

    public void setAllowReview(boolean allowReview) {
        this.allowReview = allowReview;
    }
}

package com.actest.server.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Exam {
    private int id;
    private String name;
    private int duration; // in minutes
    private int creatorId;
    private String status;
    private String creatorName;
    private boolean allowReview;
    private List<Question> questions;

    public Exam() {
    }

    public Exam(int id, String name, int duration, int creatorId) {
        this.id = id;
        this.name = name;
        this.duration = duration;
        this.creatorId = creatorId;
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

    public boolean isAllowReview() {
        return allowReview;
    }

    public void setAllowReview(boolean allowReview) {
        this.allowReview = allowReview;
    }
}

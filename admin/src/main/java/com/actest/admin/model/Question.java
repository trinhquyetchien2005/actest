package com.actest.admin.model;

import java.util.List;

public class Question {
    private int id;
    private String content;
    private List<String> options;
    private int correctOptionIndex;

    public Question() {
    }

    public Question(int id, String content, List<String> options, int correctOptionIndex) {
        this.id = id;
        this.content = content;
        this.options = options;
        this.correctOptionIndex = correctOptionIndex;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public int getCorrectOptionIndex() {
        return correctOptionIndex;
    }

    public void setCorrectOptionIndex(int correctOptionIndex) {
        this.correctOptionIndex = correctOptionIndex;
    }
}

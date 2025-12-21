package com.actest.server.model;

public class ChatRequest {
    private int id;
    private int senderId;
    private int receiverId;
    private String status; // PENDING, ACCEPTED, REJECTED
    private String senderName; // For display

    public ChatRequest() {
    }

    public ChatRequest(int id, int senderId, int receiverId, String status) {
        this.id = id;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.status = status;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSenderId() {
        return senderId;
    }

    public void setSenderId(int senderId) {
        this.senderId = senderId;
    }

    public int getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(int receiverId) {
        this.receiverId = receiverId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSenderName() {
        return senderName;
    }

    public void setSenderName(String senderName) {
        this.senderName = senderName;
    }
}

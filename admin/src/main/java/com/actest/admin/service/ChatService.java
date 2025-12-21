package com.actest.admin.service;

import com.actest.admin.util.HttpClientUtil;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;

public class ChatService {
    private static final String BASE_URL = "http://localhost:8080/api";
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Map<String, Object>> searchUsers(String query, int currentUserId) throws Exception {
        var response = HttpClientUtil
                .get("/users/search?q=" + java.net.URLEncoder.encode(query, "UTF-8") + "&userId=" + currentUserId);
        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), new TypeReference<List<Map<String, Object>>>() {
            });
        }
        return new java.util.ArrayList<>();
    }

    public List<Map<String, Object>> getMessages(int userId1, int userId2) throws Exception {
        var response = HttpClientUtil.get("/messages?userId1=" + userId1 + "&userId2=" + userId2);
        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), new TypeReference<List<Map<String, Object>>>() {
            });
        }
        return new java.util.ArrayList<>();
    }

    public void sendMessage(int senderId, int receiverId, String content) throws Exception {
        Map<String, Object> message = new java.util.HashMap<>();
        message.put("senderId", senderId);
        message.put("receiverId", receiverId);
        message.put("content", content);

        HttpClientUtil.post("/messages", message);
    }

    public boolean sendRequest(int senderId, int receiverId) throws Exception {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("senderId", senderId);
        body.put("receiverId", receiverId);
        var response = HttpClientUtil.post("/friend-request", body);
        return response.statusCode() == 200;
    }

    public boolean respondToRequest(int requestId, String status) throws Exception {
        Map<String, Object> body = new java.util.HashMap<>();
        body.put("status", status);
        var response = HttpClientUtil.put("/friend-request/" + requestId, body);
        return response.statusCode() == 200;
    }

    public List<Map<String, Object>> getPendingRequests(int userId) throws Exception {
        var response = HttpClientUtil.get("/friend-requests?userId=" + userId);
        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), new TypeReference<List<Map<String, Object>>>() {
            });
        }
        return new java.util.ArrayList<>();
    }

    public List<Map<String, Object>> getFriends(int userId) throws Exception {
        var response = HttpClientUtil.get("/friends?userId=" + userId);
        if (response.statusCode() == 200) {
            return objectMapper.readValue(response.body(), new TypeReference<List<Map<String, Object>>>() {
            });
        }
        return new java.util.ArrayList<>();
    }
}

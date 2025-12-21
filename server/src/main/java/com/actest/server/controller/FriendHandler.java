package com.actest.server.controller;

import com.actest.server.model.FriendRequest;
import com.actest.server.model.User;
import com.actest.server.repository.FriendRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class FriendHandler {
    private final FriendRepository friendRepository = new FriendRepository();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public HttpHandler getFriendHandler() {
        return exchange -> {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            try {
                if ("POST".equals(method) && path.equals("/api/friend-request")) {
                    handleSendRequest(exchange);
                } else if ("PUT".equals(method) && path.matches("/api/friend-request/\\d+")) {
                    handleUpdateRequest(exchange);
                } else if ("GET".equals(method)) {
                    if (path.equals("/api/friends")) {
                        handleGetFriends(exchange);
                    } else if (path.equals("/api/friend-requests")) {
                        handleGetPendingRequests(exchange);
                    } else if (path.equals("/api/users/search")) {
                        handleSearchUsers(exchange);
                    } else {
                        sendResponse(exchange, 404, "Not Found");
                    }
                } else {
                    sendResponse(exchange, 405, "Method Not Allowed");
                }
            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "Internal Server Error: " + e.getMessage());
            }
        };
    }

    private void handleSendRequest(HttpExchange exchange) throws IOException {
        Map<String, Object> body = objectMapper.readValue(exchange.getRequestBody(), Map.class);
        int senderId = (int) body.get("senderId");
        int receiverId = (int) body.get("receiverId");

        if (friendRepository.sendFriendRequest(senderId, receiverId)) {
            sendResponse(exchange, 200, "Friend request sent");
        } else {
            sendResponse(exchange, 400, "Failed to send request");
        }
    }

    private void handleUpdateRequest(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        int requestId = Integer.parseInt(path.substring(path.lastIndexOf('/') + 1));
        Map<String, Object> body = objectMapper.readValue(exchange.getRequestBody(), Map.class);
        String status = (String) body.get("status");

        if (friendRepository.updateFriendRequestStatus(requestId, status)) {
            sendResponse(exchange, 200, "Request updated");
        } else {
            sendResponse(exchange, 400, "Failed to update request");
        }
    }

    private void handleGetFriends(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        int userId = Integer.parseInt(query.split("=")[1]);
        List<User> friends = friendRepository.getFriends(userId);
        String response = objectMapper.writeValueAsString(friends);
        sendResponse(exchange, 200, response);
    }

    private void handleGetPendingRequests(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        int userId = Integer.parseInt(query.split("=")[1]);
        List<FriendRequest> requests = friendRepository.getPendingRequests(userId);
        String response = objectMapper.writeValueAsString(requests);
        sendResponse(exchange, 200, response);
    }

    private void handleSearchUsers(HttpExchange exchange) throws IOException {
        String query = exchange.getRequestURI().getQuery();
        String searchQuery = "";
        int currentUserId = -1;

        for (String param : query.split("&")) {
            String[] pair = param.split("=");
            if (pair[0].equals("q"))
                searchQuery = pair[1];
            if (pair[0].equals("userId"))
                currentUserId = Integer.parseInt(pair[1]);
        }

        List<User> users = friendRepository.searchUsers(searchQuery, currentUserId);
        String response = objectMapper.writeValueAsString(users);
        sendResponse(exchange, 200, response);
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, response.length());
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(response.getBytes(StandardCharsets.UTF_8));
        }
    }
}

package com.actest.server.controller;

import com.actest.server.model.Message;
import com.actest.server.model.User;
import com.actest.server.repository.MessageRepository;
import com.actest.server.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class ChatHandler {

    private final com.actest.server.repository.MessageRepository messageRepository = new com.actest.server.repository.MessageRepository();
    private final com.actest.server.repository.UserRepository userRepository = new com.actest.server.repository.UserRepository();
    private final com.actest.server.repository.FriendRepository friendRepository = new com.actest.server.repository.FriendRepository();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public HttpHandler getChatHandler() {
        return exchange -> {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();

            if ("POST".equals(method)) {
                handleSendMessage(exchange);
            } else if ("GET".equals(method)) {
                handleGetMessages(exchange);
            } else {
                exchange.sendResponseHeaders(405, -1); // Method Not Allowed
            }
        };
    }

    public HttpHandler getUsersHandler() {
        return exchange -> {
            if ("GET".equals(exchange.getRequestMethod())) {
                handleGetUsers(exchange);
            } else {
                exchange.sendResponseHeaders(405, -1);
            }
        };
    }

    private void handleSendMessage(HttpExchange exchange) throws IOException {
        try {
            InputStream is = exchange.getRequestBody();
            Message message = objectMapper.readValue(is, Message.class);

            if (!friendRepository.areFriends(message.getSenderId(), message.getReceiverId())) {
                String response = "You are not friends with this user.";
                exchange.sendResponseHeaders(403, response.length());
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(response.getBytes(StandardCharsets.UTF_8));
                }
                return;
            }

            messageRepository.save(message);

            String response = "Message sent";
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            e.printStackTrace();
            String response = "Error sending message";
            exchange.sendResponseHeaders(500, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    private void handleGetMessages(HttpExchange exchange) throws IOException {
        try {
            String query = exchange.getRequestURI().getQuery();
            Map<String, String> params = queryToMap(query);

            int userId1 = Integer.parseInt(params.get("userId1"));
            int userId2 = Integer.parseInt(params.get("userId2"));

            List<Message> messages = messageRepository.getMessagesBetween(userId1, userId2);

            String response = objectMapper.writeValueAsString(messages);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            e.printStackTrace();
            String response = "Error fetching messages";
            exchange.sendResponseHeaders(500, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    private void handleGetUsers(HttpExchange exchange) throws IOException {
        try {
            // In a real app, we might filter users or exclude self.
            // For now, return all users so admin can choose who to chat with.
            // We need a method in UserRepository to get all users.
            // Let's assume we add it or use a raw query here if needed, but better to add
            // to repo.
            // For now, let's just return a hardcoded list or fetch from DB if we add the
            // method.
            // I'll add getAllUsers to UserRepository in the next step.

            // Placeholder response until repo update
            String response = "[]";

            // Actually, let's try to fetch if method exists, or just fail for now and I'll
            // fix it.
            List<User> users = userRepository.getAllUsers();
            response = objectMapper.writeValueAsString(users);

            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            e.printStackTrace();
            String response = "Error fetching users";
            exchange.sendResponseHeaders(500, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    private Map<String, String> queryToMap(String query) {
        Map<String, String> result = new java.util.HashMap<>();
        if (query == null)
            return result;
        for (String param : query.split("&")) {
            String[] entry = param.split("=");
            if (entry.length > 1) {
                result.put(entry[0], entry[1]);
            } else {
                result.put(entry[0], "");
            }
        }
        return result;
    }
}

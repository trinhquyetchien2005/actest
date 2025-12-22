package com.actest.server;

import com.actest.server.controller.AuthHandler;
import com.sun.net.httpserver.HttpServer;

import java.net.InetSocketAddress;

public class ServerApp {
    public static void main(String[] args) {
        System.out.println("Server is running...");
        try {
            com.actest.server.repository.DatabaseConnection.initializeDatabase();
            com.actest.server.util.DataSeeder.seed();

            HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
            AuthHandler authHandler = new AuthHandler();

            server.createContext("/api/login", authHandler.getLoginHandler());
            server.createContext("/api/register", authHandler.getRegisterHandler());
            server.createContext("/api/verify-otp", authHandler.getVerifyOtpHandler());
            server.createContext("/api/check-email", authHandler.getCheckEmailHandler());
            server.createContext("/api/initiate-email-change", authHandler.getInitiateEmailChangeHandler());
            server.createContext("/api/verify-email-change", authHandler.getVerifyEmailChangeHandler());
            server.createContext("/api/update-profile", authHandler.getUpdateProfileHandler());

            com.actest.server.controller.ExamHandler examHandler = new com.actest.server.controller.ExamHandler();
            server.createContext("/api/exams", examHandler.getExamHandler());

            com.actest.server.controller.ChatHandler chatHandler = new com.actest.server.controller.ChatHandler();
            server.createContext("/api/messages", chatHandler.getChatHandler());
            server.createContext("/api/users", chatHandler.getUsersHandler());

            com.actest.server.controller.FriendHandler friendHandler = new com.actest.server.controller.FriendHandler();
            server.createContext("/api/friend-request", friendHandler.getFriendHandler());
            server.createContext("/api/friends", friendHandler.getFriendHandler());
            server.createContext("/api/friend-requests", friendHandler.getFriendHandler());
            server.createContext("/api/users/search", friendHandler.getFriendHandler());

            server.setExecutor(null); // creates a default executor
            server.start();
            System.out.println("HTTP Server started on port 8080");

            // Start Video Call Server
            new com.actest.server.network.VideoCallServer().start();

            // Start Chat Server
            new com.actest.server.network.ChatServer().start();

        } catch (Exception e) {
            System.err.println("Failed to start server: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

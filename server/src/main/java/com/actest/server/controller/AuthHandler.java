package com.actest.server.controller;

import com.actest.server.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class AuthHandler {
    private final UserService userService;
    private final ObjectMapper objectMapper;

    public AuthHandler() {
        this.userService = new UserService();
        this.objectMapper = new ObjectMapper();
    }

    public HttpHandler getLoginHandler() {
        return exchange -> {
            if ("POST".equals(exchange.getRequestMethod())) {
                handleLogin(exchange);
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        };
    }

    public HttpHandler getRegisterHandler() {
        return exchange -> {
            if ("POST".equals(exchange.getRequestMethod())) {
                handleRegister(exchange);
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        };
    }

    public HttpHandler getVerifyOtpHandler() {
        return exchange -> {
            if ("POST".equals(exchange.getRequestMethod())) {
                handleVerifyOtp(exchange);
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        };
    }

    public HttpHandler getCheckEmailHandler() {
        return exchange -> {
            if ("POST".equals(exchange.getRequestMethod())) {
                handleCheckEmail(exchange);
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        };
    }

    public HttpHandler getInitiateEmailChangeHandler() {
        return exchange -> {
            if ("POST".equals(exchange.getRequestMethod())) {
                handleInitiateEmailChange(exchange);
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        };
    }

    public HttpHandler getVerifyEmailChangeHandler() {
        return exchange -> {
            if ("POST".equals(exchange.getRequestMethod())) {
                handleVerifyEmailChange(exchange);
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        };
    }

    public HttpHandler getUpdateProfileHandler() {
        return exchange -> {
            if ("POST".equals(exchange.getRequestMethod())) {
                handleUpdateProfile(exchange);
            } else {
                sendResponse(exchange, 405, "Method Not Allowed");
            }
        };
    }

    private void handleLogin(HttpExchange exchange) throws IOException {
        Map<String, String> request = parseRequest(exchange);
        String email = request.get("email");
        String password = request.get("password");

        if (userService.login(email, password)) {
            int userId = userService.getUserIdByEmail(email);
            String response = String.format("{\"message\": \"Login Successful\", \"userId\": %d}", userId);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            sendResponse(exchange, 200, response);
        } else {
            sendResponse(exchange, 401, "Invalid Credentials");
        }
    }

    private void handleRegister(HttpExchange exchange) throws IOException {
        Map<String, String> request = parseRequest(exchange);
        String email = request.get("email");
        String password = request.get("password");
        String name = request.get("name");

        if (userService.initiateRegistration(email, password, name)) {
            sendResponse(exchange, 200, "OTP Sent");
        } else {
            sendResponse(exchange, 400, "User already exists or error");
        }
    }

    private void handleVerifyOtp(HttpExchange exchange) throws IOException {
        Map<String, String> request = parseRequest(exchange);
        String email = request.get("email");
        String otp = request.get("otp");

        if (userService.verifyOtpAndRegister(email, otp)) {
            sendResponse(exchange, 200, "Registration Successful");
        } else {
            sendResponse(exchange, 400, "Invalid OTP");
        }
    }

    private void handleCheckEmail(HttpExchange exchange) throws IOException {
        Map<String, String> request = parseRequest(exchange);
        String email = request.get("email");
        if (userService.isEmailAvailable(email)) {
            sendResponse(exchange, 200, "Email Available");
        } else {
            sendResponse(exchange, 409, "Email Already Exists");
        }
    }

    private void handleInitiateEmailChange(HttpExchange exchange) throws IOException {
        Map<String, String> request = parseRequest(exchange);
        String currentEmail = request.get("currentEmail");
        String newEmail = request.get("newEmail");
        if (userService.initiateEmailChange(currentEmail, newEmail)) {
            sendResponse(exchange, 200, "OTP Sent");
        } else {
            sendResponse(exchange, 400, "Error initiating email change");
        }
    }

    private void handleVerifyEmailChange(HttpExchange exchange) throws IOException {
        Map<String, String> request = parseRequest(exchange);
        String currentEmail = request.get("currentEmail");
        String newEmail = request.get("newEmail");
        String otp = request.get("otp");
        if (userService.verifyEmailChangeOtp(currentEmail, newEmail, otp)) {
            sendResponse(exchange, 200, "Email Changed Successfully");
        } else {
            sendResponse(exchange, 400, "Invalid OTP");
        }
    }

    private void handleUpdateProfile(HttpExchange exchange) throws IOException {
        Map<String, String> request = parseRequest(exchange);
        String email = request.get("email");
        String name = request.get("name");
        String password = request.get("password");
        if (userService.updateProfile(email, name, password)) {
            sendResponse(exchange, 200, "Profile Updated Successfully");
        } else {
            sendResponse(exchange, 400, "Error updating profile");
        }
    }

    private Map<String, String> parseRequest(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        return objectMapper.readValue(is, Map.class);
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        OutputStream os = exchange.getResponseBody();
        os.write(responseBytes);
        os.close();
    }
}

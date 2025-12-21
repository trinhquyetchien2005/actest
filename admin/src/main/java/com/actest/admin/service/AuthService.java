package com.actest.admin.service;

import com.actest.admin.util.HttpClientUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;

public class AuthService {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static int currentUserId = -1;

    public static int getCurrentUserId() {
        return currentUserId;
    }

    public boolean login(String email, String password) throws IOException, InterruptedException {
        Map<String, Object> data = new HashMap<>();
        data.put("email", email);
        data.put("password", password);

        HttpResponse<String> response = HttpClientUtil.post("/login", data);
        if (response.statusCode() == 200) {
            Map<String, Object> responseBody = objectMapper.readValue(response.body(), Map.class);
            if (responseBody.containsKey("userId")) {
                currentUserId = (int) responseBody.get("userId");
            }
            return true;
        }
        return false;
    }

    public boolean register(String email, String password, String name) throws IOException, InterruptedException {
        Map<String, Object> data = new HashMap<>();
        data.put("email", email);
        data.put("password", password);
        data.put("name", name);

        HttpResponse<String> response = HttpClientUtil.post("/register", data);
        return response.statusCode() == 200;
    }

    public boolean verifyOtp(String email, String otp) throws IOException, InterruptedException {
        Map<String, Object> data = new HashMap<>();
        data.put("email", email);
        data.put("otp", otp);

        HttpResponse<String> response = HttpClientUtil.post("/verify-otp", data);
        return response.statusCode() == 200;
    }

    public boolean updateProfile(String email, String name, String password) throws IOException, InterruptedException {
        Map<String, Object> data = new HashMap<>();
        data.put("email", email);
        data.put("name", name);
        data.put("password", password);

        HttpResponse<String> response = HttpClientUtil.post("/update-profile", data);
        return response.statusCode() == 200;
    }

    public boolean checkEmail(String email) throws IOException, InterruptedException {
        Map<String, Object> data = new HashMap<>();
        data.put("email", email);
        HttpResponse<String> response = HttpClientUtil.post("/check-email", data);
        return response.statusCode() == 200;
    }

    public boolean initiateEmailChange(String currentEmail, String newEmail) throws IOException, InterruptedException {
        Map<String, Object> data = new HashMap<>();
        data.put("currentEmail", currentEmail);
        data.put("newEmail", newEmail);
        HttpResponse<String> response = HttpClientUtil.post("/initiate-email-change", data);
        return response.statusCode() == 200;
    }

    public boolean verifyEmailChange(String currentEmail, String newEmail, String otp)
            throws IOException, InterruptedException {
        Map<String, Object> data = new HashMap<>();
        data.put("currentEmail", currentEmail);
        data.put("newEmail", newEmail);
        data.put("otp", otp);
        HttpResponse<String> response = HttpClientUtil.post("/verify-email-change", data);
        return response.statusCode() == 200;
    }
}

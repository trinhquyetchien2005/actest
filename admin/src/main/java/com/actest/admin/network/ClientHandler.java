package com.actest.admin.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private final Socket socket;
    private final Server server;
    private BufferedReader in;
    private PrintWriter out;
    private String name;
    private final java.util.Map<Integer, String> answers = new java.util.HashMap<>();
    private int currentExamId = -1;

    public ClientHandler(Socket socket, Server server) {
        this.socket = socket;
        this.server = server;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getIpAddress() {
        if (socket != null && socket.getInetAddress() != null) {
            return socket.getInetAddress().getHostAddress();
        }
        return "Unknown IP";
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);

            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("Received from client: " + message);
                if (message.startsWith("ANSWER:")) {
                    handleAnswer(message);
                } else if (message.startsWith("RECONNECT:")) {
                    handleReconnect(message);
                } else if (message.equals("READY")) {
                    // Client is ready, but we already sent START_EXAM with full content.
                    // We can log it or maybe re-send if needed, but for now do nothing.
                    System.out.println("Client " + name + " is READY.");
                } else {
                    server.onMessage(this, message);
                }
            }
        } catch (IOException e) {
            // Client disconnected
        } finally {
            server.removeClient(this);
            close();
        }
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    public void close() {
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public enum ConnectionStatus {
        PENDING, ACCEPTED
    }

    private ConnectionStatus status = ConnectionStatus.PENDING;

    public ConnectionStatus getStatus() {
        return status;
    }

    public void accept() {
        this.status = ConnectionStatus.ACCEPTED;
        sendMessage("ACCEPTED");
    }

    public void reject() {
        sendMessage("REJECTED");
        close();
    }

    private void handleAnswer(String message) {
        // Format: ANSWER:examId:questionId:answer
        String[] parts = message.split(":", 4);
        if (parts.length == 4) {
            try {
                int examId = Integer.parseInt(parts[1]);
                int questionId = Integer.parseInt(parts[2]);
                String answer = parts[3];

                if (examId == currentExamId) {
                    answers.put(questionId, answer);
                    System.out.println("Stored answer for client " + name + ": Q" + questionId + "=" + answer);
                    // No auto-advance
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleReconnect(String message) {
        // Format: RECONNECT:name
        String[] parts = message.split(":", 2);
        if (parts.length == 2) {
            String clientName = parts[1];
            this.name = clientName; // Restore name

            // Check if there's an existing handler with this name (handled by Server
            // usually, but here we might need to find old state)
            // For simplicity, we assume the Server handles finding the old handler and we
            // might need to copy state or this IS the new handler.
            // Actually, if client reconnects, it's a new socket, so new ClientHandler.
            // We need to find the OLD ClientHandler (which might be in a disconnected state
            // or just kept around) and copy state.
            // Or Server should handle this.
            // Let's delegate to Server.
            server.onMessage(this, message);
        }
    }

    private int currentQuestionIndex = 0;
    private com.actest.admin.model.Exam currentExam;

    public void setCurrentExam(com.actest.admin.model.Exam exam) {
        this.currentExam = exam;
        this.currentQuestionIndex = 0;
        this.currentExamId = exam.getId();
    }

    public void startExam() {
        if (currentExam != null) {
            try {
                // Ensure questions are loaded
                if (currentExam.getQuestions() == null || currentExam.getQuestions().isEmpty()) {
                    // This might need service access, but usually controller sets it.
                    // Assuming controller set it before calling setCurrentExam.
                }

                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                String examJson = mapper.writeValueAsString(currentExam);
                sendMessage("START_EXAM:" + examJson);
                System.out.println("Sent Full Exam to " + name);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    // Reverted: No longer sending questions one by one
    public void sendNextQuestion() {
        // No-op for Azota style (Full exam loaded at start)
    }

    public int getCurrentQuestionIndex() {
        return currentQuestionIndex;
    }

    public void setCurrentQuestionIndex(int index) {
        this.currentQuestionIndex = index;
    }

    public java.util.Map<Integer, String> getAnswers() {
        return answers;
    }

    public void setCurrentExamId(int examId) {
        this.currentExamId = examId;
    }

    public int getCurrentExamId() {
        return currentExamId;
    }

    public void setAnswers(java.util.Map<Integer, String> savedAnswers) {
        this.answers.clear();
        this.answers.putAll(savedAnswers);
    }
}

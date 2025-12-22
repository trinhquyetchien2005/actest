package com.actest.server.network;

import com.actest.server.model.Message;
import com.actest.server.repository.MessageRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer implements Runnable {
    private static final int PORT = 9001;
    private ServerSocket serverSocket;
    private boolean running = false;
    private final Map<Integer, Socket> onlineUsers = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final MessageRepository messageRepository = new MessageRepository();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void start() {
        new Thread(this).start();
    }

    @Override
    public void run() {
        try {
            serverSocket = new ServerSocket(PORT);
            running = true;
            System.out.println("Chat TCP Server started on port " + PORT);

            while (running) {
                Socket clientSocket = serverSocket.accept();
                executor.submit(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            executor.shutdown();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class ClientHandler implements Runnable {
        private final Socket socket;
        private BufferedReader in;
        private PrintWriter out;
        private int userId = -1;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                String message;
                while ((message = in.readLine()) != null) {
                    handleMessage(message);
                }
            } catch (IOException e) {
                // Connection lost
            } finally {
                disconnect();
            }
        }

        private void handleMessage(String message) {
            // Protocol:
            // LOGIN:<userId>
            // MESSAGE:<receiverId>:<content>

            if (message.startsWith("LOGIN:")) {
                try {
                    userId = Integer.parseInt(message.substring(6));
                    onlineUsers.put(userId, socket);
                    System.out.println("User " + userId + " connected to Chat Server.");
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
            } else if (message.startsWith("MESSAGE:")) {
                try {
                    String[] parts = message.split(":", 3);
                    if (parts.length == 3) {
                        int receiverId = Integer.parseInt(parts[1]);
                        String content = parts[2];

                        // 1. Save to DB
                        Message msgObj = new Message();
                        msgObj.setSenderId(userId);
                        msgObj.setReceiverId(receiverId);
                        msgObj.setContent(content);
                        // Timestamp is handled by DB or Model default, but let's check Message model if
                        // needed.
                        // Assuming DB handles default timestamp or Repository does.
                        messageRepository.save(msgObj);

                        // 2. Forward if online
                        Socket receiverSocket = onlineUsers.get(receiverId);
                        if (receiverSocket != null && !receiverSocket.isClosed()) {
                            PrintWriter receiverOut = new PrintWriter(receiverSocket.getOutputStream(), true);
                            // Format: MESSAGE_RECEIVED:<senderId>:<content>
                            receiverOut.println("MESSAGE_RECEIVED:" + userId + ":" + content);
                        }

                        // 3. Confirm sent (optional, but good for UI)
                        // out.println("SENT");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        private void disconnect() {
            if (userId != -1) {
                onlineUsers.remove(userId);
                System.out.println("User " + userId + " disconnected from Chat Server.");
            }
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

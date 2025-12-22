package com.actest.admin.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ChatClient {
    private static final String SERVER_HOST = "localhost"; // Or from config
    private static final int SERVER_PORT = 9001;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private boolean connected = false;
    private ChatListener listener;

    public interface ChatListener {
        void onMessageReceived(int senderId, String content);
    }

    public void connect(int userId) {
        new Thread(() -> {
            try {
                socket = new Socket(SERVER_HOST, SERVER_PORT);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                connected = true;

                // Send Login
                out.println("LOGIN:" + userId);

                // Listen for messages
                String message;
                while (connected && (message = in.readLine()) != null) {
                    if (message.startsWith("MESSAGE_RECEIVED:")) {
                        // Format: MESSAGE_RECEIVED:<senderId>:<content>
                        String[] parts = message.split(":", 3);
                        if (parts.length == 3 && listener != null) {
                            try {
                                int senderId = Integer.parseInt(parts[1]);
                                String content = parts[2];
                                listener.onMessageReceived(senderId, content);
                            } catch (NumberFormatException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                disconnect();
            }
        }).start();
    }

    public void sendMessage(int receiverId, String content) {
        if (connected && out != null) {
            out.println("MESSAGE:" + receiverId + ":" + content);
        }
    }

    public void setListener(ChatListener listener) {
        this.listener = listener;
    }

    public void disconnect() {
        connected = false;
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

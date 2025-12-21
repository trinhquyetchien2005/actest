package com.actest.client.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class Client {
    private static final int PORT = 9999;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private boolean connected;

    public boolean connect(String serverAddress) {
        try {
            socket = new Socket(serverAddress, PORT);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream(), true);
            connected = true;

            // Start listener thread
            new Thread(this::listen).start();

            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public interface ClientListener {
        void onMessageReceived(String message);
    }

    private ClientListener listener;

    public void setListener(ClientListener listener) {
        this.listener = listener;
    }

    private void listen() {
        try {
            String message;
            while (connected && (message = in.readLine()) != null) {
                System.out.println("Received from server: " + message);
                if (listener != null) {
                    listener.onMessageReceived(message);
                }
            }
        } catch (IOException e) {
            if (connected) {
                e.printStackTrace();
            }
        } finally {
            close();
        }
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    public void close() {
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

package com.actest.admin.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private static final int PORT = 9999;
    private ServerSocket serverSocket;
    private boolean running;
    private final List<ClientHandler> clients = new ArrayList<>();
    private final ExecutorService pool = Executors.newCachedThreadPool();
    private com.actest.admin.model.Exam currentExam;

    public void setCurrentExam(com.actest.admin.model.Exam exam) {
        this.currentExam = exam;
    }

    public com.actest.admin.model.Exam getCurrentExam() {
        return currentExam;
    }

    public List<ClientHandler> getClients() {
        return new ArrayList<>(clients);
    }

    public List<ClientHandler> getPendingClients() {
        List<ClientHandler> pending = new ArrayList<>();
        for (ClientHandler client : clients) {
            if (client.getStatus() == ClientHandler.ConnectionStatus.PENDING) {
                pending.add(client);
            }
        }
        return pending;
    }

    public List<ClientHandler> getAcceptedClients() {
        List<ClientHandler> accepted = new ArrayList<>();
        for (ClientHandler client : clients) {
            if (client.getStatus() == ClientHandler.ConnectionStatus.ACCEPTED) {
                accepted.add(client);
            }
        }
        return accepted;
    }

    public void start() {
        running = true;
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(PORT);
                System.out.println("TCP Server started on port " + PORT);

                while (running) {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("New client connected: " + clientSocket.getInetAddress());
                    ClientHandler clientHandler = new ClientHandler(clientSocket, this);
                    clients.add(clientHandler);
                    if (listener != null) {
                        listener.onClientConnected(clientHandler);
                    }
                    pool.execute(clientHandler);
                }
            } catch (IOException e) {
                if (running) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            for (ClientHandler client : clients) {
                client.close();
            }
            pool.shutdownNow();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public interface ClientListener {
        void onClientConnected(ClientHandler client);

        void onClientDisconnected(ClientHandler client);

        void onMessageReceived(ClientHandler client, String message);
    }

    private ClientListener listener;

    public void setListener(ClientListener listener) {
        this.listener = listener;
    }

    public void removeClient(ClientHandler clientHandler) {
        clients.remove(clientHandler);
        if (listener != null) {
            listener.onClientDisconnected(clientHandler);
        }
        System.out.println("Client disconnected");
    }

    public void broadcast(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    public void onMessage(ClientHandler client, String message) {
        if (listener != null) {
            listener.onMessageReceived(client, message);
        }
    }
}

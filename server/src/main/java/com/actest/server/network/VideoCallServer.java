package com.actest.server.network;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VideoCallServer {
    private static final int PORT = 9000;
    private static final Map<Integer, Socket> clients = new ConcurrentHashMap<>();

    public void start() {
        new Thread(() -> {
            try (ServerSocket serverSocket = new ServerSocket(PORT)) {
                System.out.println("Video Call Server started on port " + PORT);

                while (true) {
                    Socket socket = serverSocket.accept();
                    new Thread(new ClientHandler(socket)).start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static class ClientHandler implements Runnable {
        private final Socket socket;
        private int userId = -1;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (DataInputStream in = new DataInputStream(socket.getInputStream());
                    DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

                while (true) {
                    // Protocol:
                    // REGISTER <userId>
                    // FRAME <targetUserId> <length> <data>

                    int type = in.readInt(); // 1=REGISTER, 2=FRAME

                    if (type == 1) { // REGISTER
                        userId = in.readInt();
                        clients.put(userId, socket);
                        System.out.println("Video Client Registered: " + userId);
                    } else if (type == 2) { // FRAME
                        int targetId = in.readInt();
                        int length = in.readInt();
                        byte[] data = new byte[length];
                        in.readFully(data);

                        // Relay to target
                        Socket targetSocket = clients.get(targetId);
                        if (targetSocket != null && !targetSocket.isClosed()) {
                            try {
                                synchronized (targetSocket) {
                                    DataOutputStream targetOut = new DataOutputStream(targetSocket.getOutputStream());
                                    targetOut.writeInt(2); // FRAME
                                    targetOut.writeInt(userId); // From User ID
                                    targetOut.writeInt(length);
                                    targetOut.write(data);
                                    targetOut.flush();
                                }
                            } catch (IOException e) {
                                System.out.println("Failed to relay frame to " + targetId);
                                clients.remove(targetId);
                            }
                        }
                    }
                }
            } catch (IOException e) {
                if (userId != -1) {
                    clients.remove(userId);
                    System.out.println("Video Client Disconnected: " + userId);
                }
            }
        }
    }
}

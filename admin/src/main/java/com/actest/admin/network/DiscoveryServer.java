package com.actest.admin.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class DiscoveryServer implements Runnable {
    private static final int DISCOVERY_PORT = 8888;
    private static final int RESPONSE_PORT = 8889;
    private final String serverName;
    private boolean running;
    private DatagramSocket socket;

    public DiscoveryServer(String serverName) {
        this.serverName = serverName;
    }

    public void start() {
        running = true;
        new Thread(this).start();
    }

    public void stop() {
        running = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    @Override
    public void run() {
        try {
            socket = new DatagramSocket(DISCOVERY_PORT, InetAddress.getByName("0.0.0.0"));
            socket.setBroadcast(true);
            System.out.println("Discovery Server started on port " + DISCOVERY_PORT);

            while (running) {
                byte[] buffer = new byte[1024];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String message = new String(packet.getData(), 0, packet.getLength());
                if ("ACTEST_DISCOVERY_REQUEST".equals(message)) {
                    System.out.println("Received discovery request from " + packet.getAddress());
                    sendResponse(packet.getAddress());
                }
            }
        } catch (IOException e) {
            if (running) {
                e.printStackTrace();
            }
        }
    }

    private void sendResponse(InetAddress clientAddress) {
        try (DatagramSocket responseSocket = new DatagramSocket()) {
            String response = "ACTEST_SERVER:" + serverName;
            byte[] responseData = response.getBytes();
            DatagramPacket responsePacket = new DatagramPacket(
                    responseData,
                    responseData.length,
                    clientAddress,
                    RESPONSE_PORT);
            responseSocket.send(responsePacket);
            System.out.println("Sent response to " + clientAddress);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

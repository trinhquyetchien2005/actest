package com.actest.server.network;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VideoCallServer implements Runnable {
    private static final int PORT = 9000;
    private static final int BUFFER_SIZE = 64000; // Max UDP size is ~65k
    private final Map<Integer, InetSocketAddress> clients = new ConcurrentHashMap<>();
    private DatagramSocket socket;
    private boolean running = false;

    public void start() {
        new Thread(this).start();
    }

    @Override
    public void run() {
        try {
            socket = new DatagramSocket(PORT);
            running = true;
            System.out.println("Video Call UDP Server started on port " + PORT);

            byte[] buffer = new byte[BUFFER_SIZE];

            while (running) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);


                try {
                    DataInputStream in = new DataInputStream(
                            new ByteArrayInputStream(packet.getData(), 0, packet.getLength()));
                    int type = in.readInt();
                    int senderId = in.readInt();

                    if (type == 1) { // REGISTER
                        InetSocketAddress address = (InetSocketAddress) packet.getSocketAddress();
                        clients.put(senderId, address);
                        System.out.println("UDP Client Registered: " + senderId + " at " + address);
                    } else if (type == 2 || type == 3) { // FRAME (2) or AUDIO (3)
                        int targetId = in.readInt();
                        InetSocketAddress targetAddress = clients.get(targetId);
                        if (targetAddress != null) {
                            DatagramPacket forwardPacket = new DatagramPacket(
                                    packet.getData(),
                                    packet.getLength(),
                                    targetAddress.getAddress(),
                                    targetAddress.getPort());
                            socket.send(forwardPacket);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }

    public void stop() {
        running = false;
        if (socket != null) {
            socket.close();
        }
    }
}

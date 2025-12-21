package com.actest.client.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class DiscoveryClient {
    private static final int DISCOVERY_PORT = 8888;
    private static final int RESPONSE_PORT = 8889;
    private static final int TIMEOUT_MS = 2000;

    public List<String> discoverServers() {
        List<String> servers = new ArrayList<>();
        DatagramSocket socket = null;

        try {
            socket = new DatagramSocket(RESPONSE_PORT);
            socket.setBroadcast(true);
            socket.setSoTimeout(TIMEOUT_MS);

            // Send discovery request
            String message = "ACTEST_DISCOVERY_REQUEST";
            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(
                    buffer,
                    buffer.length,
                    InetAddress.getByName("255.255.255.255"),
                    DISCOVERY_PORT);
            socket.send(packet);
            System.out.println("Sent discovery request");

            // Listen for responses
            long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < TIMEOUT_MS) {
                try {
                    byte[] responseBuffer = new byte[1024];
                    DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length);
                    socket.receive(responsePacket);

                    String response = new String(responsePacket.getData(), 0, responsePacket.getLength());
                    if (response.startsWith("ACTEST_SERVER:")) {
                        String serverName = response.substring("ACTEST_SERVER:".length());
                        String serverInfo = serverName + " (" + responsePacket.getAddress().getHostAddress() + ")";
                        if (!servers.contains(serverInfo)) {
                            servers.add(serverInfo);
                        }
                    }
                } catch (IOException e) {
                    // Timeout reached for receive, loop continues until total timeout
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }

        return servers;
    }
}

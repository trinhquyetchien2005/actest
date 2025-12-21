package com.actest.client.service;

import com.actest.client.network.Client;
import java.util.ArrayList;
import java.util.List;

public class AuthService {

    public List<String> discoverServers() {
        // Placeholder for UDP discovery logic
        // In a real implementation, this would broadcast a packet and listen for
        // responses
        List<String> servers = new ArrayList<>();
        servers.add("Server 1 (192.168.1.10)"); // Mock data for now
        return servers;
    }

    private static final Client tcpClient = new Client();

    public boolean login(String username, String password) {
        // Online login logic
        return true;
    }

    public boolean loginOffline(String serverAddress, String userName) {
        if (tcpClient.connect(serverAddress)) {
            tcpClient.sendMessage("JOIN:" + userName);
            return true;
        }
        return false;
    }

    public static Client getTcpClient() {
        return tcpClient;
    }

    public void disconnect() {
        tcpClient.close();
    }
}

package com.actest.admin.service;

import java.util.ArrayList;
import java.util.List;

public class ClientService {
    // Placeholder for client management logic
    // This will likely interact with the TCP server component

    private final List<String> connectedClients = new ArrayList<>();

    public void addClient(String clientName) {
        connectedClients.add(clientName);
    }

    public void removeClient(String clientName) {
        connectedClients.remove(clientName);
    }

    public List<String> getConnectedClients() {
        return new ArrayList<>(connectedClients);
    }
}

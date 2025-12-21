package com.actest.admin.network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Timer;
import java.util.TimerTask;

public class TimeBroadcastServer {
    private DatagramSocket socket;
    private int port = 4446;
    private final java.util.Map<Integer, Timer> timers = new java.util.concurrent.ConcurrentHashMap<>();
    private final java.util.Map<Integer, Integer> remainingTimes = new java.util.concurrent.ConcurrentHashMap<>();

    public TimeBroadcastServer() {
        try {
            socket = new DatagramSocket();
            socket.setBroadcast(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addTime(int examId, int seconds) {
        if (remainingTimes.containsKey(examId)) {
            remainingTimes.put(examId, remainingTimes.get(examId) + seconds);
        }
    }

    public void startBroadcast(int examId, int durationSeconds) {
        stopBroadcast(examId); // Ensure previous timer for this exam is stopped

        try {
            if (socket == null || socket.isClosed()) {
                socket = new DatagramSocket();
                socket.setBroadcast(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        remainingTimes.put(examId, durationSeconds);

        Timer timer = new Timer();
        timers.put(examId, timer);

        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!timers.containsKey(examId)) {
                    cancel();
                    return;
                }

                int remaining = remainingTimes.getOrDefault(examId, 0);

                if (remaining >= 0) {
                    broadcast("TIME:" + examId + ":" + remaining);
                    remainingTimes.put(examId, remaining - 1);
                } else {
                    broadcast("TIME:" + examId + ":FINISHED");
                    stopBroadcast(examId);
                }
            }
        }, 0, 1000);
    }

    public void stopBroadcast(int examId) {
        if (timers.containsKey(examId)) {
            timers.get(examId).cancel();
            timers.remove(examId);
            remainingTimes.remove(examId);
        }

        // Only close socket if no timers are running
        if (timers.isEmpty() && socket != null && !socket.isClosed()) {
            // socket.close(); // Keep socket open to avoid re-creation issues or manage
            // lifecycle better
            // Ideally, we keep it open as long as the app is running or at least one exam
            // is running.
            // For simplicity, let's keep it open.
        }
    }

    public void stopAll() {
        for (Timer t : timers.values()) {
            t.cancel();
        }
        timers.clear();
        remainingTimes.clear();
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    private void broadcast(String msg) {
        try {
            if (socket == null || socket.isClosed())
                return;
            byte[] buf = msg.getBytes();
            InetAddress address = InetAddress.getByName("255.255.255.255");
            DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

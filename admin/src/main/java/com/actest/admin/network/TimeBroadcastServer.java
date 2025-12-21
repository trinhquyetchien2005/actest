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
    private Timer timer;
    private int remainingSeconds;
    private boolean isRunning = false;

    public TimeBroadcastServer() {
        try {
            socket = new DatagramSocket();
            socket.setBroadcast(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void addTime(int seconds) {
        this.remainingSeconds += seconds;
    }

    public void startBroadcast(int durationSeconds) {
        stopBroadcast(); // Ensure previous timer is stopped
        this.remainingSeconds = durationSeconds;
        this.isRunning = true;
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (!isRunning) {
                    cancel();
                    return;
                }
                if (remainingSeconds >= 0) {
                    broadcast("TIME:" + remainingSeconds);
                    remainingSeconds--;
                } else {
                    broadcast("TIME:FINISHED");
                    stopBroadcast();
                }
            }
        }, 0, 1000);
    }

    public void stopBroadcast() {
        isRunning = false;
        if (timer != null) {
            timer.cancel();
        }
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    private void broadcast(String msg) {
        try {
            byte[] buf = msg.getBytes();
            InetAddress address = InetAddress.getByName("255.255.255.255");
            DatagramPacket packet = new DatagramPacket(buf, buf.length, address, port);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

package com.actest.client.network;

import javafx.application.Platform;
import javafx.scene.control.Label;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class TimeListener implements Runnable {
    private DatagramSocket socket;
    private int port = 4446;
    private boolean running = true;
    private Label timeLabel;

    public TimeListener(Label timeLabel) {
        this.timeLabel = timeLabel;
    }

    @Override
    public void run() {
        try {
            socket = new DatagramSocket(port, InetAddress.getByName("0.0.0.0"));
            socket.setBroadcast(true);

            byte[] buf = new byte[256];
            while (running) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                String received = new String(packet.getData(), 0, packet.getLength());

                if (received.startsWith("TIME:")) {
                    String timeStr = received.substring(5);
                    Platform.runLater(() -> {
                        if ("FINISHED".equals(timeStr)) {
                            timeLabel.setText("00:00");
                        } else {
                            try {
                                int seconds = Integer.parseInt(timeStr);
                                int mins = seconds / 60;
                                int secs = seconds % 60;
                                timeLabel.setText(String.format("%02d:%02d", mins, secs));
                            } catch (NumberFormatException e) {
                                // Ignore
                            }
                        }
                    });
                }
            }
        } catch (Exception e) {
            if (running) {
                e.printStackTrace();
            }
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }

    public void stop() {
        running = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }
}

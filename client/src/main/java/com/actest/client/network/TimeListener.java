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

    private int currentExamId;

    public TimeListener(Label timeLabel, int currentExamId) {
        this.timeLabel = timeLabel;
        this.currentExamId = currentExamId;
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
                    // Format: TIME:examId:seconds or TIME:examId:FINISHED
                    String[] parts = received.split(":");
                    if (parts.length >= 3) {
                        try {
                            int examId = Integer.parseInt(parts[1]);
                            if (examId != currentExamId) {
                                continue; // Ignore updates for other exams
                            }

                            String timeStr = parts[2];
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
                        } catch (NumberFormatException e) {
                            // Ignore malformed packets
                        }
                    } else if (parts.length == 2) {
                        // Legacy support (optional, or just ignore)
                        // If we want to support legacy admin (no examId), we might assume it's for us?
                        // But better to be strict to avoid the bug we are fixing.
                    }
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

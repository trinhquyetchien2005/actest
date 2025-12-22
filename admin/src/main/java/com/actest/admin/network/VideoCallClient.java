package com.actest.admin.network;

import com.github.sarxos.webcam.Webcam;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import com.actest.admin.config.Config;

import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class VideoCallClient {
    private static final String SERVER_HOST = Config.get("SERVER_HOST", "127.0.0.1");
    private static final int SERVER_PORT = Config.getInt("VIDEO_PORT", 9000);
    private static final int BUFFER_SIZE = 64000;

    private DatagramSocket socket;
    private InetAddress serverAddress;
    private Webcam webcam;
    private boolean isRunning = false;
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    private Consumer<Image> onRemoteFrame;
    private Consumer<Image> onLocalFrame;

    // Audio
    private TargetDataLine microphone;
    private SourceDataLine speakers;
    private static final AudioFormat AUDIO_FORMAT = new AudioFormat(8000.0f, 16, 1, true, true);

    public void setOnRemoteFrame(Consumer<Image> onRemoteFrame) {
        this.onRemoteFrame = onRemoteFrame;
    }

    public void setOnLocalFrame(Consumer<Image> onLocalFrame) {
        this.onLocalFrame = onLocalFrame;
    }

    public void start(int userId, int targetUserId) {
        try {
            socket = new DatagramSocket();
            serverAddress = InetAddress.getByName(SERVER_HOST);

            isRunning = true;

            // Register
            sendRegisterPacket(userId);

            // Start Webcam
            webcam = Webcam.getDefault();
            if (webcam != null) {
                webcam.setViewSize(new Dimension(320, 240));
                webcam.open();
                executor.submit(() -> sendFrames(userId, targetUserId));
            }

            // Start Audio
            setupAudio();
            executor.submit(() -> captureAudio(userId, targetUserId));

            // Start Receiving
            executor.submit(this::receiveData);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendRegisterPacket(int userId) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos);
            dos.writeInt(1); // REGISTER
            dos.writeInt(userId);
            byte[] data = baos.toByteArray();
            DatagramPacket packet = new DatagramPacket(data, data.length, serverAddress, SERVER_PORT);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setupAudio() {
        try {
            DataLine.Info micInfo = new DataLine.Info(TargetDataLine.class, AUDIO_FORMAT);
            if (!AudioSystem.isLineSupported(micInfo)) {
                System.err.println("Microphone not supported");
            } else {
                microphone = (TargetDataLine) AudioSystem.getLine(micInfo);
                microphone.open(AUDIO_FORMAT);
                microphone.start();
            }

            DataLine.Info speakerInfo = new DataLine.Info(SourceDataLine.class, AUDIO_FORMAT);
            if (!AudioSystem.isLineSupported(speakerInfo)) {
                System.err.println("Speakers not supported");
            } else {
                speakers = (SourceDataLine) AudioSystem.getLine(speakerInfo);
                speakers.open(AUDIO_FORMAT);
                speakers.start();
            }
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    private void captureAudio(int userId, int targetUserId) {
        byte[] audioBuffer = new byte[1024];
        while (isRunning && microphone != null && microphone.isOpen()) {
            try {
                int bytesRead = microphone.read(audioBuffer, 0, audioBuffer.length);
                if (bytesRead > 0) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    DataOutputStream dos = new DataOutputStream(baos);
                    dos.writeInt(3); // AUDIO
                    dos.writeInt(userId);
                    dos.writeInt(targetUserId);
                    dos.writeInt(bytesRead); // Length
                    dos.write(audioBuffer, 0, bytesRead); // Data

                    byte[] packetData = baos.toByteArray();
                    DatagramPacket packet = new DatagramPacket(packetData, packetData.length, serverAddress,
                            SERVER_PORT);
                    socket.send(packet);
                }

            } catch (java.net.SocketException e) {
                // Socket closed, exit loop
                break;
            } catch (IOException e) {
                if (isRunning)
                    e.printStackTrace();
            }
        }

    }

    private void sendFrames(int userId, int targetUserId) {
        while (isRunning && webcam != null && webcam.isOpen()) {
            try {
                BufferedImage image = webcam.getImage();
                if (image != null) {
                    // Update local view
                    if (onLocalFrame != null) {
                        onLocalFrame.accept(SwingFXUtils.toFXImage(image, null));
                    }

                    // Send to server
                    ByteArrayOutputStream imgBaos = new ByteArrayOutputStream();
                    ImageIO.write(image, "JPG", imgBaos);
                    byte[] imgData = imgBaos.toByteArray();

                    if (imgData.length > 60000) {
                        // Skip frames that are too large for UDP
                        System.err.println("Frame too large: " + imgData.length);
                        continue;
                    }

                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    DataOutputStream dos = new DataOutputStream(baos);
                    dos.writeInt(2); // FRAME
                    dos.writeInt(userId);
                    dos.writeInt(targetUserId);
                    dos.writeInt(imgData.length); // Length
                    dos.write(imgData); // Data

                    byte[] packetData = baos.toByteArray();
                    DatagramPacket packet = new DatagramPacket(packetData, packetData.length, serverAddress,
                            SERVER_PORT);
                    socket.send(packet);
                }
                Thread.sleep(100); // 10 FPS
            } catch (java.net.SocketException e) {
                // Socket closed, exit loop
                break;
            } catch (Exception e) {
                if (isRunning)
                    e.printStackTrace();
            }
        }
    }

    private void receiveData() {
        byte[] buffer = new byte[BUFFER_SIZE];
        while (isRunning) {
            try {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                DataInputStream in = new DataInputStream(
                        new ByteArrayInputStream(packet.getData(), 0, packet.getLength()));
                int type = in.readInt();
                in.readInt(); // senderId (unused)
                in.readInt(); // targetId (unused)
                int length = in.readInt();

                byte[] data = new byte[length];
                in.readFully(data);

                if (type == 2) { // FRAME
                    BufferedImage img = ImageIO.read(new ByteArrayInputStream(data));
                    if (img != null && onRemoteFrame != null) {
                        onRemoteFrame.accept(SwingFXUtils.toFXImage(img, null));
                    }
                } else if (type == 3) { // AUDIO
                    if (speakers != null && speakers.isOpen()) {
                        speakers.write(data, 0, length);
                    }
                }
            } catch (IOException e) {
                if (isRunning)
                    e.printStackTrace();
            }
        }
    }

    public void stop() {
        isRunning = false;
        if (webcam != null) {
            webcam.close();
        }
        if (microphone != null) {
            microphone.close();
        }
        if (speakers != null) {
            speakers.close();
        }
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
        executor.shutdownNow();
    }
}

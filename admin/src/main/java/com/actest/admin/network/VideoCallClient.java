package com.actest.admin.network;

import com.github.sarxos.webcam.Webcam;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import javax.imageio.ImageIO;

public class VideoCallClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 9000;

    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private Webcam webcam;
    private boolean isRunning = false;
    private final ExecutorService executor = Executors.newFixedThreadPool(2);

    private Consumer<Image> onRemoteFrame;
    private Consumer<Image> onLocalFrame;

    public void setOnRemoteFrame(Consumer<Image> onRemoteFrame) {
        this.onRemoteFrame = onRemoteFrame;
    }

    public void setOnLocalFrame(Consumer<Image> onLocalFrame) {
        this.onLocalFrame = onLocalFrame;
    }

    public void start(int userId, int targetUserId) {
        try {
            socket = new Socket(SERVER_HOST, SERVER_PORT);
            out = new DataOutputStream(socket.getOutputStream());
            in = new DataInputStream(socket.getInputStream());

            // Register
            out.writeInt(1); // REGISTER
            out.writeInt(userId);

            isRunning = true;

            // Start Webcam
            webcam = Webcam.getDefault();
            if (webcam != null) {
                webcam.setViewSize(new Dimension(320, 240));
                webcam.open();
                executor.submit(() -> sendFrames(targetUserId));
            }

            // Start Receiving
            executor.submit(this::receiveFrames);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendFrames(int targetUserId) {
        while (isRunning && webcam.isOpen()) {
            try {
                BufferedImage image = webcam.getImage();
                if (image != null) {
                    // Update local view
                    if (onLocalFrame != null) {
                        onLocalFrame.accept(SwingFXUtils.toFXImage(image, null));
                    }

                    // Send to server
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(image, "JPG", baos);
                    byte[] data = baos.toByteArray();

                    synchronized (out) {
                        out.writeInt(2); // FRAME
                        out.writeInt(targetUserId);
                        out.writeInt(data.length);
                        out.write(data);
                        out.flush();
                    }
                }
                Thread.sleep(100); // 10 FPS
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void receiveFrames() {
        while (isRunning) {
            try {
                int type = in.readInt();
                if (type == 2) { // FRAME
                    int fromId = in.readInt();
                    int length = in.readInt();
                    byte[] data = new byte[length];
                    in.readFully(data);

                    BufferedImage img = ImageIO.read(new java.io.ByteArrayInputStream(data));
                    if (img != null && onRemoteFrame != null) {
                        onRemoteFrame.accept(SwingFXUtils.toFXImage(img, null));
                    }
                }
            } catch (IOException e) {
                if (isRunning)
                    e.printStackTrace();
                stop();
            }
        }
    }

    public void stop() {
        isRunning = false;
        if (webcam != null) {
            webcam.close();
        }
        try {
            if (socket != null)
                socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        executor.shutdownNow();
    }
}

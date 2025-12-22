package com.actest.admin.controller;

import com.actest.admin.network.VideoCallClient;
import com.actest.admin.service.AuthService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

public class VideoCallController {

    @FXML
    private ImageView remoteVideoView;
    @FXML
    private ImageView localVideoView;

    private VideoCallClient client;
    private int targetUserId;

    public void setTargetUserId(int targetUserId) {
        this.targetUserId = targetUserId;
        startCall();
    }

    private void startCall() {
        client = new VideoCallClient();

        client.setOnLocalFrame(image -> Platform.runLater(() -> localVideoView.setImage(image)));
        client.setOnRemoteFrame(image -> Platform.runLater(() -> remoteVideoView.setImage(image)));

        int currentUserId = AuthService.getCurrentUserId();
        client.start(currentUserId, targetUserId);
    }

    @FXML
    private void handleEndCall() {
        if (client != null) {
            client.stop();
        }
        Stage stage = (Stage) remoteVideoView.getScene().getWindow();
        stage.close();
    }

    public void stop() {
        if (client != null) {
            client.stop();
        }
    }
}

package com.actest.client.controller;

import com.actest.client.service.AuthService;
import com.actest.client.util.ViewManager;
import javafx.fxml.FXML;

public class WaitingController {

    @FXML
    private void handleCancel() {
        // Close connection
        if (AuthService.getTcpClient() != null) {
            AuthService.getTcpClient().close();
        }
        // Go back to login
        ViewManager.getInstance().switchView("/com/actest/client/view/login.fxml", "ACTEST Client - Login");
    }
}

package com.actest.admin.controller;

import com.actest.admin.service.ChatService;
import com.actest.admin.service.AuthService;
import com.actest.admin.util.ViewManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ChatController {

    @FXML
    private javafx.scene.control.TextField searchField;
    @FXML
    private ListView<HBox> requestListView;
    @FXML
    private ListView<HBox> friendListView;
    @FXML
    private ListView<HBox> searchResultListView;
    @FXML
    private Label chatWithLabel;
    @FXML
    private VBox messageContainer;
    @FXML
    private ScrollPane messageScrollPane;
    @FXML
    private TextArea messageInput;

    private final ChatService chatService = new ChatService();
    private Map<String, Object> selectedUser;
    private ScheduledExecutorService poller;

    // Cache data to avoid flickering
    private List<Map<String, Object>> friendsList = new java.util.ArrayList<>();
    private List<Map<String, Object>> requestsList = new java.util.ArrayList<>();

    @FXML
    public void initialize() {
        refreshAll();

        // Start polling for messages and requests
        poller = Executors.newSingleThreadScheduledExecutor();
        poller.scheduleAtFixedRate(() -> {
            pollMessages();
            Platform.runLater(this::refreshAll);
        }, 0, 3, TimeUnit.SECONDS);
    }

    private void refreshAll() {
        loadFriends();
        loadRequests();
    }

    private void loadFriends() {
        try {
            List<Map<String, Object>> newFriends = chatService.getFriends(AuthService.getCurrentUserId());
            if (newFriends.size() != friendsList.size()) { // Simple check, can be improved
                friendsList = newFriends;
                friendListView.getItems().clear();
                for (Map<String, Object> friend : friendsList) {
                    friendListView.getItems().add(createFriendCell(friend));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private HBox createFriendCell(Map<String, Object> friend) {
        HBox cell = new HBox(10);
        cell.setAlignment(Pos.CENTER_LEFT);
        cell.setStyle(
                "-fx-padding: 10; -fx-background-color: white; -fx-background-radius: 5; -fx-border-color: #e2e8f0; -fx-border-radius: 5;");

        Label nameLabel = new Label((String) friend.get("name"));
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        Label emailLabel = new Label((String) friend.get("email"));
        emailLabel.setStyle("-fx-text-fill: grey; -fx-font-size: 12px;");

        VBox info = new VBox(2, nameLabel, emailLabel);
        HBox.setHgrow(info, javafx.scene.layout.Priority.ALWAYS);

        cell.setOnMouseClicked(e -> {
            selectedUser = friend;
            chatWithLabel.setText("Chat with " + friend.get("name"));
            loadMessages();
        });

        cell.getChildren().add(info);
        return cell;
    }

    private void loadRequests() {
        try {
            List<Map<String, Object>> newRequests = chatService.getPendingRequests(AuthService.getCurrentUserId());
            if (newRequests.size() != requestsList.size()) {
                requestsList = newRequests;
                requestListView.getItems().clear();
                for (Map<String, Object> req : requestsList) {
                    requestListView.getItems().add(createRequestCell(req));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private HBox createRequestCell(Map<String, Object> req) {
        HBox cell = new HBox(10);
        cell.setAlignment(Pos.CENTER_LEFT);
        cell.setStyle(
                "-fx-padding: 10; -fx-background-color: white; -fx-background-radius: 5; -fx-border-color: #e2e8f0; -fx-border-radius: 5;");

        Label nameLabel = new Label("From: " + req.get("senderName"));
        nameLabel.setStyle("-fx-font-weight: bold;");
        HBox.setHgrow(nameLabel, javafx.scene.layout.Priority.ALWAYS);

        Button acceptBtn = new Button("Accept");
        acceptBtn.getStyleClass().add("button-primary");
        acceptBtn.setOnAction(e -> {
            try {
                chatService.respondToRequest((int) req.get("id"), "ACCEPTED");
                refreshAll();
                showAlert("Success", "Friend request accepted!");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        cell.getChildren().addAll(nameLabel, acceptBtn);
        return cell;
    }

    @FXML
    public void handleSearch() {
        String query = searchField.getText().trim();
        // If empty, show all users (or maybe limit to 20?)

        try {
            int currentUserId = AuthService.getCurrentUserId();
            List<Map<String, Object>> foundUsers = chatService.searchUsers(query, currentUserId);

            searchResultListView.getItems().clear();
            for (Map<String, Object> user : foundUsers) {
                searchResultListView.getItems().add(createSearchUserCell(user));
            }

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Search failed: " + e.getMessage());
        }
    }

    private HBox createSearchUserCell(Map<String, Object> user) {
        HBox cell = new HBox(10);
        cell.setAlignment(Pos.CENTER_LEFT);
        cell.setStyle(
                "-fx-padding: 10; -fx-background-color: white; -fx-background-radius: 5; -fx-border-color: #e2e8f0; -fx-border-radius: 5;");

        Label nameLabel = new Label((String) user.get("name"));
        nameLabel.setStyle("-fx-font-weight: bold;");

        Label emailLabel = new Label((String) user.get("email"));
        emailLabel.setStyle("-fx-text-fill: grey; -fx-font-size: 12px;");

        VBox info = new VBox(2, nameLabel, emailLabel);
        HBox.setHgrow(info, javafx.scene.layout.Priority.ALWAYS);

        Button addBtn = new Button("Add");
        addBtn.getStyleClass().add("button-secondary");
        addBtn.setOnAction(e -> {
            try {
                if (chatService.sendRequest(AuthService.getCurrentUserId(), (int) user.get("id"))) {
                    showAlert("Success", "Friend request sent!");
                    addBtn.setDisable(true);
                    addBtn.setText("Sent");
                } else {
                    showAlert("Error", "Failed to send request.");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert("Error", "Error sending request.");
            }
        });

        cell.getChildren().addAll(info, addBtn);
        return cell;
    }

    private void pollMessages() {
        if (selectedUser != null) {
            Platform.runLater(this::loadMessages);
        }
    }

    private void loadMessages() {
        if (selectedUser == null)
            return;

        try {
            int currentUserId = AuthService.getCurrentUserId();
            int otherUserId = (int) selectedUser.get("id");

            List<Map<String, Object>> messages = chatService.getMessages(currentUserId, otherUserId);

            messageContainer.getChildren().clear();
            for (Map<String, Object> msg : messages) {
                int senderId = (int) msg.get("senderId");
                String content = (String) msg.get("content");

                // Handle timestamp
                String timeStr = "";
                if (msg.get("timestamp") != null) {
                    long timestamp = 0;
                    Object tsObj = msg.get("timestamp");
                    if (tsObj instanceof Number) {
                        timestamp = ((Number) tsObj).longValue();
                    } else if (tsObj instanceof String) {
                        // Try parsing if it's a string (unlikely with default Jackson but possible)
                        try {
                            timestamp = Long.parseLong((String) tsObj);
                        } catch (NumberFormatException e) {
                            // ignore
                        }
                    }

                    if (timestamp > 0) {
                        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm");
                        timeStr = sdf.format(new java.util.Date(timestamp));
                    }
                }

                HBox bubble = new HBox();
                VBox bubbleContent = new VBox(2);

                Label label = new Label(content);
                label.setWrapText(true);
                label.setMaxWidth(300);
                label.setStyle("-fx-padding: 10; -fx-background-radius: 10;");

                Label timeLabel = new Label(timeStr);
                timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: grey;");

                if (senderId == currentUserId) {
                    bubble.setAlignment(Pos.CENTER_RIGHT);
                    bubbleContent.setAlignment(Pos.BOTTOM_RIGHT);
                    label.setStyle(label.getStyle() + "-fx-background-color: #007bff; -fx-text-fill: white;");
                } else {
                    bubble.setAlignment(Pos.CENTER_LEFT);
                    bubbleContent.setAlignment(Pos.BOTTOM_LEFT);
                    label.setStyle(label.getStyle() + "-fx-background-color: #e9ecef; -fx-text-fill: black;");
                }

                bubbleContent.getChildren().addAll(label, timeLabel);
                bubble.getChildren().add(bubbleContent);
                messageContainer.getChildren().add(bubble);
            }

            // Scroll to bottom
            messageScrollPane.layout();
            messageScrollPane.setVvalue(1.0);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleSend() {
        if (selectedUser == null)
            return;

        String content = messageInput.getText().trim();
        if (content.isEmpty())
            return;

        try {
            int currentUserId = AuthService.getCurrentUserId();
            int otherUserId = (int) selectedUser.get("id");

            chatService.sendMessage(currentUserId, otherUserId, content);
            messageInput.clear();
            loadMessages();
        } catch (Exception e) {
            e.printStackTrace();
            if (e.getMessage().contains("403")) {
                showAlert("Error", "You are not friends with this user.");
            }
        }
    }

    @FXML
    public void handleBack() {
        if (poller != null) {
            poller.shutdown();
        }
        ViewManager.getInstance().switchView("/com/actest/admin/view/dashboard_online.fxml",
                "ACTEST Admin - Dashboard");
    }

    public void shutdown() {
        if (poller != null) {
            poller.shutdown();
        }
    }

    private void showAlert(String title, String content) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(
                javafx.scene.control.Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}

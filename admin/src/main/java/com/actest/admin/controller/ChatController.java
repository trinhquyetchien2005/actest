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

    private javafx.scene.layout.StackPane createAvatar(String name) {
        javafx.scene.layout.StackPane avatar = new javafx.scene.layout.StackPane();
        avatar.getStyleClass().add("avatar-circle");

        String initials = "";
        if (name != null && !name.isEmpty()) {
            String[] parts = name.split(" ");
            if (parts.length >= 2) {
                initials = ("" + parts[0].charAt(0) + parts[parts.length - 1].charAt(0)).toUpperCase();
            } else {
                initials = ("" + name.charAt(0)).toUpperCase();
                if (name.length() > 1) {
                    initials += name.charAt(1);
                }
            }
        }

        Label initialsLabel = new Label(initials);
        avatar.getChildren().add(initialsLabel);

        // Randomize color based on name hash
        int hash = name != null ? name.hashCode() : 0;
        String[] colors = { "#ebf8ff", "#faf5ff", "#fff5f5", "#f0fff4", "#fffff0", "#edf2f7" };
        String[] textColors = { "#2b6cb0", "#553c9a", "#c53030", "#276749", "#744210", "#4a5568" };

        int colorIndex = Math.abs(hash) % colors.length;
        avatar.setStyle("-fx-background-color: " + colors[colorIndex] + "; -fx-background-radius: 50%;");
        initialsLabel.setStyle("-fx-text-fill: " + textColors[colorIndex] + "; -fx-font-weight: bold;");

        return avatar;
    }

    private HBox createFriendCell(Map<String, Object> friend) {
        HBox cell = new HBox(15);
        cell.setAlignment(Pos.CENTER_LEFT);
        cell.getStyleClass().add("list-cell");

        String name = (String) friend.get("name");
        cell.getChildren().add(createAvatar(name));

        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2d3748;");

        Label emailLabel = new Label((String) friend.get("email"));
        emailLabel.setStyle("-fx-text-fill: #718096; -fx-font-size: 12px;");

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
        HBox cell = new HBox(15);
        cell.setAlignment(Pos.CENTER_LEFT);
        cell.getStyleClass().add("list-cell");

        String senderName = (String) req.get("senderName");
        cell.getChildren().add(createAvatar(senderName));

        Label nameLabel = new Label("Request from " + senderName);
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2d3748;");
        HBox.setHgrow(nameLabel, javafx.scene.layout.Priority.ALWAYS);

        Button acceptBtn = new Button("Accept");
        acceptBtn.getStyleClass().add("button-primary");
        acceptBtn.setStyle("-fx-font-size: 12px; -fx-padding: 5 15;");
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
        HBox cell = new HBox(15);
        cell.setAlignment(Pos.CENTER_LEFT);
        cell.getStyleClass().add("list-cell");

        String name = (String) user.get("name");
        cell.getChildren().add(createAvatar(name));

        Label nameLabel = new Label(name);
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #2d3748;");

        Label emailLabel = new Label((String) user.get("email"));
        emailLabel.setStyle("-fx-text-fill: #718096; -fx-font-size: 12px;");

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

            // Add date separators? For now just messages.

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
                VBox bubbleContent = new VBox(4);

                Label label = new Label(content);
                label.setWrapText(true);
                label.setMaxWidth(350); // Slightly wider

                Label timeLabel = new Label(timeStr);
                timeLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #a0aec0;");

                if (senderId == currentUserId) {
                    bubble.setAlignment(Pos.CENTER_RIGHT);
                    bubbleContent.setAlignment(Pos.BOTTOM_RIGHT);
                    bubbleContent.getStyleClass().add("message-bubble-sent");
                    // label style handled by css
                } else {
                    bubble.setAlignment(Pos.CENTER_LEFT);
                    bubbleContent.setAlignment(Pos.BOTTOM_LEFT);
                    bubbleContent.getStyleClass().add("message-bubble-received");
                    // label style handled by css
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

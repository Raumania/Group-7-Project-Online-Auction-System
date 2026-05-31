package auction_system.client.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import java.io.File;

public class AIViewportController {

    @FXML private TextField chatInput;
    @FXML private Label imageStatusLabel;
    @FXML private VBox messageContainer;
    @FXML private ScrollPane chatScrollPane;
    @FXML private Button sendButton;

    private File selectedFile;

    @FXML
    public void initialize() {
        messageContainer.heightProperty().addListener((observable, oldHeight, newHeight) -> {
            chatScrollPane.setVvalue(1.0);
            Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
        });
        chatInput.setOnKeyPressed(event -> {
            if (event.getCode() == javafx.scene.input.KeyCode.ENTER) {
                event.consume();
                handleSendMessage(null);
            }
        });
    }

    @FXML
    void handleAttachImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Image");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));
        File file = fileChooser.showOpenDialog(chatInput.getScene().getWindow());

        if (file != null) {
            this.selectedFile = file;
            imageStatusLabel.setText("📷 Attached: " + file.getName());
            imageStatusLabel.setVisible(true);
            imageStatusLabel.setManaged(true);
        }
    }

    @FXML
    void handleSendMessage(ActionEvent event) {
        String userText = chatInput.getText().trim();
        if (userText.isEmpty() && selectedFile == null) return;
        
        // Save selectedFile to local variable before clearing
        File fileToSend = selectedFile;
        appendUserMessage(userText, fileToSend);

        // Disable input to prevent spam
        chatInput.setDisable(true);
        if (sendButton != null) sendButton.setDisable(true);

        // Show AI thinking status (Do not use typewriter effect for this line)
        HBox loadingBox = appendAIMessage("🤖 AI is thinking...", false);

        // Call AIService to send data to Server and receive result
        auction_system.client.service.AIService.getInstance().sendChatRequest(userText, fileToSend, reply -> {
            Platform.runLater(() -> {
                // Remove thinking status bubble
                messageContainer.getChildren().remove(loadingBox);
                // Add AI response message with typewriter effect (true)
                appendAIMessage(reply, true);
                
                // Re-enable input
                chatInput.setDisable(false);
                if (sendButton != null) sendButton.setDisable(false);
                chatInput.requestFocus();
            });
        });

        // Then clear file and chatInput to allow user to send new data
        chatInput.clear();
        selectedFile = null;
        imageStatusLabel.setVisible(false);
        imageStatusLabel.setManaged(false);
    }

    private void appendUserMessage(String text, File imageFile) {
        HBox messageBox = new HBox();
        messageBox.setAlignment(Pos.CENTER_RIGHT);
        messageBox.setPadding(new Insets(5, 0, 5, 0));

        VBox bubble = new VBox(8);
        bubble.getStyleClass().add("bubble-user");
        bubble.setMaxWidth(300);

        if (imageFile != null) {
            ImageView imageView = new ImageView(new Image(imageFile.toURI().toString()));
            imageView.setFitWidth(250);
            imageView.setPreserveRatio(true);
            bubble.getChildren().add(imageView);
        }

        if (text != null && !text.isEmpty()) {
            Label content = new Label(text);
            content.getStyleClass().add("bubble-text");
            content.setWrapText(true);
            bubble.getChildren().add(content);
        }

        messageBox.getChildren().add(bubble);
        messageContainer.getChildren().add(messageBox);
    }

    private HBox appendAIMessage(String text, boolean animate) {
        HBox messageBox = new HBox();
        messageBox.setAlignment(Pos.CENTER_LEFT);
        messageBox.setPadding(new Insets(5, 0, 5, 0));

        VBox bubble = new VBox(8);
        bubble.getStyleClass().add("bubble-ai");
        bubble.setMaxWidth(300);

        Label content = new Label();
        content.getStyleClass().add("bubble-text");
        content.setWrapText(true);
        bubble.getChildren().add(content);

        messageBox.getChildren().add(bubble);
        messageContainer.getChildren().add(messageBox);

        if (animate && text != null && !text.isEmpty()) {
            animateTypewriterText(content, text);
        } else {
            content.setText(text);
        }

        return messageBox;
    }

    private void animateTypewriterText(Label label, String fullText) {
        final int length = fullText.length();
        Timeline timeline = new Timeline();

        // Reveal 1 character every 15 milliseconds
        for (int i = 0; i <= length; i++) {
            final int index = i;
            KeyFrame keyFrame = new KeyFrame(
                Duration.millis(index * 15),
                event -> {
                    label.setText(fullText.substring(0, index));
                    // Force scrollbar to always stick to bottom after layout updates with new character
                    Platform.runLater(() -> chatScrollPane.setVvalue(1.0));
                }
            );
            timeline.getKeyFrames().add(keyFrame);
        }

        timeline.play();
    }
}
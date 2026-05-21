package auction_system.client.controller;

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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import java.io.File;

public class AIViewportController {

    @FXML private TextField chatInput;
    @FXML private Label imageStatusLabel;
    @FXML private VBox messageContainer;
    @FXML private ScrollPane chatScrollPane;

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
        appendUserMessage(userText, selectedFile);
        //gọi service để xử lí message vừa mới gửi ở dưới đây


        //sau đó sẽ xóa hết file và chatInput đi để cho user gửi dữ liệu mới
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
}
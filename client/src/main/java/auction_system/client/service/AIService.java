package auction_system.client.service;

import auction_system.client.util.GsonUtil;
import auction_system.client.socket.SocketClient;
import auction_system.common.dto.AIDTO;
import auction_system.common.enums.Action;
import auction_system.common.enums.Status;
import auction_system.common.protocol.Request;
import auction_system.common.protocol.Response;
import javafx.application.Platform;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;
import java.util.function.Consumer;

public class AIService {
    private static AIService instance;

    private AIService() {}

    public static synchronized AIService getInstance() {
        if (instance == null) {
            instance = new AIService();
        }
        return instance;
    }

    public void sendChatRequest(String prompt, File imageFile, Consumer<String> onResponse) {
        new Thread(() -> {
            try {
                AIDTO aidto = new AIDTO();
                aidto.setPrompt(prompt);
                aidto.setImageBase64("");

                if (imageFile != null) {
                    try {
                        byte[] fileContent = Files.readAllBytes(imageFile.toPath());
                        String base64Image = Base64.getEncoder().encodeToString(fileContent);
                        aidto.setImageBase64(base64Image);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                Request request = new Request(
                        Action.CHAT_AI,
                        GsonUtil.getGson().toJsonTree(aidto)
                );

                // Send request via SocketClient
                SocketClient.getInstance().send(request);

                // Block and wait for response (in background thread to prevent JavaFX UI freezing)
                Response response = SocketClient.getInstance().receive();

                if (response != null && response.getStatus() == Status.SUCCESS) {
                    String reply = (String) response.getData();
                    Platform.runLater(() -> onResponse.accept(reply));
                } else {
                    String errorMsg = (response != null) ? response.getMessage() : "No response from server";
                    Platform.runLater(() -> onResponse.accept("Error: " + errorMsg));
                }
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> onResponse.accept("Error: " + e.getMessage()));
            }
        }).start();
    }
}


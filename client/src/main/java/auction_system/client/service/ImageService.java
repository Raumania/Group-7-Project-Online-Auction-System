package auction_system.client.service;

import javafx.scene.image.Image;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

public class ImageService {
    //singleton for image service
    private static ImageService instance;

    private ImageService() {}

    public static ImageService getInstance() {
        if(instance == null) {
            instance = new ImageService();
        }
        return instance;
    }

    //core in below :))
    public String fileToBase64(File file) {
        try {
            byte[] bytes = Files.readAllBytes(file.toPath());
            return Base64.getEncoder().encodeToString(bytes);
        }
        catch (IOException e) {
            e.printStackTrace();
            return "";
        }
    }

    public Image base64ToImage(String base64String) {
        if (base64String == null || base64String.trim().isEmpty()) {
            return null;
        }
        try {
            if (base64String.contains(",")) {
                base64String = base64String.split(",")[1];
            }
            byte[] imageBytes = Base64.getDecoder().decode(base64String);
            ByteArrayInputStream bis = new ByteArrayInputStream(imageBytes);
            return new Image(bis);

        } catch (IllegalArgumentException e) {
            System.err.println("Invalid Base64 string: " + e.getMessage());
            return null;
        }
    }

}

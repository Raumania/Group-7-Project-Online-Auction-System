package auction_system.server.service;

import auction_system.server.dao.CountImagesDAO;
import auction_system.server.store.ImageCounterStore;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.Base64;

public class ImageService {
    private static ImageService instance;
    private static final String IMAGE_FOLDER;

    static {
        // Get absolute path of the directory containing server's .class file
        // to ensure data/images directory is always created in the right place
        // regardless of the working directory (useful after multi-module refactor)
        String baseDir;
        try {
            Path jarPath = Path.of(
                ImageService.class.getProtectionDomain().getCodeSource().getLocation().toURI()
            );
            // Go back 1 level from target/classes or target/*.jar
            Path projectRoot = jarPath.getParent().getParent();
            baseDir = projectRoot.resolve("data/images").toString();
        } catch (URISyntaxException e) {
            // Fallback: use user.dir (current working directory)
            baseDir = System.getProperty("user.dir") + "/data/images";
        }
        IMAGE_FOLDER = baseDir;
        System.out.println("[ImageService] Image folder: " + IMAGE_FOLDER);
    }

    private ImageService(){
    }
    public static ImageService getInstance() {
        if (instance == null) {
            instance = new ImageService();
        }
        return instance;
    }

    public String getBase64Image(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return null;
        }
        try {
            // Try absolute path first
            Path path = Path.of(imagePath);
            if (!path.isAbsolute()) {
                // IMAGE_FOLDER is server/data/images, getParent() is server/data, getParent().getParent() is server
                // If it is a relative path (saved in DB as data/images/auction_X.png),
                // we need to resolve it with the server's root directory (server/projectRoot)
                path = Path.of(IMAGE_FOLDER).getParent().getParent().resolve(imagePath);
            }
            if (Files.exists(path)) {
                byte[] imageBytes = Files.readAllBytes(path);
                return Base64.getEncoder().encodeToString(imageBytes);
            } else {
                System.err.println("Image file not found: " + path.toAbsolutePath());
                return null;
            }
        } catch (IOException e) {
            System.err.println("Error reading image file: " + imagePath);
            e.printStackTrace();
            return null;
        }
    }

    //core in below
    public String saveBase64Image(Connection connection, String imageBase64) {
        try {
            byte[] imageBytes = Base64.getMimeDecoder().decode(imageBase64);

            Path folderPath = Path.of(IMAGE_FOLDER);
            Files.createDirectories(folderPath);

            String safeFileName = createSafeFileName(connection);
            Path imagePath = folderPath.resolve(safeFileName);

            System.out.println("Working directory = " + System.getProperty("user.dir"));
            System.out.println("Image absolute path = " + imagePath.toAbsolutePath());

            Files.write(imagePath, imageBytes);

            // Return relative path (data/images/auction_18.png) instead of absolute path
            // to avoid "Data too long for column 'path'" error when absolute path is too long
            return "data/images/" + safeFileName;

        } catch (Exception e) {
            throw new RuntimeException("Cannot save image", e);
        }
    }

    private String createSafeFileName(Connection connection) {
        try {
            int newId = ImageCounterStore.getInstance().getNextId();
            return "auction_" + newId + ".png";
        } catch (Exception e) {
            throw new RuntimeException("Error generating image ID from Store", e);
        }
    }

    public void deleteImage(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return;
        }
        try {
            Path path = Path.of(imagePath);
            if (!path.isAbsolute()) {
                path = Path.of(IMAGE_FOLDER).getParent().getParent().resolve(imagePath);
            }
            if (Files.exists(path)) {
                Files.delete(path);
                System.out.println("[ImageService] Deleted image file: " + path.toAbsolutePath());
            } else {
                System.err.println("[ImageService] Image file to delete not found: " + path.toAbsolutePath());
            }
        } catch (IOException e) {
            System.err.println("[ImageService] Error deleting image file: " + imagePath);
            e.printStackTrace();
        }
    }
}
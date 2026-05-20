package auction_system.server.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

public class ImageService {
    private static long  dem=0;
    private static ImageService instance;
    private static final String IMAGE_FOLDER = "data/images";
    private ImageService(){
    }
    public static ImageService getInstance() {
        if (instance == null) {
            instance = new ImageService();
        }
        return instance;
    }
    public String saveBase64Image(String imageBase64, int auctionId) {
        try {
            //validateImageData(imageBase64);
            byte[] imageBytes = Base64.getMimeDecoder().decode(imageBase64);

            Path folderPath = Path.of(IMAGE_FOLDER);
            Files.createDirectories(folderPath);

            String safeFileName = createSafeFileName(auctionId);
            Path imagePath = folderPath.resolve(safeFileName);

            System.out.println("Working directory = " + System.getProperty("user.dir"));
            System.out.println("Image absolute path = " + imagePath.toAbsolutePath());

            Files.write(imagePath, imageBytes);

            return imagePath.toString();

        } catch (Exception e) {
            throw new RuntimeException("Cannot save image", e);
        }
    }

    private void validateImageData(String imageBase64) {
        if (imageBase64 == null || imageBase64.isBlank()) {
            throw new RuntimeException("Image base64 is empty");
        }
    }

    private String removeBase64Prefix(String imageBase64) {
        imageBase64 = imageBase64.trim();

        if (imageBase64.contains(",")) {
            return imageBase64.substring(imageBase64.indexOf(",") + 1).trim();
        }

        return imageBase64;
    }

    private String createSafeFileName(int auctionId) {
        dem=dem+1;
        return "auction_" + dem + ".png";
    }
}
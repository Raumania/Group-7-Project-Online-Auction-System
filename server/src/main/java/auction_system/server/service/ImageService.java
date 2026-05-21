package auction_system.server.service;

import auction_system.server.dao.CountImagesDAO;

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
        // Lấy đường dẫn tuyệt đối của thư mục chứa file .class của server
        // để đảm bảo thư mục data/images luôn được tạo đúng chỗ
        // bất kể working directory là gì (hữu ích sau khi refactor multi-module)
        String baseDir;
        try {
            Path jarPath = Path.of(
                ImageService.class.getProtectionDomain().getCodeSource().getLocation().toURI()
            );
            // Lùi 1 cấp so với target/classes hoặc target/*.jar
            Path projectRoot = jarPath.getParent().getParent();
            baseDir = projectRoot.resolve("data/images").toString();
        } catch (URISyntaxException e) {
            // Fallback: dùng user.dir (working directory hiện tại)
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
            // Thử đường dẫn tuyệt đối trước
            Path path = Path.of(imagePath);
            if (!path.isAbsolute()) {
                // IMAGE_FOLDER là server/data/images, getParent() là server/data, getParent().getParent() là server
                // Nếu là relative path (lưu từ DB dưới dạng data/images/auction_X.png),
                // ta cần ghép với thư mục root của server (server/projectRoot)
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

            // Trả về relative path (data/images/auction_18.png) thay vì absolute path
            // để tránh lỗi "Data too long for column 'path'" khi đường dẫn tuyệt đối quá dài
            return "data/images/" + safeFileName;

        } catch (Exception e) {
            throw new RuntimeException("Cannot save image", e);
        }
    }

    private String createSafeFileName(Connection connection) {
        try {
            CountImagesDAO countImagesDAO = new CountImagesDAO();
            int newId = countImagesDAO.generateNextImageId(connection);
            return "auction_" + newId + ".png";
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi sinh ID ảnh từ Database", e);
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
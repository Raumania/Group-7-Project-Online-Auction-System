package auction_system.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Tiện ích chuyển đổi giữa các màn hình (Scene) trong JavaFX.
 * Dùng để tránh lặp lại code load FXML, set scene.
 */
public class SceneSwitcher {
    private final Stage stage;

    public SceneSwitcher(Stage stage) {
        this.stage = stage;
    }

    /**
     * Chuyển sang màn hình mới (không controller tùy chỉnh).
     * @param fxmlPath đường dẫn file FXML (bắt đầu bằng "/view/...")
     * @param title    tiêu đề cửa sổ
     */
    public void switchTo(String fxmlPath, String title) throws IOException {
        Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
        stage.setScene(new Scene(root));
        stage.setTitle(title);
        stage.show();
    }

    /**
     * Chuyển sang màn hình mới với controller được tạo bên ngoài.
     * @param fxmlPath   đường dẫn file FXML
     * @param title      tiêu đề cửa sổ
     * @param controller đối tượng controller (đã được inject dependencies)
     */
    public void switchTo(String fxmlPath, String title, Object controller) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
        loader.setController(controller);
        Parent root = loader.load();
        stage.setScene(new Scene(root));
        stage.setTitle(title);
        stage.show();
    }

    /**
     * Lấy stage hiện tại (nếu cần thao tác trực tiếp).
     */
    public Stage getStage() {
        return stage;
    }
}
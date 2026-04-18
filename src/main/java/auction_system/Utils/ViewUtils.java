package auction_system.Utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Region;

import java.io.IOException;

public class ViewUtils {
    private static final double BASE_WIDTH = 1920.0;
    private static final double BASE_HEIGHT = 1080.0;
    //Tạo scene
    public static Scene createScene(String fxmlPath, String cssPath) throws IOException {
        Parent root = FXMLLoader.load(ViewUtils.class.getResource(fxmlPath));
        Scene scene = new Scene(root,1920,1080);
        if(cssPath != null){
            scene.getStylesheets().add(ViewUtils.class.getResource(cssPath).toExternalForm());
        }
        return scene;
    }

    //Sửa layout (Ví dụ như dùng được cho máy 125%, 100% hoặc khác)
    private static void applyScale(AnchorPane root, double sceneW, double sceneH) {
        double scaleX = sceneW / BASE_WIDTH;
        double scaleY = sceneH / BASE_HEIGHT;

        root.getChildren().forEach(node -> {
            if (node instanceof Region) {
                node.setLayoutX(node.getLayoutX() == 0 ? 0 : node.getLayoutX() * scaleX);
                node.setLayoutY(node.getLayoutY() == 0 ? 0 : node.getLayoutY() * scaleY);
            }
        });
    }
    public static void scaleLayout(Scene scene) {
        AnchorPane root = (AnchorPane) scene.getRoot();
        applyScale(root, scene.getWidth(), scene.getHeight());
        scene.widthProperty().addListener((obs, oldVal, newVal) ->
                applyScale(root, newVal.doubleValue(), scene.getHeight())
        );
        scene.heightProperty().addListener((obs, oldVal, newVal) ->
                applyScale(root, scene.getWidth(), newVal.doubleValue())
        );
    }
}

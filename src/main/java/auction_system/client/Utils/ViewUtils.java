package auction_system.client.Utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.transform.Scale;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.animation.PauseTransition;

import java.io.IOException;

public class ViewUtils {

    private static final double BASE_WIDTH  = 1920.0;
    private static final double BASE_HEIGHT = 1080.0;

    public static void scaleLayout(Scene scene) {
        AnchorPane root = (AnchorPane) scene.getRoot();

        double screenW = Screen.getPrimary().getBounds().getWidth();
        double screenH = Screen.getPrimary().getBounds().getHeight();

        root.setPrefWidth(screenW);
        root.setPrefHeight(screenH);

        Scale scale = new Scale();
        scale.xProperty().bind(scene.widthProperty().divide(BASE_WIDTH));
        scale.yProperty().bind(scene.heightProperty().divide(BASE_HEIGHT));
        scale.setPivotX(0);
        scale.setPivotY(0);
        root.getTransforms().setAll(scale);

        root.prefWidthProperty().bind(scene.widthProperty().divide(scale.xProperty()));
        root.prefHeightProperty().bind(scene.heightProperty().divide(scale.yProperty()));
    }

    public static Scene createScene(FXMLLoader loader, String cssPath) throws IOException {
        Parent root = loader.load();
        Scene scene = new Scene(root, BASE_WIDTH, BASE_HEIGHT);

        if (cssPath != null && !cssPath.isBlank()) {
            String css = ViewUtils.class.getResource(cssPath).toExternalForm();
            scene.getStylesheets().add(css);
        }

        scaleLayout(scene);
        return scene;
    }

    public static void switchScene(Stage stage, String fxmlPath, String cssPath, int delayMs) {
        PauseTransition pause = new PauseTransition(Duration.millis(delayMs));
        pause.setOnFinished(e -> {
            try {
                FXMLLoader loader = new FXMLLoader(
                        ViewUtils.class.getResource(fxmlPath)
                );
                Scene scene = createScene(loader, cssPath);
                stage.setScene(scene);
                stage.setFullScreen(true);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        });
        pause.play();
    }
}
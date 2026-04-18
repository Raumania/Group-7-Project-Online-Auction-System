package auction_system;

import auction_system.Utils.ViewUtils;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {
    public void start(Stage stage) throws IOException {
        Scene scene = ViewUtils.createScene("/fxml/login.fxml","/css/login.css");
        stage.setTitle("Online Auction");
        stage.setScene(scene);
        stage.setFullScreen(true);
        stage.setFullScreenExitHint("");
        stage.show();

        //luôn phải thêm dòng này kể cả ở trong các controller để tránh bị scale
        ViewUtils.scaleLayout(scene);
    }

    public static void main(String[] args) {
        launch(args);
    }
}

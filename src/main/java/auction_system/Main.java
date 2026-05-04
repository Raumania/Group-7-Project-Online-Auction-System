package auction_system;

import auction_system.client.Utils.ViewUtils;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {
    public void start(Stage stage) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/auction_list.fxml"));
        Scene scene = new Scene(loader.load());

//        stage.setFullScreen(true);
//        stage.setFullScreenExitHint("");
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

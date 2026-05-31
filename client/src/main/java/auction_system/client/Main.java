package auction_system.client;

import auction_system.client.socket.SocketClient;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;

public class Main extends Application {
    public void start(Stage stage) throws IOException {
        SocketClient.getInstance().connect("18.143.65.69",3636);

        Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
        Scene scene = new Scene(root);

        Image icon = new Image(getClass().getResourceAsStream("/images/icon/icon.png"));
        stage.getIcons().add(icon);
        stage.setScene(scene);
        stage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}

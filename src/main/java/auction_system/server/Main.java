package auction_system.server;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class Main {
    public static void main(String[] args) {
        AuctionServer server = new AuctionServer();

        try {
            server.start();
            System.out.println("Press Enter to stop server...");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

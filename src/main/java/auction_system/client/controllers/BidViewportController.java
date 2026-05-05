package auction_system.client.controllers;

import auction_system.client.Utils.ViewSingleton;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class BidViewportController {
    @FXML
    void handleBackToDetail(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/fxml/detailViewport.fxml"));
            VBox detailViewport = loader.load();
            ViewSingleton.getInstance().getViewport().getChildren().setAll(detailViewport);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

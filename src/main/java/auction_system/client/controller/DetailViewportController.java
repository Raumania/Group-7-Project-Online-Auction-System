package auction_system.client.controller;

import auction_system.client.Util.ViewSingleton;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class DetailViewportController {

    @FXML
    void handleBackToList(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/fxml/listViewport.fxml"));
            VBox listViewport = loader.load();
            ViewSingleton.getInstance().getViewport().getChildren().setAll(listViewport);
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void handleJoinRoom(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/fxml/bidViewport.fxml"));
            VBox bidViewport = loader.load();
            ViewSingleton.getInstance().getViewport().getChildren().setAll(bidViewport);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

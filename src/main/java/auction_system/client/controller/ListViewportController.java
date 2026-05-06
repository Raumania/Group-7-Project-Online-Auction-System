package auction_system.client.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class ListViewportController implements Initializable {
    @FXML private FlowPane itemContainer;

    public void initialize(URL location, ResourceBundle resources) {
        for(int qty = 1;qty <= 10;qty++) {
            try {
                FXMLLoader loader = new FXMLLoader();
                loader.setLocation(getClass().getResource("/fxml/itemCard.fxml"));
                VBox pane = loader.load();
                ItemCardController itemCardController = loader.getController();
                itemContainer.getChildren().add(pane);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}

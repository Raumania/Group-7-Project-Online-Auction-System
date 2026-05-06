package auction_system.client.controller;

import auction_system.client.Util.ViewSingleton;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.io.IOException;


public class ItemCardController {

    @FXML
    private Label bidPrice;

    @FXML
    private Label bids;

    @FXML
    private ImageView itemImage;

    @FXML
    private Label itemName;

    @FXML
    private Label startingPrice;

    @FXML
    private Label timeLeft;

    @FXML
    public void detailViewportBtn(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/fxml/detailViewport.fxml"));
            VBox detailViewport = loader.load();
            ViewSingleton.getInstance().getViewport().getChildren().setAll(detailViewport);
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }

}

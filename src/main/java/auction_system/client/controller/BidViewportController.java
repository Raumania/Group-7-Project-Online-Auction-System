package auction_system.client.controller;

import auction_system.client.Util.ViewSingleton;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import javax.swing.*;
import java.io.IOException;

public class BidViewportController {
    @FXML
    private Label bidStatusLabel;

    @FXML
    private ToggleButton autoBidToggle;

    @FXML
    private TextField incrementField;

    @FXML
    private TextField maxBidField;
    @FXML
    private TextField bidField;

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
    @FXML
    void placeBidAction(ActionEvent event) {
        double currentBid = Double.parseDouble(bidField.getText());
        System.out.println("Request: bid " + bidField.getText());
        //service sẽ xử lí cái giá lớn bé này
        if (currentBid < 36) {
            bidStatusLabel.setText("Bid amount is too low!");
            bidStatusLabel.getStyleClass().removeAll("status-default", "status-success");
            bidStatusLabel.getStyleClass().add("status-error");
        } else {
            bidStatusLabel.setText("Bid placed successfully!");
            bidStatusLabel.getStyleClass().removeAll("status-default", "status-error");
            bidStatusLabel.getStyleClass().add("status-success");
        }
    }
    @FXML
    void switchAutoBid(ActionEvent event) {
        if(autoBidToggle.isSelected()) {
            autoBidToggle.setText("Disable Auto Bidding");
            maxBidField.setDisable(true);
            incrementField.setDisable(true);
            System.out.println("Request: Bật Auto Bid với giá Max là " + maxBidField.getText());
        }
        else {
            autoBidToggle.setText("Enable Auto Bidding");
            maxBidField.setDisable(false);
            incrementField.setDisable(false);
            System.out.println("Request: Hủy auto bid ");
        }
    }

}

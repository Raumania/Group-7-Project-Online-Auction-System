package auction_system.client.controller;

import auction_system.client.Util.ViewSingleton;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MainAuctionController implements Initializable{
    @FXML
    private StackPane viewport;
    private VBox listViewport;
    private VBox sellerViewport;

    public void initialize(URL location, ResourceBundle resources) {
        try {
            ViewSingleton.getInstance().setViewport(viewport);
            FXMLLoader listLoader = new FXMLLoader();
            FXMLLoader sellerLoader = new FXMLLoader();
            listLoader.setLocation(getClass().getResource("/fxml/listViewport.fxml"));
            sellerLoader.setLocation(getClass().getResource("/fxml/sellerViewport.fxml"));
            listViewport = listLoader.load();
            sellerViewport = sellerLoader.load();
            viewport.getChildren().setAll(listViewport);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void listViewportBtn(ActionEvent event) throws IOException{
        viewport.getChildren().setAll(listViewport);
    }
    @FXML
    void sellerViewportBtn(ActionEvent event) throws IOException{
        viewport.getChildren().setAll(sellerViewport);
    }

    @FXML
    void logoutBtn(ActionEvent event) {
        Node node = viewport;
        Stage stage = (Stage)node.getScene().getWindow();

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Logout");
        alert.setHeaderText("You're about to logout!");
        alert.setContentText("Do you want to logout?:");

        if(alert.showAndWait().get() == ButtonType.OK) {
            stage.close();
        }
    }



}

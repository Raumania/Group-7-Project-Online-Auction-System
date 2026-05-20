package auction_system.client.controller;

import auction_system.client.Util.ViewSingleton;
import auction_system.client.session.UserSession;
import auction_system.common.dto.UserDTO;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class MainAuctionController implements Initializable{
    @FXML
    private Label balanceCardLabel;

    @FXML
    private Label fullnameCardLabel;

    @FXML
    private Label fullnameHeaderLabel;

    @FXML
    private Label usernameCardLabel;

    @FXML
    private StackPane viewport;

    private VBox listViewport;
    private VBox sellerViewport;
    private VBox AIViewport;

    private ListViewportController listViewController;
    private SellerViewportController sellerViewController;

    public void initialize(URL location, ResourceBundle resources) {
        try {
            //load user's info
            UserDTO user = UserSession.getInstance().getUser();
            fullnameCardLabel.setText(user.getFullname());
            fullnameHeaderLabel.setText(user.getFullname());
            usernameCardLabel.setText(user.getUsername());
            balanceCardLabel.setText(String.valueOf(user.getBalance()));
            //load stage
            ViewSingleton.getInstance().setViewport(viewport);
            FXMLLoader listLoader = new FXMLLoader();
            FXMLLoader sellerLoader = new FXMLLoader();
            FXMLLoader AILoader = new FXMLLoader();
            listLoader.setLocation(getClass().getResource("/fxml/ListViewport.fxml"));
            sellerLoader.setLocation(getClass().getResource("/fxml/sellerViewport.fxml"));
            AILoader.setLocation(getClass().getResource("/fxml/AIViewport.fxml"));
            listViewport = listLoader.load();
            sellerViewport = sellerLoader.load();
            AIViewport = AILoader.load();

            listViewController = listLoader.getController();
            sellerViewController = sellerLoader.getController();

            viewport.getChildren().setAll(listViewport);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void listViewportBtn(ActionEvent event) throws IOException{
        if (listViewController != null) {
            listViewController.refreshList(null);
        }
        viewport.getChildren().setAll(listViewport);
    }
    @FXML
    void sellerViewportBtn(ActionEvent event) throws IOException{
        if (sellerViewController != null) {
            sellerViewController.refreshTable(null);
        }
        viewport.getChildren().setAll(sellerViewport);
    }
    @FXML
    void AIViewportBtn(ActionEvent event) {
        viewport.getChildren().setAll(AIViewport);
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
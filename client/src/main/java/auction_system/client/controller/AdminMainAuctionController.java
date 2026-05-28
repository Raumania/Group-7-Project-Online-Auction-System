package auction_system.client.controller;

import auction_system.client.util.ViewSingleton;
import auction_system.client.session.UserSession;
import auction_system.client.store.AdminUserStore;
import auction_system.client.store.AuctionStore;
import auction_system.client.store.BidTransactionStore;
import auction_system.client.store.SellerAuctionStore;
import auction_system.common.dto.UserDTO;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class AdminMainAuctionController implements Initializable {
    @FXML private Label fullnameCardLabel;
    @FXML private Label fullnameHeaderLabel;
    @FXML private Label usernameCardLabel;
    @FXML private StackPane viewport;

    private VBox userViewport;
    private VBox auctionViewport;
    private VBox AIViewport;

    private AdminUserManageController userController;
    private AdminAuctionManageController auctionController;

    public void initialize(URL location, ResourceBundle resources) {
        try {
            //load admin's info
            UserDTO user = UserSession.getInstance().getUser();
            fullnameCardLabel.setText(user.getFullname());
            fullnameHeaderLabel.setText(user.getFullname());
            usernameCardLabel.setText(user.getUsername());
            //load stage
            ViewSingleton.getInstance().setViewport(viewport);
            FXMLLoader userLoader = new FXMLLoader();
            FXMLLoader auctionLoader = new FXMLLoader();
            FXMLLoader AILoader = new FXMLLoader();
            userLoader.setLocation(getClass().getResource("/fxml/AdminUserManageViewport.fxml"));
            auctionLoader.setLocation(getClass().getResource("/fxml/AdminAuctionManageViewport.fxml"));
            AILoader.setLocation(getClass().getResource("/fxml/AIViewport.fxml"));
            userViewport = userLoader.load();
            auctionViewport = auctionLoader.load();
            AIViewport = AILoader.load();

            userController = userLoader.getController();
            auctionController = auctionLoader.getController();

            viewport.getChildren().setAll(userViewport);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    void adminAuctionManageViewportBtn(ActionEvent event)  {
        viewport.getChildren().setAll(auctionViewport);
    }

    @FXML
    void adminUserManageViewportBtn(ActionEvent event) {
        viewport.getChildren().setAll(userViewport);
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
            //Clear all sessions and data before logout
            UserSession.getInstance().logout();
            AdminUserStore.getInstance().logout();
            AuctionStore.getInstance().logout();
            BidTransactionStore.getInstance().logout();
            SellerAuctionStore.getInstance().logout();
            //change the scene to loginViewport
            try {
                FXMLLoader loader = new FXMLLoader();
                loader.setLocation(getClass().getResource("/fxml/login.fxml"));
                Parent root = loader.load();
                Scene scene = new Scene(root);
                stage.setScene(scene);
            }
            catch(IOException e) {
                e.printStackTrace();
            }

        }
    }

    @FXML
    void exitBtn(ActionEvent event) {
        Node node = viewport;
        Stage stage = (Stage)node.getScene().getWindow();

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Exit");
        alert.setHeaderText("You're about to exit!");
        alert.setContentText("Do you want to exit?:");

        if(alert.showAndWait().get() == ButtonType.OK) {
            stage.close();
        }
    }

}
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
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.text.NumberFormat;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.ResourceBundle;

public class MainAuctionController implements Initializable{
    private static MainAuctionController instance;

    public static MainAuctionController getInstance() {
        return instance;
    }

    public void refreshBalance() {
        javafx.application.Platform.runLater(() -> {
            UserDTO user = UserSession.getInstance().getUser();
            if (user != null && user.getAvailableBalance() != null && user.getFrozenBalance() != null) {
                NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("en", "US"));
                availableBalanceLabel.setText(currencyFormatter.format(user.getAvailableBalance()));
                frozenBalanceLabel.setText(currencyFormatter.format(user.getFrozenBalance()));
                
                BigDecimal total = user.getAvailableBalance().add(user.getFrozenBalance());
                totalBalanceLabel.setText(currencyFormatter.format(total));
            }
        });
    }

    @FXML private Label availableBalanceLabel;
    @FXML private Label frozenBalanceLabel;
    @FXML private Label totalBalanceLabel;
    @FXML private Label fullnameCardLabel;
    @FXML private Label fullnameHeaderLabel;
    @FXML private Label usernameCardLabel;
    @FXML private StackPane viewport;
    @FXML private TextField searchBarTextfFeld;

    private VBox listViewport;
    private VBox sellerViewport;
    private VBox AIViewport;

    private ListViewportController listViewController;
    private SellerViewportController sellerViewController;

    public void initialize(URL location, ResourceBundle resources) {
        instance = this;
        try {
            //load user's info
            UserDTO user = UserSession.getInstance().getUser();
            fullnameCardLabel.setText(user.getFullname());
            fullnameHeaderLabel.setText(user.getFullname());
            usernameCardLabel.setText(user.getUsername());
            if (user.getAvailableBalance() != null && user.getFrozenBalance() != null) {
                NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("en", "US"));
                availableBalanceLabel.setText(currencyFormatter.format(user.getAvailableBalance()));
                frozenBalanceLabel.setText(currencyFormatter.format(user.getFrozenBalance()));
                
                BigDecimal total = user.getAvailableBalance().add(user.getFrozenBalance());
                totalBalanceLabel.setText(currencyFormatter.format(total));
            } else {
                availableBalanceLabel.setText("$0.00");
                frozenBalanceLabel.setText("$0.00");
                totalBalanceLabel.setText("$0.00");
            }
            //load stage
            ViewSingleton.getInstance().setViewport(viewport);
            FXMLLoader listLoader = new FXMLLoader();
            FXMLLoader sellerLoader = new FXMLLoader();
            FXMLLoader AILoader = new FXMLLoader();
            listLoader.setLocation(getClass().getResource("/fxml/listViewport.fxml"));
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
    void searchBtn(ActionEvent event) {
        listViewController.searchBar(searchBarTextfFeld.getText());
    }

    @FXML
    void listViewportBtn(ActionEvent event) throws IOException{
        viewport.getChildren().setAll(listViewport);
    }

    /**
     * Quay về màn hình danh sách đấu giá bằng cách tái sử dụng listViewport instance đã được cache.
     * Dùng bởi DetailViewportController khi người dùng bấm "Back to List" để tránh tạo
     * ListViewportController mới (gây Listener Accumulation và Memory Leak).
     */
    public void showListViewport() {
        if (listViewport != null) {
            viewport.getChildren().setAll(listViewport);
        }
    }
    @FXML
    void sellerViewportBtn(ActionEvent event) throws IOException{
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
            //Clear all sessions and data before logout
            UserSession.getInstance().logout();
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
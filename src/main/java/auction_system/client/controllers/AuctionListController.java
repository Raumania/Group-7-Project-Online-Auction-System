package auction_system.client.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class AuctionListController implements Initializable {
    @FXML
    private TilePane itemContainer;
    @FXML
    private StackPane contentPane;
    private int column = 0;
    private int row = 1;
    @FXML
    private ImageView avartarImageView;
    @FXML
    private Label fullnameLabel;
    @FXML
    private Label moneyLabel;

    @FXML
    private Label usernameLabel;
    public void initialize(URL location, ResourceBundle resources) {
        try {
            Image avartarImage = new Image(getClass().getResource("/images/avartar/avartar.jpg").toExternalForm());
            avartarImageView.setImage(avartarImage);
            FXMLLoader detailItemLoader = new FXMLLoader(getClass().getResource("/fxml/detail-item.fxml"));
            BorderPane detailItem = detailItemLoader.load();
            contentPane.getChildren().add(detailItem);
            for(int i = 1;i <= 10;i++) {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/item-card.fxml"));
                VBox itemCard = loader.load();
                ItemController itemController = loader.getController();
                itemContainer.getChildren().add(itemCard);
                System.out.println(itemContainer.getChildren().size());
                if(column == 4) {
                    column = 0;
                    row++;
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}

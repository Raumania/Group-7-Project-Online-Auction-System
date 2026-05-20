package auction_system.client.controller;

import auction_system.client.service.AuctionListService;
import auction_system.client.store.AuctionStore;
import auction_system.common.dto.AuctionDTO;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class ListViewportController implements Initializable {
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private FlowPane itemContainer;
    @FXML private ComboBox<String> sortByComboBox;
    @FXML private ComboBox<String> statusComboBox;

    private final ObservableList<AuctionDTO> allAuctions = AuctionStore.getInstance().getAuctions();

    @FXML
    void handleFilterChange(ActionEvent event) {

    }

    public void initialize(URL location, ResourceBundle resources) {
        //Categories
        ObservableList<String> categoryOptions = javafx.collections.FXCollections.observableArrayList(
                "All Category", "Electronics", "Art","Vehicle"
        );
        categoryComboBox.setItems(categoryOptions);
        categoryComboBox.getSelectionModel().selectFirst();
        //Status
        ObservableList<String> statusOptions = javafx.collections.FXCollections.observableArrayList(
                "All Status", "OPEN", "RUNNING","FINISHED","PAID/CANCELED"
        );
        statusComboBox.setItems(statusOptions);
        statusComboBox.getSelectionModel().selectFirst();
        //Sortby
        ObservableList<String> sortByOptions = javafx.collections.FXCollections.observableArrayList(
                "Newest","Oldest"
        );
        sortByComboBox.setItems(sortByOptions);
        sortByComboBox.getSelectionModel().selectFirst();

        allAuctions.addListener((ListChangeListener<AuctionDTO>) c -> renderAuctions());
        
        AuctionListService.getInstance().fetchAllAuctions();
    }

    private void renderAuctions() {
        itemContainer.getChildren().clear();
        for (AuctionDTO auction : allAuctions) {
            try {
                FXMLLoader loader = new FXMLLoader();
                loader.setLocation(getClass().getResource("/fxml/itemCard.fxml"));
                VBox pane = loader.load();
                ItemCardController itemCardController = loader.getController();
                itemCardController.setData(auction);
                itemContainer.getChildren().add(pane);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void refreshList(ActionEvent event) {
        AuctionListService.getInstance().fetchAllAuctions();
    }
}
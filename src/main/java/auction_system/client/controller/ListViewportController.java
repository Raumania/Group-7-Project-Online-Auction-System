package auction_system.client.controller;

import javafx.collections.FXCollections;
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

    @FXML
    void handleFilterChange(ActionEvent event) {

    }
    public void initialize(URL location, ResourceBundle resources) {
        // Add item after login
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
        //Categories
        ObservableList<String> categoryOptions = FXCollections.observableArrayList(
                "All Category", "Electronics", "Art","Vehicle"
        );
        categoryComboBox.setItems(categoryOptions);
        categoryComboBox.getSelectionModel().selectFirst();
        //Status
        ObservableList<String> statusOptions = FXCollections.observableArrayList(
                "All Status", "OPEN", "RUNNING","FINISHED","PAID/CANCELED"
        );
        statusComboBox.setItems(statusOptions);
        statusComboBox.getSelectionModel().selectFirst();
        //Sortby
        ObservableList<String> sortByOptions = FXCollections.observableArrayList(
                "Newest","Oldest"
        );
        sortByComboBox.setItems(sortByOptions);
        sortByComboBox.getSelectionModel().selectFirst();
    }

}

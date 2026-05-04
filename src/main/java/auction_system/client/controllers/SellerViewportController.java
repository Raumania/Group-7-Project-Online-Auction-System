package auction_system.client.controllers;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;

public class SellerViewportController {
    @FXML
    private ComboBox<?> cbCategory;

    @FXML
    private TableColumn<?, ?> colCategory;

    @FXML
    private TableColumn<?, ?> colCurrentBid;

    @FXML
    private TableColumn<?, ?> colEndTime;

    @FXML
    private TableColumn<?, ?> colId;

    @FXML
    private TableColumn<?, ?> colName;

    @FXML
    private TableColumn<?, ?> colStartPrice;

    @FXML
    private TableColumn<?, ?> colStatus;

    @FXML
    private DatePicker dpEndTime;

    @FXML
    private TableView<?> productTable;

    @FXML
    private TextArea txtDescription;

    @FXML
    private TextField txtProductName;

    @FXML
    private TextField txtStartPrice;

    @FXML
    void handleAdd(ActionEvent event) {

    }

    @FXML
    void handleClear(ActionEvent event) {

    }

    @FXML
    void handleDelete(ActionEvent event) {

    }

    @FXML
    void handleUpdate(ActionEvent event) {

    }

    @FXML
    void refreshTable(ActionEvent event) {

    }
}

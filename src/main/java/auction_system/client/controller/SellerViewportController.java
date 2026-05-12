package auction_system.client.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.net.URL;
import java.util.ResourceBundle;

public class SellerViewportController implements Initializable {

    @FXML private ComboBox<String> categoryComboBox;
    @FXML private ComboBox<String> cbCategory;
    @FXML private ComboBox<String> sortByComboBox;
    @FXML private ComboBox<String> statusComboBox;
    @FXML private TableColumn<?, ?> colCategory;
    @FXML private TableColumn<?, ?> colCurrentBid;
    @FXML private TableColumn<?, ?> colEndTime;
    @FXML private TableColumn<?, ?> colId;
    @FXML private TableColumn<?, ?> colName;
    @FXML private TableColumn<?, ?> colStartPrice;
    @FXML private TableColumn<?, ?> colStatus;
    @FXML private DatePicker dpEndTime;
    @FXML private TableView<?> productTable;
    @FXML private TextArea txtDescription;
    @FXML private TextField txtProductName;
    @FXML private TextField txtStartPrice;
    @FXML private ImageView productImageView; // Cái ImageView để hiển thị ảnh sau khi chọn
    @FXML private VBox imageVbox;
    private File selectedImageFile; // Lưu biến này để lát nữa gửi lên Server

    public void initialize(URL url, ResourceBundle rb)  {
        //add category
        ObservableList<String> cbOptions = FXCollections.observableArrayList(
                 "Electronics", "Art","Vehicle"
        );
        cbCategory.setItems(cbOptions);
        cbCategory.getSelectionModel().selectFirst();
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

    @FXML
    void handleAdd(ActionEvent event) {

    }

    @FXML
    void handleEdit(ActionEvent event) {

    }

    @FXML
    void handleDelete(ActionEvent event) {

    }

    @FXML
    void handleFilterChange(ActionEvent event) {

    }

    @FXML
    void refreshTable(ActionEvent event) {

    }

    @FXML
    void handleSelectImage(MouseEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh sản phẩm");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );
        Stage stage = (Stage) productImageView.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            this.selectedImageFile = file;
            Image image = new Image(file.toURI().toString());
            productImageView.setImage(image);
            productImageView.setFitWidth(200);
            productImageView.setPreserveRatio(true);

            imageVbox.setVisible(false);
        }
    }

}

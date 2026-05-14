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
    @FXML private ImageView productImageView;
    @FXML private VBox imageVbox;

    private File selectedImageFile;

    @Override
    public void initialize(URL url, ResourceBundle rb)  {
        // Category cho form Add Product
        ObservableList<String> cbOptions = FXCollections.observableArrayList(
                "Electronics", "Art","Vehicle"
        );
        cbCategory.setItems(cbOptions);
        cbCategory.getSelectionModel().selectFirst();

        // Filter Categories
        ObservableList<String> categoryOptions = FXCollections.observableArrayList(
                "All Category", "Electronics", "Art","Vehicle"
        );
        categoryComboBox.setItems(categoryOptions);
        categoryComboBox.getSelectionModel().selectFirst();

        // Filter Status
        ObservableList<String> statusOptions = FXCollections.observableArrayList(
                "All Status", "OPEN", "RUNNING","FINISHED","PAID/CANCELED"
        );
        statusComboBox.setItems(statusOptions);
        statusComboBox.getSelectionModel().selectFirst();

        // Filter Sortby
        ObservableList<String> sortByOptions = FXCollections.observableArrayList(
                "Newest","Oldest"
        );
        sortByComboBox.setItems(sortByOptions);
        sortByComboBox.getSelectionModel().selectFirst();
    }

    @FXML
    void handleAdd(ActionEvent event) {
        // Logic thêm sản phẩm vào database
    }

    @FXML
    void handleEdit(ActionEvent event) {
        // Logic sửa sản phẩm
    }

    @FXML
    void handleDelete(ActionEvent event) {
        // Logic xóa sản phẩm
    }

    @FXML
    void handleFilterChange(ActionEvent event) {
        // Logic khi thay đổi bộ lọc
    }

    @FXML
    void refreshTable(ActionEvent event) {
        // Logic tải lại danh sách productTable
    }

    // HÀM MỚI ĐƯỢC THÊM VÀO ĐỂ XÓA TRẮNG FORM ADD PRODUCT
    @FXML
    void refreshAddProduct(ActionEvent event) {
        // Xóa nội dung các TextField, TextArea
        txtProductName.clear();
        txtStartPrice.clear();
        txtDescription.clear();

        // Xóa ngày tháng đã chọn
        dpEndTime.setValue(null);

        // Trả ComboBox danh mục về giá trị đầu tiên
        cbCategory.getSelectionModel().selectFirst();

        // Xóa ảnh đã chọn
        productImageView.setImage(null);

        // Bật lại hiển thị cho VBox chứa biểu tượng Upload Image
        imageVbox.setVisible(true);

        // Reset file ảnh đã lưu trong biến
        selectedImageFile = null;
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

            // Ẩn VBox biểu tượng đi khi đã có ảnh
            imageVbox.setVisible(false);
        }
    }
}
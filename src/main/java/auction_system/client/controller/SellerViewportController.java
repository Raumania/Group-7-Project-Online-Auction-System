package auction_system.client.controller;

import auction_system.client.service.AuctionManageService;
import auction_system.client.session.UserSession;
import auction_system.common.dto.AuctionDTO;
import auction_system.common.enums.ItemType;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.controlsfx.control.PropertySheet;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ResourceBundle;

public class SellerViewportController implements Initializable {

    @FXML private ComboBox<String> categoryComboBox;
    @FXML private ComboBox<String> cbCategory;
    @FXML private ComboBox<String> sortByComboBox;
    @FXML private ComboBox<String> statusComboBox;

    @FXML private TableView<?> productTable;
    @FXML private TableColumn<?, ?> colId, colName, colCategory, colStartPrice, colCurrentBid, colEndTime, colStatus;

    @FXML private DatePicker dpStartTime, dpEndTime;
    @FXML private Spinner<Integer> spinStartHour, spinStartMinute, spinEndHour, spinEndMinute;

    @FXML private VBox imageVbox;
    @FXML private ImageView productImageView;
    @FXML private TextArea txtDescription;
    @FXML private TextField txtProductName, txtStartPrice;

    private File selectedImageFile;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupComboBoxes();
        setupTimeSpinners(spinStartHour, spinStartMinute);
        setupTimeSpinners(spinEndHour, spinEndMinute);
    }

    private void setupComboBoxes() {
        cbCategory.setItems(FXCollections.observableArrayList("Electronics", "Art", "Vehicle"));
        // Đã xóa dòng cbCategory.getSelectionModel().selectFirst(); để mặc định là rỗng

        categoryComboBox.setItems(FXCollections.observableArrayList("All Category", "Electronics", "Art", "Vehicle"));
        statusComboBox.setItems(FXCollections.observableArrayList("All Status", "OPEN", "RUNNING", "FINISHED", "PAID/CANCELED"));
        sortByComboBox.setItems(FXCollections.observableArrayList("Newest", "Oldest"));

        categoryComboBox.getSelectionModel().selectFirst();
        statusComboBox.getSelectionModel().selectFirst();
        sortByComboBox.getSelectionModel().selectFirst();
    }

    private void setupTimeSpinners(Spinner<Integer> hourSpinner, Spinner<Integer> minSpinner) {
        hourSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, LocalDateTime.now().getHour()));
        minSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, LocalDateTime.now().getMinute()));
    }

    private LocalDateTime getStartDateTime() {
        LocalDate date = dpStartTime.getValue();
        return (date == null) ? null : LocalDateTime.of(date, LocalTime.of(spinStartHour.getValue(), spinStartMinute.getValue()));
    }

    private LocalDateTime getEndDateTime() {
        LocalDate date = dpEndTime.getValue();
        return (date == null) ? null : LocalDateTime.of(date, LocalTime.of(spinEndHour.getValue(), spinEndMinute.getValue()));
    }

    @FXML
    void handleAdd(ActionEvent event) {
        String name = txtProductName.getText();
        String description = txtDescription.getText();
        String type = cbCategory.getValue(); // Sẽ trả về null nếu chưa chọn gì
        int sellerId = UserSession.getInstance().getUser().getId();// sẽ lấy từ UserSession
        String startPrice = txtStartPrice.getText();
        LocalDateTime startTime = getStartDateTime();
        LocalDateTime endTime = getEndDateTime();

        // --- KIỂM TRA (VALIDATION) ---

        if (name.isEmpty()) {
            showError("Tên sản phẩm không được để trống!");
            return;
        }

        // Kiểm tra Danh mục (Category)
        if (type == null || type.trim().isEmpty()) {
            showError("Vui lòng chọn danh mục cho sản phẩm!");
            return;
        }

        if (startPrice.isEmpty()) {
            showError("Vui lòng nhập giá khởi điểm!");
            return;
        }
        try {
            double price = Double.parseDouble(startPrice);
            if (price <= 0) {
                showError("Giá khởi điểm phải lớn hơn 0!");
                return;
            }
        }
        catch (NumberFormatException e) {
            showError("Giá khởi điểm phải là một số hợp lệ!");
            return;
        }

        if (startTime == null) {
            showError("Vui lòng chọn ngày bắt đầu đấu giá!");
            return;
        }

        if (endTime == null) {
            showError("Vui lòng chọn ngày kết thúc đấu giá!");
            return;
        }

        if (!endTime.isAfter(startTime)) {
            showError("Thời gian kết thúc phải diễn ra sau thời gian bắt đầu!");
            return;
        }

        if (selectedImageFile == null) {
            showError("Vui lòng tải lên ảnh sản phẩm!");
            return;
        }
        double price = Double.parseDouble(startPrice);
        AuctionDTO auctionDTO = new AuctionDTO(name, description, ItemType.valueOf(type.toUpperCase()), sellerId, price, startTime, endTime);
        if(AuctionManageService.getInstance().createAuction(auctionDTO)) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Thông báo");
            alert.setHeaderText(null); // Để null cho giao diện gọn gàng, không bị lặp chữ
            alert.setContentText("Tạo phiên đấu giá thành công!");
            alert.showAndWait(); // Hiển thị và chờ người dùng bấm OK
        }
        else {
            showError("Có lỗi trong quá trình khởi tạo đấu giá");
        }
//        System.out.println("Dữ liệu hợp lệ! Đang chuẩn bị tạo phiên đấu giá cho: " + productName);
//        System.out.println("Danh mục: " + category);
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi nhập liệu");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    void refreshAddProduct(ActionEvent event) {
        txtProductName.clear();
        txtStartPrice.clear();
        txtDescription.clear();
        dpStartTime.setValue(null);
        dpEndTime.setValue(null);
        setupTimeSpinners(spinStartHour, spinStartMinute);
        setupTimeSpinners(spinEndHour, spinEndMinute);

        // Cập nhật lại logic xóa lựa chọn danh mục
        cbCategory.getSelectionModel().clearSelection();

        productImageView.setImage(null);
        imageVbox.setVisible(true);
        selectedImageFile = null;
    }

    @FXML
    void handleSelectImage(MouseEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh sản phẩm");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));

        Stage stage = (Stage) productImageView.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            this.selectedImageFile = file;
            productImageView.setImage(new Image(file.toURI().toString()));
            imageVbox.setVisible(false);
        }
    }

    @FXML void handleEdit(ActionEvent event) {}
    @FXML void handleDelete(ActionEvent event) {}
    @FXML void handleFilterChange(ActionEvent event) {}
    @FXML void refreshTable(ActionEvent event) {}
}
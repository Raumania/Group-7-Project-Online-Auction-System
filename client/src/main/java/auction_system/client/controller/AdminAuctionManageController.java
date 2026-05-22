package auction_system.client.controller;

import auction_system.client.service.AuctionListService;
import auction_system.client.service.AuctionManageService;
import auction_system.client.service.ImageService;
import auction_system.client.session.UserSession;
import auction_system.client.store.AuctionStore;
import auction_system.common.dto.AuctionDTO;
import auction_system.common.enums.AuctionStatus;
import auction_system.common.enums.ItemType;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

public class AdminAuctionManageController implements Initializable {

    @FXML private ComboBox<String> cbCategory;
    @FXML private TableView<AuctionDTO> productTable;
    @FXML private TableColumn<AuctionDTO, Integer> colId;
    @FXML private TableColumn<AuctionDTO, String> colName;
    @FXML private TableColumn<AuctionDTO, ItemType> colCategory;
    @FXML private TableColumn<AuctionDTO, BigDecimal> colStartPrice;
    @FXML private TableColumn<AuctionDTO, BigDecimal> colCurrentBid;
    @FXML private TableColumn<AuctionDTO, LocalDateTime> colStartTime;
    @FXML private TableColumn<AuctionDTO, LocalDateTime> colEndTime;
    @FXML private TableColumn<AuctionDTO, String> colStatus;

    @FXML private DatePicker dpStartTime, dpEndTime;
    @FXML private Spinner<Integer> spinStartHour, spinStartMinute, spinEndHour, spinEndMinute;

    @FXML private VBox imageVbox;
    @FXML private ImageView productImageView;
    @FXML private TextArea txtDescription;
    @FXML private TextField txtProductName, txtStartPrice;

    @FXML private Button btnAdd;
    @FXML private Button btnEdit;
    @FXML private Button btnCancel;
    @FXML private Button btnDelete;

    private File selectedImageFile;
    private final ObservableList<AuctionDTO> adminAuctions = AuctionStore.getInstance().getAuctions();
    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("en", "US"));

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupComboBoxes();
        setupTimeSpinners(spinStartHour, spinStartMinute);
        setupTimeSpinners(spinEndHour, spinEndMinute);
        setupPriceInput();
        setupTable();
        setupTableSelectionListener();

        // Ban đầu form trống: Cho phép Add, mờ Edit, Delete và Cancel
        btnEdit.setDisable(true);
        btnDelete.setDisable(true);
        btnCancel.setDisable(true);
        btnAdd.setDisable(false);

        refreshTable(null);
    }

    private void setupPriceInput() {
        Pattern validDoubleText = Pattern.compile("^\\d*\\.?\\d{0,2}$");
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();
            if (validDoubleText.matcher(newText).matches()) {
                return change;
            }
            return null;
        };
        txtStartPrice.setTextFormatter(new TextFormatter<>(filter));
    }

    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colCategory.setCellValueFactory(new PropertyValueFactory<>("type"));
        colStartPrice.setCellValueFactory(new PropertyValueFactory<>("startingPrice"));
        colCurrentBid.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        if (colStartTime != null) {
            colStartTime.setCellValueFactory(new PropertyValueFactory<>("startTime"));
            colStartTime.setCellFactory(column -> new TableCell<AuctionDTO, LocalDateTime>() {
                @Override
                protected void updateItem(LocalDateTime item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(formatter.format(item));
                    }
                }
            });
        }

        if (colEndTime != null) {
            colEndTime.setCellValueFactory(new PropertyValueFactory<>("endTime"));
            colEndTime.setCellFactory(column -> new TableCell<AuctionDTO, LocalDateTime>() {
                @Override
                protected void updateItem(LocalDateTime item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText(formatter.format(item));
                    }
                }
            });
        }

        colStartPrice.setCellFactory(column -> new TableCell<AuctionDTO, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(currencyFormatter.format(item));
                }
            }
        });

        colCurrentBid.setCellFactory(column -> new TableCell<AuctionDTO, BigDecimal>() {
            @Override
            protected void updateItem(BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(currencyFormatter.format(item));
                }
            }
        });

        productTable.setItems(adminAuctions);
    }

    private void setupComboBoxes() {
        cbCategory.setItems(FXCollections.observableArrayList("Electronics", "Art", "Vehicle"));
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
        String type = cbCategory.getValue();
        int sellerId = UserSession.getInstance().getUser().getId();
        String startPriceStr = txtStartPrice.getText();
        LocalDateTime startTime = getStartDateTime();
        LocalDateTime endTime = getEndDateTime();
        LocalDateTime now = LocalDateTime.now();

        if (name.isEmpty()) {
            showError("Tên sản phẩm không được để trống!");
            return;
        }

        if (type == null || type.trim().isEmpty()) {
            showError("Vui lòng chọn danh mục cho sản phẩm!");
            return;
        }

        if (startPriceStr.isEmpty()) {
            showError("Vui lòng nhập giá khởi điểm!");
            return;
        }
        BigDecimal price;
        try {
            price = new BigDecimal(startPriceStr);
            if (price.compareTo(BigDecimal.ZERO) <= 0) {
                showError("Giá khởi điểm phải lớn hơn 0!");
                return;
            }
        } catch (NumberFormatException e) {
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

        if (endTime.isBefore(now)) {
            showError("Thời gian kết thúc không được nhỏ hơn thời gian hiện tại!");
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

        AuctionDTO auctionDTO = new AuctionDTO(name, description, ItemType.valueOf(type.toUpperCase()), sellerId, price, startTime, endTime, ImageService.getInstance().fileToBase64(selectedImageFile));

        if(AuctionManageService.getInstance().createAuction(auctionDTO)) {
            AuctionStore.getInstance().addAuction(auctionDTO);
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Thông báo");
            alert.setHeaderText(null);
            alert.setContentText("Tạo phiên đấu giá thành công!");
            alert.showAndWait();
            refreshTable(null);
            refreshAddProduct(null);
        } else {
            showError("Có lỗi trong quá trình khởi tạo đấu giá");
        }
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

        cbCategory.getSelectionModel().clearSelection();

        productImageView.setImage(null);
        imageVbox.setVisible(true);
        selectedImageFile = null;

        // --- RESET TRẠNG THÁI FORM VÀ TRẠNG THÁI NÚT ---
        productTable.getSelectionModel().clearSelection(); // Bỏ highlight dòng đang chọn trên bảng
        btnAdd.setDisable(false);                          // Mở lại nút Add
        btnEdit.setDisable(true);                          // Làm mờ nút Edit
        btnDelete.setDisable(true);                        // Làm mờ nút Delete
        btnCancel.setDisable(true);                        // Làm mờ nút Cancel
    }

    @FXML
    void handleSelectImage(MouseEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn ảnh sản phẩm");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));

        Stage stage = (Stage) productImageView.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            long MAX_SIZE = 5 * 1024 * 1024; // 5MB
            if (file.length() > MAX_SIZE) {
                showError("Kích thước ảnh không được vượt quá 5MB. Vui lòng chọn ảnh khác!");
                return;
            }
            this.selectedImageFile = file;
            productImageView.setImage(new Image(file.toURI().toString()));
            imageVbox.setVisible(false);
        }
    }

    @FXML
    void handleEdit(ActionEvent event) {
        AuctionDTO selectedAuction = productTable.getSelectionModel().getSelectedItem();

        if (selectedAuction == null) {
            showError("Vui lòng chọn phiên đấu giá cần chỉnh sửa!");
            return;
        }

        if (selectedAuction.getStatus() != AuctionStatus.OPEN) {
            showError("Chỉ có thể chỉnh sửa phiên đấu giá đang ở trạng thái OPEN!");
            return;
        }

        String name = txtProductName.getText().trim();
        String description = txtDescription.getText().trim();
        String type = cbCategory.getValue();
        String startPriceStr = txtStartPrice.getText().trim();
        LocalDateTime startTime = getStartDateTime();
        LocalDateTime endTime = getEndDateTime();
        LocalDateTime now = LocalDateTime.now();

        if (name.isEmpty()) {
            showError("Tên sản phẩm không được để trống!");
            return;
        }
        if (type == null || type.trim().isEmpty()) {
            showError("Vui lòng chọn danh mục cho sản phẩm!");
            return;
        }
        if (startPriceStr.isEmpty()) {
            showError("Vui lòng nhập giá khởi điểm!");
            return;
        }
        BigDecimal price;
        try {
            price = new BigDecimal(startPriceStr);
            if (price.compareTo(BigDecimal.ZERO) <= 0) {
                showError("Giá khởi điểm phải lớn hơn 0!");
                return;
            }
        } catch (NumberFormatException e) {
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

        if (endTime.isBefore(now)) {
            showError("Thời gian kết thúc không được nhỏ hơn thời gian hiện tại!");
            return;
        }

        if (!endTime.isAfter(startTime)) {
            showError("Thời gian kết thúc phải diễn ra sau thời gian bắt đầu!");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Xác nhận chỉnh sửa");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText("Bạn có chắc chắn muốn thay đổi thông tin phiên đấu giá này không?");

        if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {

            selectedAuction.setName(name);
            selectedAuction.setDescription(description);
            selectedAuction.setType(ItemType.valueOf(type.toUpperCase()));
            selectedAuction.setStartingPrice(price);
            selectedAuction.setStartTime(startTime);
            selectedAuction.setEndTime(endTime);
            if (selectedImageFile != null) {
                selectedAuction.setImageBase64(ImageService.getInstance().fileToBase64(selectedImageFile));
            }

            boolean isEdited = AuctionManageService.getInstance().editAuction(selectedAuction);

            if (isEdited) {
                AuctionStore.getInstance().updateAuction(selectedAuction);
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Thông báo");
                alert.setHeaderText(null);
                alert.setContentText("Cập nhật thông tin phiên đấu giá thành công!");
                alert.showAndWait();

                productTable.refresh();
                refreshAddProduct(null);
            } else {
                showError("Cập nhật phiên đấu giá thất bại từ hệ thống!");
            }
        }
    }

    @FXML
    void handleDelete(ActionEvent event) {
        AuctionDTO selectedAuction = productTable.getSelectionModel().getSelectedItem();

        if (selectedAuction == null) {
            showError("Vui lòng chọn phiên đấu giá cần xóa!");
            return;
        }

        if (selectedAuction.getStatus() != AuctionStatus.OPEN) {
            showError("Chỉ có thể xóa phiên đấu giá đang ở trạng thái OPEN!");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Xác nhận xóa");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText("Bạn có chắc chắn muốn xóa phiên đấu giá \"" + selectedAuction.getName() + "\" không?");

        if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            boolean isDeleted = AuctionManageService.getInstance().deleteAuction(selectedAuction);

            if (isDeleted) {
                AuctionStore.getInstance().removeAuction(selectedAuction.getId());

                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Thông báo");
                alert.setHeaderText(null);
                alert.setContentText("Xóa phiên đấu giá thành công!");
                alert.showAndWait();

                refreshAddProduct(null);
            } else {
                showError("Xóa phiên đấu giá thất bại từ hệ thống!");
            }
        }
    }

    @FXML void handleFilterChange(ActionEvent event) {}

    @FXML
    void handleCancel(ActionEvent event) {
        AuctionDTO selectedAuction = productTable.getSelectionModel().getSelectedItem();

        if (selectedAuction == null) {
            showError("Vui lòng chọn phiên đấu giá cần hủy!");
            return;
        }

        if (selectedAuction.getStatus() != AuctionStatus.OPEN && selectedAuction.getStatus() != AuctionStatus.RUNNING) {
            showError("Chỉ có thể hủy phiên đấu giá đang ở trạng thái OPEN hoặc RUNNING!");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Xác nhận hủy phiên đấu giá");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText("Bạn có chắc chắn muốn hủy phiên đấu giá \"" + selectedAuction.getName() + "\"? Hành động này không thể hoàn tác và sẽ cấm mọi người đặt giá.");

        if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            boolean isCancelled = AuctionManageService.getInstance().cancelAuction(selectedAuction);

            if (isCancelled) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Thông báo");
                alert.setHeaderText(null);
                alert.setContentText("Hủy phiên đấu giá thành công!");
                alert.showAndWait();

                refreshAddProduct(null);
                refreshTable(null);
            } else {
                showError("Hủy phiên đấu giá thất bại từ hệ thống!");
            }
        }
    }
    
    @FXML
    void refreshTable(ActionEvent event) {
        try {
            AuctionListService.getInstance().fetchAllAuctions();
            System.out.println("Đã tải " + adminAuctions.size() + " phiên đấu giá cho Admin.");
        } catch (Exception e) {
            e.printStackTrace();
            showError("Không thể tải danh sách sản phẩm từ máy chủ!");
        }
    }

    private void setupTableSelectionListener() {
        productTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                txtProductName.setText(newSelection.getName());
                if (newSelection.getStartingPrice() != null) {
                    txtStartPrice.setText(newSelection.getStartingPrice().toPlainString());
                } else {
                    txtStartPrice.clear();
                }
                if (newSelection.getDescription() != null) {
                    txtDescription.setText(newSelection.getDescription());
                } else {
                    txtDescription.clear();
                }
                if (newSelection.getType() != null) {
                    String typeStr = newSelection.getType().name();
                    String formattedType = typeStr.substring(0, 1).toUpperCase() + typeStr.substring(1).toLowerCase();
                    cbCategory.setValue(formattedType);
                }

                if (newSelection.getStartTime() != null) {
                    dpStartTime.setValue(newSelection.getStartTime().toLocalDate());
                    spinStartHour.getValueFactory().setValue(newSelection.getStartTime().getHour());
                    spinStartMinute.getValueFactory().setValue(newSelection.getStartTime().getMinute());
                }

                if (newSelection.getEndTime() != null) {
                    dpEndTime.setValue(newSelection.getEndTime().toLocalDate());
                    spinEndHour.getValueFactory().setValue(newSelection.getEndTime().getHour());
                    spinEndMinute.getValueFactory().setValue(newSelection.getEndTime().getMinute());
                }

                // Đổ ảnh sản phẩm vào ô click to upload
                selectedImageFile = null;
                if (newSelection.getImageBase64() != null && !newSelection.getImageBase64().trim().isEmpty()) {
                    Image image = ImageService.getInstance().base64ToImage(newSelection.getImageBase64());
                    if (image != null) {
                        productImageView.setImage(image);
                        imageVbox.setVisible(false);
                    } else {
                        productImageView.setImage(null);
                        imageVbox.setVisible(true);
                    }
                } else {
                    productImageView.setImage(null);
                    imageVbox.setVisible(true);
                }

                btnAdd.setDisable(true);
                boolean isOpen = newSelection.getStatus() == AuctionStatus.OPEN;
                btnEdit.setDisable(!isOpen);
                btnDelete.setDisable(!isOpen);
                
                boolean canCancel = newSelection.getStatus() == AuctionStatus.OPEN || newSelection.getStatus() == AuctionStatus.RUNNING;
                btnCancel.setDisable(!canCancel);
            }
        });
    }
}

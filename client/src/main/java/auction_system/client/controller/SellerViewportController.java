package auction_system.client.controller;

import auction_system.client.service.AuctionManageService;
import auction_system.client.service.ImageService;
import auction_system.client.session.UserSession;
import auction_system.client.store.SellerAuctionStore;
import auction_system.common.dto.AuctionDTO;
import auction_system.common.dto.UserDTO;
import auction_system.common.enums.AuctionStatus;
import auction_system.common.enums.ItemType;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
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
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

public class SellerViewportController implements Initializable {

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
    @FXML private DatePicker dpStartTime;
    @FXML private DatePicker dpEndTime;
    @FXML private Spinner<Integer> spinStartHour;
    @FXML private Spinner<Integer> spinStartMinute;
    @FXML private Spinner<Integer> spinEndHour;
    @FXML private Spinner<Integer> spinEndMinute;
    @FXML private VBox imageVbox;
    @FXML private ImageView productImageView;
    @FXML private TextArea txtDescription;
    @FXML private TextField txtProductName;
    @FXML private TextField txtStartPrice;
    @FXML private Button btnAdd;
    @FXML private Button btnEdit;
    @FXML private Button btnDelete;

    private File selectedImageFile;
    private final ObservableList<AuctionDTO> sellerAuctions = SellerAuctionStore.getInstance().getAuctions();
    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("en", "US"));
    private FilteredList<AuctionDTO> filteredData;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupComboBoxes();
        setupTimeSpinners(spinStartHour, spinStartMinute);
        setupTimeSpinners(spinEndHour, spinEndMinute);
        setupInputValidations();
        setupTable();
        setupTableSelectionListener();

        // Initially empty form: Allow Add, disable Edit and Delete
        btnEdit.setDisable(true);
        btnDelete.setDisable(true);
        btnAdd.setDisable(false);

        refreshTable(null);
    }

    private void setupInputValidations() {
        // 1. Limit input to real numbers for starting price
        Pattern validDoubleText = Pattern.compile("^\\d*\\.?\\d{0,2}$");
        UnaryOperator<TextFormatter.Change> priceFilter = change -> {
            String newText = change.getControlNewText();
            if (validDoubleText.matcher(newText).matches()) {
                return change;
            }
            return null;
        };
        txtStartPrice.setTextFormatter(new TextFormatter<>(priceFilter));

        // 2. Limit product name length (max 100 characters)
        UnaryOperator<TextFormatter.Change> nameFilter = change -> {
            if (change.getControlNewText().length() <= 100) {
                return change;
            }
            return null;
        };
        txtProductName.setTextFormatter(new TextFormatter<>(nameFilter));

        // 3. Limit product description length (max 5000 characters)
        UnaryOperator<TextFormatter.Change> descFilter = change -> {
            if (change.getControlNewText().length() <= 5000) {
                return change;
            }
            return null;
        };
        txtDescription.setTextFormatter(new TextFormatter<>(descFilter));
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

        productTable.setItems(sellerAuctions);
        filteredData = new FilteredList<>(sellerAuctions, b -> true);

        // 2. Wrap FilteredList in SortedList to keep column sorting feature
        SortedList<AuctionDTO> sortedData = new SortedList<>(filteredData);

        // 3. Bind SortedList to your TableView
        sortedData.comparatorProperty().bind(productTable.comparatorProperty());

        // 4. Set data into table (instead of productTable.setItems(sellerAuctions);)
        productTable.setItems(sortedData);
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
            showError("Product name cannot be empty!");
            return;
        }
        if (name.length() > 100) {
            showError("Product name cannot exceed 100 characters!");
            return;
        }
        if (description.length() > 5000) {
            showError("Product description cannot exceed 5000 characters!");
            return;
        }

        if (type == null || type.trim().isEmpty()) {
            showError("Please select a category for the product!");
            return;
        }

        if (startPriceStr.isEmpty()) {
            showError("Please enter the starting price!");
            return;
        }
        BigDecimal price;
        try {
            price = new BigDecimal(startPriceStr);
            if (price.compareTo(BigDecimal.ZERO) <= 0) {
                showError("Starting price must be greater than 0!");
                return;
            }
        } catch (NumberFormatException e) {
            showError("Starting price must be a valid number!");
            return;
        }

        if (startTime == null) {
            showError("Please select the auction start date!");
            return;
        }

        if (endTime == null) {
            showError("Please select the auction end date!");
            return;
        }

        if (endTime.isBefore(now)) {
            showError("End time cannot be earlier than the current time!");
            return;
        }

        if (!endTime.isAfter(startTime)) {
            showError("End time must be after the start time!");
            return;
        }

        if (selectedImageFile == null) {
            showError("Please upload a product image!");
            return;
        }

        AuctionDTO auctionDTO = new AuctionDTO(name, description, ItemType.valueOf(type.toUpperCase()), sellerId, price, startTime, endTime, ImageService.getInstance().fileToBase64(selectedImageFile));

        // Disable button to prevent double-submit
        btnAdd.setDisable(true);

        // Perform network I/O on background thread
        // NOTE: Do not call SellerAuctionStore.addAuction() manually here.
        // Server will broadcast EVENT_NEW_AUCTION_ADDED -> SocketClient.handleNewAuctionEvent()
        // -> automatically add to both AuctionStore and SellerAuctionStore.
        // Calling manually will cause duplicate and refreshTable() causes race condition on responseQueue.
        new Thread(() -> {
            boolean success = AuctionManageService.getInstance().createAuction(auctionDTO);

            javafx.application.Platform.runLater(() -> {
                btnAdd.setDisable(false);
                if (success) {
                    // No need to add manually or call refreshTable() -
                    // broadcast from server has updated both AuctionStore and SellerAuctionStore.
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Notification");
                    alert.setHeaderText(null);
                    alert.setContentText("Auction created successfully!");
                    alert.showAndWait();
                    refreshAddProduct(null);
                } else {
                    showError("An error occurred during auction initialization");
                }
            });
        }, "seller-add-auction-worker").start();
    }
    @FXML
    void searchBar(String text){
        String keyword = (text == null) ? "" : text.trim().toLowerCase();

        filteredData.setPredicate(auction -> {
            // If search box is empty, display all data again
            if (keyword.isEmpty()) {
                return true;
            }
            String name = auction.getName();
            if (name != null && name.toLowerCase().contains(keyword)) {
                return true;
            }
            return false;
        });

    }
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Input Error");
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

        // --- RESET FORM AND BUTTON STATES ---
        productTable.getSelectionModel().clearSelection(); // Remove highlight of selected row in table
        btnAdd.setDisable(false);                          // Enable Add button
        btnEdit.setDisable(true);                          // Disable Edit button
        btnDelete.setDisable(true);                        // Disable Delete button
    }

    @FXML
    void handleSelectImage(MouseEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select product image");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg"));

        Stage stage = (Stage) productImageView.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            long MAX_SIZE = 5 * 1024 * 1024; // 5MB
            if (file.length() > MAX_SIZE) {
                showError("Image size cannot exceed 5MB. Please choose another image!");
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
            showError("Please select the auction to edit!");
            return;
        }

        if (selectedAuction.getStatus() != AuctionStatus.OPEN) {
            showError("Only auctions in OPEN status can be edited!");
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
            showError("Product name cannot be empty!");
            return;
        }
        if (name.length() > 100) {
            showError("Product name cannot exceed 100 characters!");
            return;
        }
        if (description.length() > 5000) {
            showError("Product description cannot exceed 5000 characters!");
            return;
        }
        if (type == null || type.trim().isEmpty()) {
            showError("Please select a category for the product!");
            return;
        }
        if (startPriceStr.isEmpty()) {
            showError("Please enter the starting price!");
            return;
        }
        BigDecimal price;
        try {
            price = new BigDecimal(startPriceStr);
            if (price.compareTo(BigDecimal.ZERO) <= 0) {
                showError("Starting price must be greater than 0!");
                return;
            }
        } catch (NumberFormatException e) {
            showError("Starting price must be a valid number!");
            return;
        }
        if (startTime == null) {
            showError("Please select the auction start date!");
            return;
        }
        if (endTime == null) {
            showError("Please select the auction end date!");
            return;
        }

        if (endTime.isBefore(now)) {
            showError("End time cannot be earlier than the current time!");
            return;
        }

        if (!endTime.isAfter(startTime)) {
            showError("End time must be after the start time!");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Edit");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText("Are you sure you want to change this auction's information?");

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

            // Disable button to prevent double-submit
            btnEdit.setDisable(true);

            // Perform network I/O on background thread
            AuctionDTO auctionToEdit = selectedAuction;
            new Thread(() -> {
                boolean isEdited = AuctionManageService.getInstance().editAuction(auctionToEdit);

                javafx.application.Platform.runLater(() -> {
                    btnEdit.setDisable(false);
                    if (isEdited) {
                        SellerAuctionStore.getInstance().updateAuction(auctionToEdit);
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Notification");
                        alert.setHeaderText(null);
                        alert.setContentText("Auction information updated successfully!");
                        alert.showAndWait();
                        productTable.refresh();
                        refreshAddProduct(null);
                    } else {
                        showError("Failed to update auction from system!");
                    }
                });
            }, "seller-edit-auction-worker").start();
        }
    }

    @FXML
    void handleDelete(ActionEvent event) {
        AuctionDTO selectedAuction = productTable.getSelectionModel().getSelectedItem();

        if (selectedAuction == null) {
            showError("Please select the auction to delete!");
            return;
        }

        if (selectedAuction.getStatus() != AuctionStatus.OPEN) {
            showError("Only auctions in OPEN status can be deleted!");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirm Delete");
        confirmAlert.setHeaderText(null);
        confirmAlert.setContentText("Are you sure you want to delete the auction \"" + selectedAuction.getName() + "\"?");

        if (confirmAlert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            // Disable button to prevent double-click
            btnDelete.setDisable(true);

            // Perform network I/O on background thread
            AuctionDTO auctionToDelete = selectedAuction;
            new Thread(() -> {
                boolean isDeleted = AuctionManageService.getInstance().deleteAuction(auctionToDelete);

                javafx.application.Platform.runLater(() -> {
                    btnDelete.setDisable(false);
                    if (isDeleted) {
                        SellerAuctionStore.getInstance().removeAuction(auctionToDelete.getId());

                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setTitle("Notification");
                        alert.setHeaderText(null);
                        alert.setContentText("Auction deleted successfully!");
                        alert.showAndWait();

                        refreshAddProduct(null);
                    } else {
                        showError("Failed to delete auction from system!");
                    }
                });
            }, "seller-delete-auction-worker").start();
        }
    }

    @FXML void handleFilterChange(ActionEvent event) {}
    @FXML
    void refreshTable(ActionEvent event) {
        try {
            int currentSellerId = UserSession.getInstance().getUser().getId();
            // Perform data loading on background thread
            new Thread(() -> {
                try {
                    List<AuctionDTO> auctions = AuctionManageService.getInstance().getAuctionsBySellerId(currentSellerId);
                    javafx.application.Platform.runLater(() -> {
                        SellerAuctionStore.getInstance().setAuctions(auctions);
                        System.out.println("Loaded " + sellerAuctions.size() + " auctions for Seller ID: " + currentSellerId);
                    });
                } catch (Exception e) {
                    javafx.application.Platform.runLater(() -> showError("Failed to load product list from server!"));
                }
            }, "seller-refresh-table-worker").start();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to load product list from server!");
        }
    }

    /**
     * Recalculate button states based on the actual status of the passed auction.
     * Extracted into a separate method so it can be called from multiple places (selection listener
     * and store change listener) to always ensure synchronization.
     */
    private void updateButtonStates(AuctionDTO auction) {
        if (auction == null) {
            btnAdd.setDisable(false);
            btnEdit.setDisable(true);
            btnDelete.setDisable(true);
            return;
        }
        btnAdd.setDisable(true);
        boolean isOpen = auction.getStatus() == AuctionStatus.OPEN;
        btnEdit.setDisable(!isOpen);
        btnDelete.setDisable(!isOpen);
    }

    private void setupTableSelectionListener() {
        // Listener 1: When user clicks to select a different row on the table
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

                // Fill product image into click to upload box
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

                updateButtonStates(newSelection);
            }
        });

        // Listener 2: When store is updated from server push (e.g. OPEN -> RUNNING)
        // Detect if the currently selected item has changed status -> update buttons immediately
        sellerAuctions.addListener((ListChangeListener<AuctionDTO>) change -> {
            AuctionDTO currentlySelected = productTable.getSelectionModel().getSelectedItem();
            if (currentlySelected == null) return;

            while (change.next()) {
                if (change.wasReplaced() || change.wasAdded()) {
                    for (AuctionDTO updated : change.getAddedSubList()) {
                        if (updated.getId() == currentlySelected.getId()) {
                            // The currently selected item was just updated from server -> recalculate button states
                            updateButtonStates(updated);
                            return;
                        }
                    }
                }
            }
        });
    }
}
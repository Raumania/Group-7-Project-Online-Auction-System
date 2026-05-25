package auction_system.client.controller;

import auction_system.client.Util.TimeUtil;
import auction_system.client.Util.ViewSingleton;
import auction_system.client.session.UserSession;
import auction_system.client.service.BidService;
import auction_system.client.service.ImageService;
import auction_system.client.store.AuctionStore;
import auction_system.common.dto.AuctionDTO;
import auction_system.common.dto.BidTransactionDTO;
import auction_system.common.dto.UserDTO;
import auction_system.common.enums.AuctionStatus;
import auction_system.common.enums.Status;
import auction_system.common.protocol.Response;
import javafx.collections.ListChangeListener;
import javafx.application.Platform;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.control.ToggleButton;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

public class BidViewportController implements Initializable {
    @FXML
    private Label bidStatusLabel;

    @FXML
    private ToggleButton autoBidToggle;

    @FXML
    private TextField incrementField;

    @FXML
    private TextField maxBidField;

    @FXML
    private TextField bidField;
    @FXML
    private javafx.scene.control.Button placeBidButton;

    // FXML Dynamic Components
    @FXML private ImageView productImageView;
    @FXML private Label sellerLabel;
    @FXML private Label categoryLabel;
    @FXML private Label startingPriceLabel;
    @FXML private Label minIncrementLabel;
    @FXML private Label itemNameLabel;
    @FXML private Label highestBidLabel;
    @FXML private Label highestBidderLabel;
    @FXML private Label statusLabel;
    @FXML private Label timerLabel;
    @FXML private Label minIncrementHintLabel;
    @FXML private LineChart<String, Number> biddingChart;

    // TableView Components
    @FXML private TableView<BidTransactionDTO> historyTable;
    @FXML private TableColumn<BidTransactionDTO, Integer> noColumn;
    @FXML private TableColumn<BidTransactionDTO, String> userColumn;
    @FXML private TableColumn<BidTransactionDTO, String> amountColumn;
    @FXML private TableColumn<BidTransactionDTO, String> timeColumn;

    private Timeline timeline;
    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("en", "US"));
    private AuctionDTO auctionDTO;
    private ListChangeListener<AuctionDTO> storeListener;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        setupPriceInput(bidField);
        setupPriceInput(incrementField);
        setupPriceInput(maxBidField);

        // Setup TableView Columns
        noColumn.setCellValueFactory(cellData -> new SimpleIntegerProperty(historyTable.getItems().indexOf(cellData.getValue()) + 1).asObject());
        userColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getBidder() != null ? cellData.getValue().getBidder().getUsername() : "Anonymous"
        ));
        amountColumn.setCellValueFactory(cellData -> new SimpleStringProperty(currencyFormatter.format(cellData.getValue().getAmount())));
        timeColumn.setCellValueFactory(cellData -> {
            LocalDateTime time = cellData.getValue().getBiddingtime();
            if (time != null) {
                return new SimpleStringProperty(time.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            }
            return new SimpleStringProperty("");
        });
    }

    private void setupPriceInput(TextField textField) {
        Pattern validDoubleText = Pattern.compile("^\\d*\\.?\\d{0,2}$");
        UnaryOperator<TextFormatter.Change> filter = change -> {
            String newText = change.getControlNewText();
            if (validDoubleText.matcher(newText).matches()) {
                return change;
            }
            return null;
        };
        textField.setTextFormatter(new TextFormatter<>(filter));
    }

    @FXML
    void handleBackToDetail(ActionEvent event) {
        if (storeListener != null) {
            AuctionStore.getInstance().getAuctions().removeListener(storeListener);
        }
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/fxml/detailViewport.fxml"));
            VBox detailViewport = loader.load();

            DetailViewportController controller = loader.getController();
            controller.setData(auctionDTO);

            ViewSingleton.getInstance().getViewport().getChildren().setAll(detailViewport);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setData(AuctionDTO auctionDTO) {
        this.auctionDTO = auctionDTO;

        // Listen for real-time updates to this specific auction in the store (Server Broadcasts)
        if (storeListener != null) {
            AuctionStore.getInstance().getAuctions().removeListener(storeListener);
        }
        
        storeListener = c -> {
            for (AuctionDTO updated : AuctionStore.getInstance().getAuctions()) {
                if (updated.getId() == this.auctionDTO.getId()) {
                    Platform.runLater(() -> {
                        refreshAuctionData(updated);
                    });
                    break;
                }
            }
        };
        AuctionStore.getInstance().getAuctions().addListener(storeListener);

        itemNameLabel.setText(auctionDTO.getName());
        sellerLabel.setText(String.valueOf(auctionDTO.getSellerId()));
        categoryLabel.setText(String.valueOf(auctionDTO.getType()));
        statusLabel.setText(auctionDTO.getStatus().toString());

        if (auctionDTO.getStartingPrice() != null) {
            startingPriceLabel.setText(currencyFormatter.format(auctionDTO.getStartingPrice()));
        }

        // Dynamically calculate and display minimum increment
        BigDecimal currentPrice = auctionDTO.getCurrentPrice();
        BigDecimal priceForIncrement = (currentPrice != null && currentPrice.compareTo(BigDecimal.ZERO) > 0) ? currentPrice : auctionDTO.getStartingPrice();
        BigDecimal increment = getBidIncrement(priceForIncrement);
        minIncrementLabel.setText(currencyFormatter.format(increment));
        minIncrementHintLabel.setText("Min increment: " + currencyFormatter.format(increment));

        // Display highest current bid and bidder
        if (currentPrice != null && currentPrice.compareTo(BigDecimal.ZERO) > 0) {
            highestBidLabel.setText(currencyFormatter.format(currentPrice));
            highestBidderLabel.setText(auctionDTO.getHighestBidderUsername() != null ? auctionDTO.getHighestBidderUsername() : "Anonymous");
        } else {
            highestBidLabel.setText(currencyFormatter.format(auctionDTO.getStartingPrice()));
            highestBidderLabel.setText("No bids yet");
        }

        // Handle base64 image decoding
        try {
            Image itemImage = ImageService.getInstance().base64ToImage(auctionDTO.getImageBase64());
            productImageView.setImage(itemImage);
        } catch (Exception e) {
            try {
                Image placeholder = new Image(getClass().getResourceAsStream("/images/items/placeholder.png"));
                productImageView.setImage(placeholder);
            } catch (Exception ex) {
                System.err.println("Could not load placeholder image.");
            }
        }

        // Handle countdown timer
        if (timeline != null) {
            timeline.stop();
        }

        if (auctionDTO.getStatus() == AuctionStatus.OPEN) {
            setupCountdown(auctionDTO.getStartTime(), () -> {
                statusLabel.setText(AuctionStatus.RUNNING.toString());
                auctionDTO.setStatus(AuctionStatus.RUNNING); // Update DTO
                
                // Re-evaluate inputs and enable/disable them
                Platform.runLater(this::updateBiddingUIState);
                
                setupCountdown(auctionDTO.getEndTime(), () -> {
                    timerLabel.setText("00 : 00 : 00");
                    statusLabel.setText(AuctionStatus.FINISHED.toString());
                    auctionDTO.setStatus(AuctionStatus.FINISHED); // Update DTO
                    
                    // Re-evaluate inputs and enable/disable them
                    Platform.runLater(this::updateBiddingUIState);
                });
            });
        } else if (auctionDTO.getStatus() == AuctionStatus.RUNNING) {
            setupCountdown(auctionDTO.getEndTime(), () -> {
                timerLabel.setText("00 : 00 : 00");
                statusLabel.setText(AuctionStatus.FINISHED.toString());
                auctionDTO.setStatus(AuctionStatus.FINISHED); // Update DTO
                
                // Re-evaluate inputs and enable/disable them
                Platform.runLater(this::updateBiddingUIState);
            });
        } else {
            timerLabel.setText("00 : 00 : 00");
        }

        // Load history records
        loadBidHistory();
        
        // Dynamically configure inputs/buttons state
        updateBiddingUIState();
    }

    private void refreshAuctionData(AuctionDTO updated) {
        this.auctionDTO = updated;
        
        statusLabel.setText(updated.getStatus().toString());
        
        // Update current price
        BigDecimal currentPrice = updated.getCurrentPrice();
        if (currentPrice != null && currentPrice.compareTo(BigDecimal.ZERO) > 0) {
            highestBidLabel.setText(currencyFormatter.format(currentPrice));
            highestBidderLabel.setText(updated.getHighestBidderUsername() != null ? updated.getHighestBidderUsername() : "Anonymous");
        } else {
            highestBidLabel.setText(currencyFormatter.format(updated.getStartingPrice()));
            highestBidderLabel.setText("No bids yet");
        }
        
        // Recalculate increment
        BigDecimal priceForIncrement = (currentPrice != null && currentPrice.compareTo(BigDecimal.ZERO) > 0) ? currentPrice : updated.getStartingPrice();
        BigDecimal increment = getBidIncrement(priceForIncrement);
        minIncrementLabel.setText(currencyFormatter.format(increment));
        minIncrementHintLabel.setText("Min increment: " + currencyFormatter.format(increment));
        
        // Reload history and chart
        loadBidHistory();
        
        // Update UI states
        updateBiddingUIState();
    }

    private void updateBiddingUIState() {
        UserDTO currentUser = UserSession.getInstance().getUser();
        boolean isSeller = (currentUser != null && auctionDTO.getSellerId() == currentUser.getId());
        boolean isNotRunning = (auctionDTO.getStatus() != AuctionStatus.RUNNING);

        if (isNotRunning || isSeller) {
            bidField.setDisable(true);
            maxBidField.setDisable(true);
            incrementField.setDisable(true);
            autoBidToggle.setDisable(true);
            if (placeBidButton != null) {
                placeBidButton.setDisable(true);
            }
            
            if (isSeller) {
                bidStatusLabel.setText("Sellers cannot bid on their own items.");
            } else {
                bidStatusLabel.setText("Bidding is closed.");
            }
            setStatusStyle("status-error");
        } else {
            bidField.setDisable(false);
            maxBidField.setDisable(false);
            incrementField.setDisable(false);
            autoBidToggle.setDisable(false);
            if (placeBidButton != null) {
                placeBidButton.setDisable(false);
            }
            bidStatusLabel.setText("Bidding is active!");
            setStatusStyle("status-success");
        }
    }

    private void setupCountdown(LocalDateTime targetTime, Runnable onFinished) {
        timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            Map<String, Long> remainingTime = TimeUtil.getTimeRemaining(targetTime);
            long hours = remainingTime.get("hours");
            long minutes = remainingTime.get("minutes");
            long seconds = remainingTime.get("seconds");

            if (hours <= 0 && minutes <= 0 && seconds <= 0) {
                if (timeline != null) {
                    timeline.stop();
                }
                if (onFinished != null) {
                    onFinished.run();
                }
            } else {
                timerLabel.setText(String.format("%02d : %02d : %02d", hours, minutes, seconds));
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void loadBidHistory() {
        if (auctionDTO == null) return;
        List<BidTransactionDTO> history = BidService.getInstance().getBidHistory(auctionDTO.getId());
        
        // Cập nhật biểu đồ theo thứ tự thời gian tăng dần để đường line vẽ đúng chiều
        updateChart(history);

        // Đảo ngược list để hiển thị trên bảng: bid mới nhất (cao nhất) ở trên cùng
        java.util.List<BidTransactionDTO> reversedHistory = new java.util.ArrayList<>(history);
        java.util.Collections.reverse(reversedHistory);
        
        historyTable.getItems().setAll(reversedHistory);
    }

    private void updateChart(List<BidTransactionDTO> history) {
        if (biddingChart == null) return;
        biddingChart.getData().clear();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Price (USD)");

        if (auctionDTO != null && auctionDTO.getStartingPrice() != null) {
            series.getData().add(new XYChart.Data<>("Start", auctionDTO.getStartingPrice()));
        }

        if (history != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
            for (BidTransactionDTO bid : history) {
                String timeStr = bid.getBiddingtime() != null ? bid.getBiddingtime().format(formatter) : "Unknown";
                series.getData().add(new XYChart.Data<>(timeStr, bid.getAmount()));
            }
        }

        biddingChart.getData().add(series);
    }

    private void setStatusStyle(String styleClass) {
        bidStatusLabel.getStyleClass().removeAll("status-default", "status-success", "status-error");
        bidStatusLabel.getStyleClass().add(styleClass);
    }

    @FXML
    void placeBidAction(ActionEvent event) {
        if (auctionDTO == null) {
            bidStatusLabel.setText("No auction loaded!");
            setStatusStyle("status-error");
            return;
        }

        if (auctionDTO.getStatus() != AuctionStatus.RUNNING) {
            bidStatusLabel.setText("Auction is not running!");
            setStatusStyle("status-error");
            return;
        }

        String bidText = bidField.getText();
        if (bidText.isEmpty()) {
            bidStatusLabel.setText("Bid amount cannot be empty!");
            setStatusStyle("status-error");
            return;
        }

        BigDecimal bidAmount;
        try {
            bidAmount = new BigDecimal(bidText);
        } catch (NumberFormatException e) {
            bidStatusLabel.setText("Invalid bid amount!");
            setStatusStyle("status-error");
            return;
        }

        // Validate bid amount client-side
        BigDecimal currentPrice = auctionDTO.getCurrentPrice();
        BigDecimal startingPrice = auctionDTO.getStartingPrice();
        BigDecimal priceForIncrement = (currentPrice != null && currentPrice.compareTo(BigDecimal.ZERO) > 0) ? currentPrice : startingPrice;
        BigDecimal increment = getBidIncrement(priceForIncrement);
        BigDecimal minRequiredBid = priceForIncrement;

        if (currentPrice != null && currentPrice.compareTo(BigDecimal.ZERO) > 0) {
            minRequiredBid = currentPrice.add(increment);
        }

        if (bidAmount.compareTo(minRequiredBid) < 0) {
            bidStatusLabel.setText("Bid must be at least " + currencyFormatter.format(minRequiredBid) + "!");
            setStatusStyle("status-error");
            return;
        }

        UserDTO currentUser = UserSession.getInstance().getUser();
        if (currentUser == null) {
            bidStatusLabel.setText("Please log in first!");
            setStatusStyle("status-error");
            return;
        }
        
        if (auctionDTO.getSellerId() == currentUser.getId()) {
            bidStatusLabel.setText("Sellers cannot bid on their own items!");
            setStatusStyle("status-error");
            return;
        }

        // Send request to server
        Response response = BidService.getInstance().placeBid(auctionDTO.getId(), bidAmount, currentUser.getId());

        if (response != null && response.getStatus() == Status.SUCCESS) {
            bidStatusLabel.setText("Bid placed successfully!");
            setStatusStyle("status-success");

            // Update local view model
            auctionDTO.setCurrentPrice(bidAmount);
            auctionDTO.setHighestBidderUsername(currentUser.getUsername());

            highestBidLabel.setText(currencyFormatter.format(bidAmount));
            highestBidderLabel.setText(currentUser.getUsername());

            // Recalculate minimum increment
            BigDecimal nextIncrement = getBidIncrement(bidAmount);
            minIncrementLabel.setText(currencyFormatter.format(nextIncrement));
            minIncrementHintLabel.setText("Min increment: " + currencyFormatter.format(nextIncrement));

            // Reload bid history table
            loadBidHistory();

            bidField.clear();
        } else {
            String errMsg = (response != null && response.getMessage() != null) ? response.getMessage() : "Unknown error";
            bidStatusLabel.setText("Failed: " + errMsg);
            setStatusStyle("status-error");
        }
    }

    @FXML
    void switchAutoBid(ActionEvent event) {
        if(autoBidToggle.isSelected()) {
            autoBidToggle.setText("Disable Auto Bidding");
            maxBidField.setDisable(true);
            incrementField.setDisable(true);
            String maxBidText = maxBidField.getText();
            if (!maxBidText.isEmpty()) {
                BigDecimal maxBid = new BigDecimal(maxBidText);
                System.out.println("Request: Bật Auto Bid với giá Max là " + maxBid.toPlainString());
            } else {
                System.out.println("Request: Bật Auto Bid nhưng chưa có giá Max.");
            }
        }
        else {
            autoBidToggle.setText("Enable Auto Bidding");
            maxBidField.setDisable(false);
            incrementField.setDisable(false);
            System.out.println("Request: Hủy auto bid ");
        }
    }

    public static BigDecimal getBidIncrement(BigDecimal price) {
        if (price.compareTo(new BigDecimal("1")) < 0) return new BigDecimal("0.05");
        else if (price.compareTo(new BigDecimal("5")) < 0) return new BigDecimal("0.25");
        else if (price.compareTo(new BigDecimal("25")) < 0) return new BigDecimal("0.5");
        else if (price.compareTo(new BigDecimal("100")) < 0) return new BigDecimal("1");
        else if (price.compareTo(new BigDecimal("250")) < 0) return new BigDecimal("2.5");
        else if (price.compareTo(new BigDecimal("500")) < 0) return new BigDecimal("5");
        else if (price.compareTo(new BigDecimal("1000")) < 0) return new BigDecimal("10");
        else return new BigDecimal("25");
    }
}
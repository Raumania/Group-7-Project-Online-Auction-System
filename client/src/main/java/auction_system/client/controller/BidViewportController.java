package auction_system.client.controller;

import auction_system.client.util.TimeUtil;
import auction_system.client.util.ViewSingleton;
import auction_system.client.session.UserSession;
import auction_system.client.service.BidService;
import auction_system.client.service.ImageService;
import auction_system.client.store.AuctionStore;
import auction_system.client.store.BidTransactionStore;
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
    private ListChangeListener<BidTransactionDTO> historyListener;
    private final BidService bidService = BidService.getInstance();

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

        // Chống Memory Leak: Tự động dọn dẹp khi View bị tháo khỏi Scene
        historyTable.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                if (timeline != null) {
                    timeline.stop();
                    timeline = null;
                }
                if (storeListener != null) {
                    AuctionStore.getInstance().getAuctions().removeListener(storeListener);
                }
                if (historyListener != null && auctionDTO != null) {
                    BidTransactionStore.getInstance().getHistory(auctionDTO.getId()).removeListener(historyListener);
                }
            }
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
        if (historyListener != null && auctionDTO != null) {
            BidTransactionStore.getInstance().getHistory(auctionDTO.getId()).removeListener(historyListener);
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
        // Remove previous history listener if switching auctions
        if (historyListener != null && this.auctionDTO != null) {
            BidTransactionStore.getInstance().getHistory(this.auctionDTO.getId()).removeListener(historyListener);
        }

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

        // Handle countdown timer reactively based on Server authoritative status
        // Reset the guard so the first server broadcast always re-triggers the timer
        lastCountdownStatus = auctionDTO.getStatus();
        setupCountdownForStatus();

        // Bind TableView directly to BidTransactionStore — auto-updates whenever a new bid event arrives
        historyTable.setItems(BidTransactionStore.getInstance().getHistory(auctionDTO.getId()));

        // Set up the listener to update the chart reactively on any store history changes
        historyListener = change -> {
            Platform.runLater(() -> {
                // The store stores history newest-first (descending).
                // Chart needs chronological order (ascending), so we copy and reverse it.
                List<BidTransactionDTO> currentHistory = new java.util.ArrayList<>(BidTransactionStore.getInstance().getHistory(auctionDTO.getId()));
                java.util.Collections.reverse(currentHistory);
                updateChart(currentHistory);
            });
        };
        BidTransactionStore.getInstance().getHistory(auctionDTO.getId()).addListener(historyListener);

        // Draw the initial chart from whatever is already in the store (or empty)
        List<BidTransactionDTO> initialHistory = new java.util.ArrayList<>(BidTransactionStore.getInstance().getHistory(auctionDTO.getId()));
        java.util.Collections.reverse(initialHistory);
        updateChart(initialHistory);

        // Load history from server only if the store cache is empty for this auction
        if (BidTransactionStore.getInstance().getHistory(auctionDTO.getId()).isEmpty()) {
            loadBidHistory();
        }

        // Dynamically configure inputs/buttons state
        updateBiddingUIState();

        // Fetch active AutoBid config for current user and this auction
        UserDTO currentUser = UserSession.getInstance().getUser();
        if (currentUser != null) {
            new Thread(() -> {
                try {
                    Response res = bidService.getAutoBidConfig(currentUser.getId(), auctionDTO.getId());
                    Platform.runLater(() -> {
                        if (res != null && res.getStatus() == Status.SUCCESS && res.getData() != null) {
                            try {
                                String jsonStr = auction_system.client.util.GsonUtil.toJson(res.getData());
                                auction_system.common.dto.AutoBidDTO abConfig = auction_system.client.util.GsonUtil.fromJson(jsonStr, auction_system.common.dto.AutoBidDTO.class);
                                if (abConfig != null) {
                                    autoBidToggle.setSelected(true);
                                    autoBidToggle.setText("Disable Auto Bidding");
                                    maxBidField.setText(abConfig.getMaxBid().toPlainString());
                                    incrementField.setText(abConfig.getBidIncrement().toPlainString());
                                    maxBidField.setDisable(true);
                                    incrementField.setDisable(true);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else {
                            autoBidToggle.setSelected(false);
                            autoBidToggle.setText("Enable Auto Bidding");
                            maxBidField.clear();
                            incrementField.clear();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, "fetch-autobid-config-worker").start();
        }
    }
    private AuctionStatus lastCountdownStatus = null;

    private void refreshAuctionData(AuctionDTO updated) {
        boolean statusChanged = (this.auctionDTO == null || this.auctionDTO.getStatus() != updated.getStatus());
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
        
        // Reset timer whenever the status actually changes OR the timer is stuck showing
        // the wrong label (e.g. "Starting soon..." while the auction is already RUNNING).
        // This guards against the edge case where statusChanged==false because the
        // previous Server broadcast already updated this.auctionDTO to RUNNING, but
        // setupCountdownForStatus() was never called for that transition.
        boolean timerMismatch = (updated.getStatus() == AuctionStatus.RUNNING
                && lastCountdownStatus != AuctionStatus.RUNNING);

        if (statusChanged || timerMismatch) {
            lastCountdownStatus = updated.getStatus();
            setupCountdownForStatus();
        }

        // Update UI states
        updateBiddingUIState();
    }

    private void setupCountdownForStatus() {
        if (timeline != null) {
            timeline.stop();
        }

        if (auctionDTO == null) {
            timerLabel.setText("00 : 00 : 00");
            return;
        }

        if (auctionDTO.getStatus() == AuctionStatus.OPEN) {
            setupCountdown(auctionDTO.getStartTime(), "Starting soon...");
        } else if (auctionDTO.getStatus() == AuctionStatus.RUNNING) {
            setupCountdown(auctionDTO.getEndTime(), "Ending soon...");
        } else {
            timerLabel.setText("00 : 00 : 00");
        }
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

    private void setupCountdown(LocalDateTime targetTime, String finishedText) {
        if (targetTime == null) {
            timerLabel.setText("00 : 00 : 00");
            return;
        }

        // Cập nhật giao diện lập tức để tránh delay 1 giây hiển thị chữ cũ
        Map<String, Long> initialRemaining = TimeUtil.getTimeRemaining(targetTime);
        long initialHours = initialRemaining.get("hours");
        long initialMinutes = initialRemaining.get("minutes");
        long initialSeconds = initialRemaining.get("seconds");

        if (initialHours <= 0 && initialMinutes <= 0 && initialSeconds <= 0) {
            timerLabel.setText(finishedText);
            return;
        }

        timerLabel.setText(String.format("%02d : %02d : %02d", initialHours, initialMinutes, initialSeconds));

        // Capture the reference BEFORE the lambda so each timeline only ever stops itself.
        // If we used `this.timeline` inside the lambda, a new RUNNING timeline created by
        // refreshAuctionData() could be stopped by the old OPEN timeline's final KeyFrame.
        Timeline[] selfRef = new Timeline[1];
        selfRef[0] = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            Map<String, Long> remainingTime = TimeUtil.getTimeRemaining(targetTime);
            long hours = remainingTime.get("hours");
            long minutes = remainingTime.get("minutes");
            long seconds = remainingTime.get("seconds");

            if (hours <= 0 && minutes <= 0 && seconds <= 0) {
                selfRef[0].stop();
                timerLabel.setText(finishedText);
            } else {
                timerLabel.setText(String.format("%02d : %02d : %02d", hours, minutes, seconds));
            }
        }));
        timeline = selfRef[0];
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void loadBidHistory() {
        if (auctionDTO == null) return;
        int currentAuctionId = auctionDTO.getId();
        new Thread(() -> {
            try {
                List<BidTransactionDTO> history = bidService.getBidHistory(currentAuctionId);
                // Push full list into Store (triggers historyListener to update chart + table)
                BidTransactionStore.getInstance().setHistory(currentAuctionId, history);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "load-history-worker").start();
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

        // Disable button during placement to prevent double-submitting
        placeBidButton.setDisable(true);
        bidStatusLabel.setText("Placing bid...");
        setStatusStyle("status-default");

        BigDecimal finalBidAmount = bidAmount;
        new Thread(() -> {
            try {
                // 1. Send bid request asynchronously (blocks background thread, not UI thread)
                Response response = bidService.placeBid(auctionDTO.getId(), finalBidAmount, currentUser.getId());

                Platform.runLater(() -> {
                    placeBidButton.setDisable(false); // re-enable button on UI thread
                    
                    if (response != null && response.getStatus() == Status.SUCCESS) {
                        bidStatusLabel.setText("Bid placed successfully!");
                        setStatusStyle("status-success");

                        // Update local balance BEFORE changing auctionDTO
                        BigDecimal oldPrice = auctionDTO.getCurrentPrice();
                        String oldBidder = auctionDTO.getHighestBidderUsername();
                        BigDecimal toFreeze = finalBidAmount;
                        if (oldPrice != null && oldBidder != null && oldBidder.equals(currentUser.getUsername())) {
                            toFreeze = finalBidAmount.subtract(oldPrice);
                        }
                        currentUser.setAvailableBalance(currentUser.getAvailableBalance().subtract(toFreeze));
                        currentUser.setFrozenBalance(currentUser.getFrozenBalance().add(toFreeze));
                        if (MainAuctionController.getInstance() != null) {
                            MainAuctionController.getInstance().refreshBalance();
                        }

                        // Update local view model
                        auctionDTO.setCurrentPrice(finalBidAmount);
                        auctionDTO.setHighestBidderUsername(currentUser.getUsername());

                        highestBidLabel.setText(currencyFormatter.format(finalBidAmount));
                        highestBidderLabel.setText(currentUser.getUsername());

                        // Recalculate minimum increment
                        BigDecimal nextIncrement = getBidIncrement(finalBidAmount);
                        minIncrementLabel.setText(currencyFormatter.format(nextIncrement));
                        minIncrementHintLabel.setText("Min increment: " + currencyFormatter.format(nextIncrement));

                        bidField.clear();
                    } else {
                        String errMsg = (response != null && response.getMessage() != null) ? response.getMessage() : "Unknown error";
                        bidStatusLabel.setText("Failed: " + errMsg);
                        setStatusStyle("status-error");
                    }
                });

                // 2. BidTransactionStore handles real-time updates via EVENT_BID_PLACED from Server.
                // No need to manually reload history here.
            } catch (Exception e) {
                Platform.runLater(() -> {
                    placeBidButton.setDisable(false);
                    bidStatusLabel.setText("System error: " + e.getMessage());
                    setStatusStyle("status-error");
                });
            }
        }, "bid-placement-worker").start();
    }

    @FXML
    void switchAutoBid(ActionEvent event) {
        UserDTO currentUser = UserSession.getInstance().getUser();
        if (currentUser == null) {
            bidStatusLabel.setText("Failed: Please log in first!");
            setStatusStyle("status-error");
            autoBidToggle.setSelected(false);
            return;
        }

        boolean isSelected = autoBidToggle.isSelected();

        if (isSelected) {
            String maxBidText = maxBidField.getText();
            if (maxBidText == null || maxBidText.trim().isEmpty()) {
                bidStatusLabel.setText("Failed: Please enter Max Bid!");
                setStatusStyle("status-error");
                autoBidToggle.setSelected(false);
                return;
            }

            BigDecimal maxBid;
            try {
                maxBid = new BigDecimal(maxBidText.trim());
            } catch (NumberFormatException e) {
                bidStatusLabel.setText("Failed: Invalid Max Bid amount!");
                setStatusStyle("status-error");
                autoBidToggle.setSelected(false);
                return;
            }

            BigDecimal increment = BigDecimal.ZERO;
            String incrementText = incrementField.getText();
            if (incrementText != null && !incrementText.trim().isEmpty()) {
                try {
                    increment = new BigDecimal(incrementText.trim());
                } catch (NumberFormatException e) {
                    bidStatusLabel.setText("Failed: Invalid Increment amount!");
                    setStatusStyle("status-error");
                    autoBidToggle.setSelected(false);
                    return;
                }
            }

            // Disable UI fields during request
            autoBidToggle.setDisable(true);
            maxBidField.setDisable(true);
            incrementField.setDisable(true);
            bidStatusLabel.setText("Enabling auto bidding...");
            setStatusStyle("status-default");

            BigDecimal finalIncrement = increment;
            new Thread(() -> {
                try {
                    Response response = bidService.setAutoBid(
                            currentUser.getId(),
                            auctionDTO.getId(),
                            maxBid,
                            finalIncrement
                    );

                    Platform.runLater(() -> {
                        autoBidToggle.setDisable(false);
                        if (response != null && response.getStatus() == Status.SUCCESS) {
                            autoBidToggle.setText("Disable Auto Bidding");
                            bidStatusLabel.setText("Auto bidding enabled successfully!");
                            setStatusStyle("status-success");
                            maxBidField.setDisable(true);
                            incrementField.setDisable(true);
                            
                            // Update local balance
                            BigDecimal oldPrice = auctionDTO.getCurrentPrice();
                            String oldBidder = auctionDTO.getHighestBidderUsername();
                            BigDecimal toFreeze = maxBid;
                            if (oldPrice != null && oldBidder != null && oldBidder.equals(currentUser.getUsername())) {
                                toFreeze = maxBid.subtract(oldPrice);
                            }
                            currentUser.setAvailableBalance(currentUser.getAvailableBalance().subtract(toFreeze));
                            currentUser.setFrozenBalance(currentUser.getFrozenBalance().add(toFreeze));
                            if (MainAuctionController.getInstance() != null) {
                                MainAuctionController.getInstance().refreshBalance();
                            }
                        } else {
                            String errMsg = (response != null && response.getMessage() != null) ? response.getMessage() : "Unknown error";
                            bidStatusLabel.setText("Failed: " + errMsg);
                            setStatusStyle("status-error");
                            autoBidToggle.setSelected(false);
                            maxBidField.setDisable(false);
                            incrementField.setDisable(false);
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        autoBidToggle.setDisable(false);
                        autoBidToggle.setSelected(false);
                        maxBidField.setDisable(false);
                        incrementField.setDisable(false);
                        bidStatusLabel.setText("System error: " + e.getMessage());
                        setStatusStyle("status-error");
                    });
                }
            }, "auto-bid-enable-worker").start();

        } else {
            // Disable Toggle during request
            autoBidToggle.setDisable(true);
            bidStatusLabel.setText("Disabling auto bidding...");
            setStatusStyle("status-default");

            new Thread(() -> {
                try {
                    Response response = bidService.cancelAutoBid(
                            currentUser.getId(),
                            auctionDTO.getId()
                    );

                    Platform.runLater(() -> {
                        autoBidToggle.setDisable(false);
                        if (response != null && response.getStatus() == Status.SUCCESS) {
                            autoBidToggle.setText("Enable Auto Bidding");
                            maxBidField.setDisable(false);
                            incrementField.setDisable(false);
                            bidStatusLabel.setText("Auto bidding disabled successfully.");
                            setStatusStyle("status-success");

                            // Update local balance
                            BigDecimal oldPrice = auctionDTO.getCurrentPrice();
                            String oldBidder = auctionDTO.getHighestBidderUsername();
                            BigDecimal maxBid;
                            try {
                                maxBid = new BigDecimal(maxBidField.getText().trim());
                            } catch (Exception ex) {
                                maxBid = BigDecimal.ZERO;
                            }
                            BigDecimal toUnfreeze = maxBid;
                            if (oldPrice != null && oldBidder != null && oldBidder.equals(currentUser.getUsername())) {
                                toUnfreeze = maxBid.subtract(oldPrice);
                            }
                            if (toUnfreeze.compareTo(BigDecimal.ZERO) > 0) {
                                currentUser.setAvailableBalance(currentUser.getAvailableBalance().add(toUnfreeze));
                                currentUser.setFrozenBalance(currentUser.getFrozenBalance().subtract(toUnfreeze));
                                if (MainAuctionController.getInstance() != null) {
                                    MainAuctionController.getInstance().refreshBalance();
                                }
                            }
                        } else {
                            String errMsg = (response != null && response.getMessage() != null) ? response.getMessage() : "Unknown error";
                            bidStatusLabel.setText("Failed: " + errMsg);
                            setStatusStyle("status-error");
                            autoBidToggle.setSelected(true);
                        }
                    });
                } catch (Exception e) {
                    Platform.runLater(() -> {
                        autoBidToggle.setDisable(false);
                        autoBidToggle.setSelected(true);
                        bidStatusLabel.setText("System error: " + e.getMessage());
                        setStatusStyle("status-error");
                    });
                }
            }, "auto-bid-disable-worker").start();
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
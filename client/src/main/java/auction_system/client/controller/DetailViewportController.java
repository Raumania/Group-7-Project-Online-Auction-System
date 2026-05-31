package auction_system.client.controller;

import auction_system.client.util.TimeUtil;
import auction_system.client.util.ViewSingleton;
import auction_system.client.service.ImageService;
import auction_system.common.dto.AuctionDTO;
import auction_system.common.enums.AuctionStatus;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;

public class DetailViewportController {

    @FXML private Label categoryLabel;
    @FXML private Label currentPriceLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label hourLabel;
    @FXML private ImageView itemImageView;
    @FXML private Label itemNameLabel;
    @FXML private Label minLabel;
    @FXML private Label secLabel;
    @FXML private Label sellerUsernameLabel;
    @FXML private Label startingPriceLabel;
    @FXML private Label statusLabel;

    private Timeline timeline;
    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("en", "US"));
    private AuctionDTO auctionDTO;

    /**
     * Clean up resources before switching screens:
     * Stop countdown timeline to avoid background CPU usage (Timeline Leak).
     */
    private void cleanup() {
        if (timeline != null) {
            timeline.stop();
            timeline = null;
        }
    }



    private void updateStatusLabel(Label label, AuctionStatus status) {
        label.setText(status.toString());
        label.getStyleClass().removeAll("badge-open", "badge-running", "badge-finished", "badge-paid", "badge-cancelled");
        switch (status) {
            case OPEN: label.getStyleClass().add("badge-open"); break;
            case RUNNING: label.getStyleClass().add("badge-running"); break;
            case FINISHED: label.getStyleClass().add("badge-finished"); break;
            case PAID: label.getStyleClass().add("badge-paid"); break;
            case CANCELLED: label.getStyleClass().add("badge-cancelled"); break;
        }
    }

    @FXML
    void handleBackToList(ActionEvent event) {
        // Clean up before leaving screen
        cleanup();

        // Reuse existing listViewport instance in MainAuctionController
        // DO NOT reload a new listViewport.fxml — that causes Listener Accumulation!
        if (MainAuctionController.getInstance() != null) {
            MainAuctionController.getInstance().showListViewport();
        } else {
            // Fallback: if MainAuctionController is not ready (rare case)
            try {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader();
                loader.setLocation(getClass().getResource("/fxml/listViewport.fxml"));
                VBox listViewport = loader.load();
                ViewSingleton.getInstance().getViewport().getChildren().setAll(listViewport);
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
        }
    }

    @FXML
    void handleJoinRoom(ActionEvent event) {
        // Clean up before leaving screen
        cleanup();

        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/fxml/bidViewport.fxml"));
            VBox bidViewport = loader.load();

            BidViewportController controller = loader.getController();
            controller.setData(auctionDTO);

            ViewSingleton.getInstance().getViewport().getChildren().setAll(bidViewport);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setData(AuctionDTO auctionDTO) {
        this.auctionDTO = auctionDTO;
        categoryLabel.setText(String.valueOf(auctionDTO.getType()));
        itemNameLabel.setText(auctionDTO.getName());
        descriptionLabel.setText(auctionDTO.getDescription());
        updateStatusLabel(statusLabel, auctionDTO.getStatus());
        sellerUsernameLabel.setText(String.valueOf(auctionDTO.getSellerId())); // Assuming we display Seller ID for now

        if (auctionDTO.getStartingPrice() != null) {
            startingPriceLabel.setText(currencyFormatter.format(auctionDTO.getStartingPrice()));
        }

        loadCurrentPrice(auctionDTO);

        try {
            Image itemImage = ImageService.getInstance().base64ToImage(auctionDTO.getImageBase64());
            if (itemImage != null) {
                itemImageView.setImage(itemImage);
            } else {
                Image placeholder = new Image(getClass().getResourceAsStream("/images/items/placeholder.png"));
                itemImageView.setImage(placeholder);
            }
        } catch (Exception e) {
            try {
                Image placeholder = new Image(getClass().getResourceAsStream("/images/items/placeholder.png"));
                itemImageView.setImage(placeholder);
            } catch (Exception ex) {
                System.err.println("Could not load placeholder image.");
            }
        }

        if (timeline != null) {
            timeline.stop();
        }

        // Auto-clean up Timeline when view is removed from scene graph (e.g. user clicks Sidebar navigation)
        // This prevents memory leaks and infinite background CPU usage.
        itemImageView.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                cleanup();
            }
        });

        // FIXED: Compare using enum
        if (auctionDTO.getStatus() == AuctionStatus.OPEN) {
            setupCountdown(auctionDTO.getStartTime(), () -> {
                updateStatusLabel(statusLabel, AuctionStatus.RUNNING);
                auctionDTO.setStatus(AuctionStatus.RUNNING); // Update DTO
                setupCountdown(auctionDTO.getEndTime(), () -> {
                    hourLabel.setText("00");
                    minLabel.setText("00");
                    secLabel.setText("00");
                    updateStatusLabel(statusLabel, AuctionStatus.FINISHED);
                });
            });
        } else if (auctionDTO.getStatus() == AuctionStatus.RUNNING) {
            setupCountdown(auctionDTO.getEndTime(), () -> {
                hourLabel.setText("00");
                minLabel.setText("00");
                secLabel.setText("00");
                updateStatusLabel(statusLabel, AuctionStatus.FINISHED);
            });
        } else {
            hourLabel.setText("00");
            minLabel.setText("00");
            secLabel.setText("00");
        }
    }

    private void loadCurrentPrice(AuctionDTO auctionDTO) {
        if (auctionDTO.getCurrentPrice() != null && auctionDTO.getCurrentPrice().compareTo(BigDecimal.ZERO) > 0) {
            currentPriceLabel.setText(currencyFormatter.format(auctionDTO.getCurrentPrice()));
        } else if (auctionDTO.getStartingPrice() != null) {
            currentPriceLabel.setText(currencyFormatter.format(auctionDTO.getStartingPrice()));
        } else {
             currentPriceLabel.setText("$0.00");
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
                hourLabel.setText(String.format("%02d", hours));
                minLabel.setText(String.format("%02d", minutes));
                secLabel.setText(String.format("%02d", seconds));
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }
}
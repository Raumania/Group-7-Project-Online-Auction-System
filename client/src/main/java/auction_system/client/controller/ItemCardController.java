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


public class ItemCardController {

    @FXML private Label currentPriceLabel;
    @FXML private Label highestBidderUsernameLabel;
    @FXML private Label categoryLabel;
    @FXML private Label hourLabel;
    @FXML private ImageView itemImageView;
    @FXML private Label minLabel;
    @FXML private Label itemNameLabel;
    @FXML private Label secLabel;
    @FXML private Label statusLabel;

    private Timeline timeline;
    private final NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("en", "US"));
    private AuctionDTO auctionDTO;


    @FXML
    public void detailViewportBtn(ActionEvent event) {
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
        categoryLabel.setText(String.valueOf(auctionDTO.getType()));
        itemNameLabel.setText(auctionDTO.getName());
        statusLabel.setText(auctionDTO.getStatus().toString());
        itemImageView.setImage(ImageService.getInstance().base64ToImage(auctionDTO.getImageBase64()));
        loadCurrentPrice(auctionDTO);

        if (auctionDTO.getHighestBidderUsername() != null && !auctionDTO.getHighestBidderUsername().isEmpty()) {
            highestBidderUsernameLabel.setText(auctionDTO.getHighestBidderUsername());
        } else {
            highestBidderUsernameLabel.setText("No bidder yet");
        }

        if (timeline != null) {
            timeline.stop();
        }

        // ĐÃ SỬA: So sánh bằng enum
        if (auctionDTO.getStatus() == AuctionStatus.OPEN) {
            setupCountdown(auctionDTO.getStartTime(), () -> {
                statusLabel.setText(AuctionStatus.RUNNING.toString());
                auctionDTO.setStatus(AuctionStatus.RUNNING); // Cập nhật DTO
                setupCountdown(auctionDTO.getEndTime(), () -> {
                    hourLabel.setText("00");
                    minLabel.setText("00");
                    secLabel.setText("00");
                    statusLabel.setText(AuctionStatus.FINISHED.toString());
                });
            });
        } else if (auctionDTO.getStatus() == AuctionStatus.RUNNING) {
            setupCountdown(auctionDTO.getEndTime(), () -> {
                hourLabel.setText("00");
                minLabel.setText("00");
                secLabel.setText("00");
                statusLabel.setText(AuctionStatus.FINISHED.toString());
            });
        } else {
            hourLabel.setText("00");
            minLabel.setText("00");
            secLabel.setText("00");
        }

        try {
            Image itemImage = ImageService.getInstance().base64ToImage(auctionDTO.getImageBase64());
            itemImageView.setImage(itemImage);
        } catch (Exception e) {
            try {
                Image placeholder = new Image(getClass().getResourceAsStream("/images/items/placeholder.png"));
                itemImageView.setImage(placeholder);
            } catch (Exception ex) {
                System.err.println("Could not load placeholder image.");
            }
            System.err.println("Could not load image for type: " + auctionDTO.getType());
        }
    }

    private void loadCurrentPrice(AuctionDTO auctionDTO) {
        if (auctionDTO.getCurrentPrice() != null && auctionDTO.getCurrentPrice().compareTo(BigDecimal.ZERO) > 0) {
            currentPriceLabel.setText(currencyFormatter.format(auctionDTO.getCurrentPrice()));
        } else {
            currentPriceLabel.setText(currencyFormatter.format(auctionDTO.getStartingPrice()));
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
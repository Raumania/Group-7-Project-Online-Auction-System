package auction_system.client.controller;

import auction_system.client.Util.TimeUtil;
import auction_system.client.Util.ViewSingleton;
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
import java.time.LocalDateTime;
import java.util.Map;


public class ItemCardController {

    @FXML private Label currentPriceLabel;
    @FXML
    private Label HighestBidderUsernameLabel;

    @FXML
    private Label categoryLable;

    @FXML
    private Label hourLabel;

    @FXML
    private ImageView itemImageView;

    @FXML
    private Label minLabel;

    @FXML
    private Label nameLabel;

    @FXML
    private Label secLabel;

    @FXML
    private Label statusLabel;

    private Timeline timeline;


    @FXML
    public void detailViewportBtn(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/fxml/detailViewport.fxml"));
            VBox detailViewport = loader.load();
            ViewSingleton.getInstance().getViewport().getChildren().setAll(detailViewport);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setData(AuctionDTO auctionDTO) {
        categoryLable.setText(String.valueOf(auctionDTO.getType()));
        nameLabel.setText(auctionDTO.getName());
        statusLabel.setText(auctionDTO.getStatus().toString());
        itemImageView.setImage(ImageService.getInstance().base64ToImage(auctionDTO.getImageBase64()));
        loadCurrentPrice(auctionDTO);

        if (auctionDTO.getHighestBidderUsername() != null && !auctionDTO.getHighestBidderUsername().isEmpty()) {
            HighestBidderUsernameLabel.setText(auctionDTO.getHighestBidderUsername());
        } else {
            HighestBidderUsernameLabel.setText("No bidder yet");
        }

        if (timeline != null) {
            timeline.stop();
        }

        if (auctionDTO.getStatus() == AuctionStatus.RUNNING) {
            setupCountdown(auctionDTO.getStartTime(), () -> {
                hourLabel.setText("00");
                minLabel.setText("00");
                secLabel.setText("00");
            });
        } else if (auctionDTO.getStatus() == AuctionStatus.OPEN) {
            setupCountdown(auctionDTO.getEndTime(), () -> {
                hourLabel.setText("00");
                minLabel.setText("00");
                secLabel.setText("00");
            });
        } else {
            hourLabel.setText("00");
            minLabel.setText("00");
            secLabel.setText("00");
        }


        // Tải ảnh sản phẩm dựa trên loại
        try {
            String imagePath = "/images/items/" + auctionDTO.getType().toString().toLowerCase() + ".png";
            Image itemImage = new Image(getClass().getResourceAsStream(imagePath));
            itemImageView.setImage(itemImage);
        } catch (Exception e) {
            // Nếu không tìm thấy ảnh cụ thể, tải ảnh mặc định
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
        if (auctionDTO.getCurrentPrice() > 0) {
            currentPriceLabel.setText(String.format("$%,.0f", auctionDTO.getCurrentPrice()));
        } else {
            currentPriceLabel.setText(String.format("$%,.0f", auctionDTO.getStartingPrice()));
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
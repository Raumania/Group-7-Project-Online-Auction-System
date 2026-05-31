package auction_system.client.controller;

import auction_system.client.util.TimeUtil;
import auction_system.client.util.ViewSingleton;
import auction_system.client.service.ImageService;
import auction_system.client.store.AuctionStore;
import auction_system.common.dto.AuctionDTO;
import auction_system.common.enums.AuctionStatus;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
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

    @FXML private VBox rootCard;
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
    private ListChangeListener<AuctionDTO> storeListener;


    @FXML
    public void detailViewportBtn(ActionEvent event) {
        // Dọn dẹp timeline và listener trước khi chuyển màn hình
        cleanup();

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

    /**
     * Dọn dẹp tài nguyên để tránh Memory Leak:
     * - Stop timeline đếm ngược (tránh CPU chạy ngầm vô hạn)
     * - Remove storeListener khỏi AuctionStore (tránh Listener Accumulation)
     */
    private void cleanup() {
        if (timeline != null) {
            timeline.stop();
            timeline = null;
        }
        if (storeListener != null) {
            AuctionStore.getInstance().getAuctions().removeListener(storeListener);
            storeListener = null;
        }
    }

    public void setData(AuctionDTO auctionDTO) {
        // Bind image fit width to card width dynamically to prevent overflow
        itemImageView.fitWidthProperty().bind(rootCard.widthProperty().subtract(30));

        // Remove previous listener if this card is being reused
        if (storeListener != null) {
            AuctionStore.getInstance().getAuctions().removeListener(storeListener);
        }

        this.auctionDTO = auctionDTO;
        categoryLabel.setText(String.valueOf(auctionDTO.getType()));
        setDynamicName(auctionDTO.getName());
        updateStatusLabel(statusLabel, auctionDTO.getStatus());
        itemImageView.setImage(ImageService.getInstance().base64ToImage(auctionDTO.getImageBase64()));
        loadCurrentPrice(auctionDTO);

        if (auctionDTO.getHighestBidderUsername() != null && !auctionDTO.getHighestBidderUsername().isEmpty()) {
            highestBidderUsernameLabel.setText(auctionDTO.getHighestBidderUsername());
        } else {
            highestBidderUsernameLabel.setText("No bidder yet");
        }

        // Listen for real-time auction updates from server broadcasts
        storeListener = c -> {
            for (AuctionDTO updated : AuctionStore.getInstance().getAuctions()) {
                if (updated.getId() == this.auctionDTO.getId()) {
                    Platform.runLater(() -> {
                        this.auctionDTO = updated;
                        // Update current price label
                        loadCurrentPrice(updated);
                        // Update highest bidder label
                        if (updated.getHighestBidderUsername() != null && !updated.getHighestBidderUsername().isEmpty()) {
                            highestBidderUsernameLabel.setText(updated.getHighestBidderUsername());
                        } else {
                            highestBidderUsernameLabel.setText("No bidder yet");
                        }
                        // Update status label
                        updateStatusLabel(statusLabel, updated.getStatus());
                    });
                    break;
                }
            }
        };
        AuctionStore.getInstance().getAuctions().addListener(storeListener);

        // ✅ Tự động cleanup khi card bị xóa khỏi scene graph (dù navigate theo cách nào).
        // sceneProperty thay đổi từ non-null → null khi node bị remove khỏi cây UI.
        itemImageView.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene == null) {
                // Card vừa bị gỡ khỏi scene — dọn dẹp ngay
                cleanup();
            }
        });

        if (timeline != null) {
            timeline.stop();
        }

        // ĐÃ SỬA: So sánh bằng enum
        if (auctionDTO.getStatus() == AuctionStatus.OPEN) {
            setupCountdown(auctionDTO.getStartTime(), () -> {
                updateStatusLabel(statusLabel, AuctionStatus.RUNNING);
                auctionDTO.setStatus(AuctionStatus.RUNNING); // Cập nhật DTO
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

    /**
     * Đặt tên sản phẩm với font-size tự động theo độ dài:
     *  ≤ 15 ký tự  → 15px (to, rõ)
     *  16–25 ký tự → 13px (vừa)
     *  26–40 ký tự → 11px (nhỏ, vẫn đọc được)
     *  > 40 ký tự  → 10px + cắt bớt với "..." để không vỡ card
     */
    private void setDynamicName(String name) {
        if (name == null || name.isBlank()) {
            itemNameLabel.setText("—");
            itemNameLabel.setStyle("-fx-font-size: 15px;");
            return;
        }

        int len = name.length();
        String displayName;
        String fontSize;

        if (len <= 15) {
            displayName = name;
            fontSize = "15px";
        } else if (len <= 25) {
            displayName = name;
            fontSize = "13px";
        } else if (len <= 40) {
            displayName = name;
            fontSize = "11px";
        } else {
            // Cắt tối đa 40 ký tự + "..."
            displayName = name.substring(0, 40) + "...";
            fontSize = "10px";
        }

        itemNameLabel.setText(displayName);
        itemNameLabel.setStyle("-fx-font-size: " + fontSize + ";");
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
}
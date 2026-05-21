package auction_system.common.dto;

import auction_system.common.enums.AuctionStatus;
import auction_system.common.enums.ItemType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AuctionDTO {
    private int id;
    private int itemId;
    private int sellerId;
    private String name;
    private String description;
    private ItemType type;
    private AuctionStatus status; // <-- ĐÃ THAY ĐỔI
    private BigDecimal startingPrice;
    private BigDecimal currentPrice;
    private int highestBidderId;
    private String highestBidderUsername;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String imageBase64;

    public AuctionDTO() {
    }

    public AuctionDTO(String name, String description, ItemType type, int sellerId, BigDecimal startingPrice, LocalDateTime startTime, LocalDateTime endTime) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.sellerId = sellerId;
        this.startingPrice = startingPrice;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public AuctionDTO(String name, String description, ItemType type, int sellerId, BigDecimal startingPrice, LocalDateTime startTime, LocalDateTime endTime, String imageBase64) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.sellerId = sellerId;
        this.startingPrice = startingPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.imageBase64 = imageBase64;
    }

    // Getter và Setter cho status đã được cập nhật
    public AuctionStatus getStatus() {
        return status;
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getSellerId() {
        return sellerId;
    }

    public void setSellerId(int sellerId) {
        this.sellerId = sellerId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ItemType getType() {
        return type;
    }

    public void setType(ItemType type) {
        this.type = type;
    }

    public BigDecimal getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(BigDecimal startingPrice) {
        this.startingPrice = startingPrice;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public int getHighestBidderId() {
        return highestBidderId;
    }

    public void setHighestBidderId(int highestBidderId) {
        this.highestBidderId = highestBidderId;
    }

    public String getHighestBidderUsername() {
        return highestBidderUsername;
    }

    public void setHighestBidderUsername(String highestBidderUsername) {
        this.highestBidderUsername = highestBidderUsername;
    }

    public String getImageBase64() {
        return this.imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }
}
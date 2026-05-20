package auction_system.common.dto;

import auction_system.common.enums.AuctionStatus;
import auction_system.common.enums.ItemType;
import java.time.LocalDateTime;

public class AuctionDTO {
    private int id;
    private int sellerId;
    private String name;
    private String description;
    private ItemType type;
    private AuctionStatus status;
    private double startingPrice;
    private double currentPrice;
    private int highestBidderId;
    private String highestBidderUsername;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String imageBase64;

    public AuctionDTO() {
    }

    public AuctionDTO(String name, String description, ItemType type, int sellerId, double startingPrice, LocalDateTime startTime, LocalDateTime endTime,String imageBase64) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.sellerId = sellerId;
        this.startingPrice = startingPrice;
        this.startTime = startTime;
        this.endTime = endTime;
        this.imageBase64 = imageBase64;
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

    public AuctionStatus getStatus() {
        return status;
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(double startingPrice) {
        this.startingPrice = startingPrice;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
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

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }
}
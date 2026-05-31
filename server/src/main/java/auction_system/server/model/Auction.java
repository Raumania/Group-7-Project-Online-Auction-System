package auction_system.server.model;

import auction_system.common.dto.AuctionDTO;
import auction_system.common.enums.AuctionStatus;
import auction_system.common.enums.ItemType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Auction extends Entity{

    private int itemId; 
    private int sellerId;
    private String name;
    private String description;
    private ItemType type;
    private AuctionStatus status;
    private BigDecimal startingPrice;
    private BigDecimal currentPrice;
    private Integer highestBidderId;
    private String highestBidderUsername;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String imageBase64;
    private String imagePath;
    private String sellerUsername;
    public Auction() {
    }

    public Auction(AuctionDTO auctionDTO) {
        this.id = auctionDTO.getId();
        this.itemId = auctionDTO.getItemId();
        this.name = auctionDTO.getName();
        this.description = auctionDTO.getDescription();
        this.type = auctionDTO.getType();
        this.sellerId = auctionDTO.getSellerId();
        this.sellerUsername = auctionDTO.getSellerUsername();
        this.startingPrice = auctionDTO.getStartingPrice();
        this.currentPrice = auctionDTO.getCurrentPrice();
        this.highestBidderId = auctionDTO.getHighestBidderId() == 0
                ? null : auctionDTO.getHighestBidderId();
        this.highestBidderUsername = auctionDTO.getHighestBidderUsername();
        this.startTime = auctionDTO.getStartTime();
        this.endTime = auctionDTO.getEndTime();
        this.imageBase64 = auctionDTO.getImageBase64();
        
        // Logic gán status đã được đơn giản hóa
        if (auctionDTO.getStatus() != null) {
            this.status = auctionDTO.getStatus();
        } else {
            this.status = AuctionStatus.OPEN; // Fallback
        }
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
    public String  getImageBase64(){
        return this.imageBase64;
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

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }

    public BigDecimal getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(BigDecimal startingPrice) {
        this.startingPrice = startingPrice != null ? startingPrice.setScale(4, java.math.RoundingMode.HALF_UP) : null;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice != null ? currentPrice.setScale(4, java.math.RoundingMode.HALF_UP) : null;
    }

    public Integer getHighestBidderId() {
        return highestBidderId;
    }

    public void setHighestBidderId(Integer highestBidderId) {
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
    public void setImageBase64(String imageBase64){
        this.imageBase64=imageBase64;
    }
    public String getImagePath() {
        return imagePath;
    }
    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
    public String getSellerUsername() {
        return sellerUsername;
    }
    public void setSellerUsername(String sellerUsername) {
        this.sellerUsername = sellerUsername;
    }
    @Override
    public String toString() {
        return "Auction{" +
                "id=" + id +
                ", itemId=" + itemId +
                ", sellerId=" + sellerId +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", type=" + type +
                ", status=" + status +
                ", startingPrice=" + startingPrice +
                ", currentPrice=" + currentPrice +
                ", highestBidderId=" + highestBidderId +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", imageBase64='" + (imageBase64 != null && imageBase64.length() > 100 ? "[BASE64 IMAGE DATA TRUNCATED]" : imageBase64) + '\'' +
                '}';
    }
}
package auction_system.server.model;

import auction_system.common.dto.AuctionDTO;
import auction_system.common.enums.AuctionStatus;
import auction_system.common.enums.ItemType;

import java.time.LocalDateTime;

public class Auction extends Entity{

    private int sellerId;
    private String name;
    private String description;
    private ItemType type;
    private AuctionStatus status;
    private double startingPrice;
    private double currentPrice;
    private Integer highestBidderId;
    private String highestBidderUsername;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String imagebase64;
    public Auction() {
    }

    public Auction(AuctionDTO auctionDTO) {
        this.name = auctionDTO.getName();
        this.description = auctionDTO.getDescription();
        this.type = auctionDTO.getType();
        this.sellerId = auctionDTO.getSellerId();
        this.startingPrice = auctionDTO.getStartingPrice();
        this.startTime = auctionDTO.getStartTime();
        this.endTime = auctionDTO.getEndTime();
        this.status = AuctionStatus.OPEN;
        this.imagebase64=auctionDTO.getImagebase64();
    }

    public int getId() {
        return id;
    }
    public String  getImagebase64(){
        return this.imagebase64;
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
    public void setImagebase64(String imagebase64){
        this.imagebase64=imagebase64;
    }
    @Override
    public String toString() {
        return "Auction{" +
                "id=" + id +
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
                ", imagebase64='" + imagebase64 + '\'' +
                '}';
    }
}
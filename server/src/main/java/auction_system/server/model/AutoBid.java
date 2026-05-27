package auction_system.server.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class AutoBid extends Entity {
    private int userId;
    private int auctionId;
    private BigDecimal maxBid;
    private BigDecimal bidIncrement;
    private boolean isActive;
    private LocalDateTime createdAt;

    public AutoBid() {
    }

    public AutoBid(int userId, int auctionId, BigDecimal maxBid, BigDecimal bidIncrement) {
        this.userId = userId;
        this.auctionId = auctionId;
        this.maxBid = maxBid != null ? maxBid.setScale(4, java.math.RoundingMode.HALF_UP) : null;
        this.bidIncrement = bidIncrement != null ? bidIncrement.setScale(4, java.math.RoundingMode.HALF_UP) : null;
        this.isActive = true;
        this.createdAt = LocalDateTime.now();
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(int auctionId) {
        this.auctionId = auctionId;
    }

    public BigDecimal getMaxBid() {
        return maxBid;
    }

    public void setMaxBid(BigDecimal maxBid) {
        this.maxBid = maxBid != null ? maxBid.setScale(4, java.math.RoundingMode.HALF_UP) : null;
    }

    public BigDecimal getBidIncrement() {
        return bidIncrement;
    }

    public void setBidIncrement(BigDecimal bidIncrement) {
        this.bidIncrement = bidIncrement != null ? bidIncrement.setScale(4, java.math.RoundingMode.HALF_UP) : null;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

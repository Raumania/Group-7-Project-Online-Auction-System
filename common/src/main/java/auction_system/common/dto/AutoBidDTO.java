package auction_system.common.dto;

import java.io.Serializable;
import java.math.BigDecimal;

public class AutoBidDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private int userId;
    private int auctionId;
    private BigDecimal maxBid;
    private BigDecimal bidIncrement;

    public AutoBidDTO() {
    }

    public AutoBidDTO(int userId, int auctionId, BigDecimal maxBid, BigDecimal bidIncrement) {
        this.userId = userId;
        this.auctionId = auctionId;
        this.maxBid = maxBid;
        this.bidIncrement = bidIncrement;
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
        this.maxBid = maxBid;
    }

    public BigDecimal getBidIncrement() {
        return bidIncrement;
    }

    public void setBidIncrement(BigDecimal bidIncrement) {
        this.bidIncrement = bidIncrement;
    }
}

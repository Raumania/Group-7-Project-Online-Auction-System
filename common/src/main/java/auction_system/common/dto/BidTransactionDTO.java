package auction_system.common.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BidTransactionDTO {
    private int id;
    private int auctionId;
    private UserDTO bidder;
    private BigDecimal amount;
    private LocalDateTime biddingtime;

    public BidTransactionDTO() {
    }

    public BidTransactionDTO(int id, UserDTO bidder, BigDecimal amount, LocalDateTime biddingtime) {
        this.id = id;
        this.bidder = bidder;
        this.amount = amount;
        this.biddingtime = biddingtime;
    }

    public BidTransactionDTO(int id, int auctionId, UserDTO bidder, BigDecimal amount, LocalDateTime biddingtime) {
        this.id = id;
        this.auctionId = auctionId;
        this.bidder = bidder;
        this.amount = amount;
        this.biddingtime = biddingtime;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(int auctionId) {
        this.auctionId = auctionId;
    }

    public UserDTO getBidder() {
        return bidder;
    }

    public void setBidder(UserDTO bidder) {
        this.bidder = bidder;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getBiddingtime() {
        return biddingtime;
    }

    public void setBiddingtime(LocalDateTime biddingtime) {
        this.biddingtime = biddingtime;
    }
}

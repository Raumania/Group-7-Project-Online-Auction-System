package auction_system.server.common.protocol;

public class BidData {
    private String auctionId;
    private double amount;
    private String bidderId;

    public BidData() {
        // Constructor rỗng cần cho Gson
    }

    public BidData(String auctionId, double amount, String bidderId) {
        this.auctionId = auctionId;
        this.amount = amount;
        this.bidderId = bidderId;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getBidderId() {
        return bidderId;
    }

    public void setBidderId(String bidderId) {
        this.bidderId = bidderId;
    }
}
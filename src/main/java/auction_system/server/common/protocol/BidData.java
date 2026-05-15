package auction_system.server.common.protocol;

public class BidData {
    // CẬP NHẬT: Đổi kiểu dữ liệu từ String sang int
    private int auctionId;
    private double amount;
    private String bidderId;

    public BidData() {
        // Constructor rỗng cần cho Gson
    }

    // CẬP NHẬT: Tham số auctionId đổi thành int
    public BidData(int auctionId, double amount, String bidderId) {
        this.auctionId = auctionId;
        this.amount = amount;
        this.bidderId = bidderId;
    }

    // CẬP NHẬT: Kiểu trả về là int
    public int getAuctionId() {
        return auctionId;
    }

    // CẬP NHẬT: Tham số truyền vào là int
    public void setAuctionId(int auctionId) {
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
package auction_system.common.dto;

public class BidDTO {
    // CẬP NHẬT: Đổi kiểu dữ liệu từ String sang int
    private int auctionId;
    private double amount;
    private int bidderId;

    public BidDTO() {
        // Constructor rỗng cần cho Gson
    }

    // CẬP NHẬT: Tham số auctionId đổi thành int
    public BidDTO(int auctionId, double amount, int bidderId) {
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

    public int getBidderId() {
        return bidderId;
    }

    public void setBidderId(int bidderId) {
        this.bidderId = bidderId;
    }
}

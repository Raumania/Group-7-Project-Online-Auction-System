package auction_system.server.model;

import auction_system.server.exception.AuthorizationException;
import auction_system.server.exception.InvalidBidException;
import auction_system.server.util.IdGenerator;

import java.time.LocalDateTime;

public class BidTransaction extends Entity {

    /*
        Trước đây bidder là Bidder.

        Bây giờ:
        - User không còn chia cứng thành Bidder/Seller nữa
        - Một User có thể có nhiều role
        - Vì vậy bidder là User
        - Nhưng User này bắt buộc phải có role BIDDER
    */
    private User bidder;

    private double amount;

    /*
        Trước đây dùng long timestamp.

        Bây giờ dùng LocalDateTime cho giống Auction/Item.
        Trong database sẽ lưu bằng DATETIME.
    */
    private LocalDateTime bidTime;

    public BidTransaction(User bidder, double amount) {
        super();

        if (bidder == null) {
            throw new NullPointerException("Bidder cannot be null");
        }

        /*
            Không dùng instanceof Bidder nữa.
            Kiểm tra bằng role.
        */
        if (!bidder.hasRole(UserRole.BIDDER)) {
            throw new AuthorizationException("Bidder must have BIDDER role");
        }

        if (amount <= 0) {
            throw new InvalidBidException("Amount must be positive");
        }

        this.id = null;
        this.bidder = bidder;
        this.amount = amount;

        /*
            Thời điểm đặt bid hiện tại.
        */
        this.bidTime = LocalDateTime.now();
    }

    public User getBidder() {
        return bidder;
    }

    public double getAmount() {
        return amount;
    }

    public LocalDateTime getBidTime() {
        return bidTime;
    }

    /*
        Setter này cần cho DAO.

        Khi lấy bid transaction từ database ra,
        object Java phải giữ lại bidTime cũ trong database.
    */
    public void setBidTime(LocalDateTime bidTime) {
        this.bidTime = bidTime;
    }

    @Override
    public String toString() {
        return bidder.getUsername() + " bid " + amount + " at " + bidTime;
    }
}
package auction_system.server.model;

import auction_system.common.enums.UserRole;
import auction_system.server.exception.AuthorizationException;
import auction_system.server.exception.InvalidBidException;
import auction_system.server.util.IdGenerator;

import java.time.LocalDateTime;

public class BidTransaction extends Entity {

    private int id;
    private User bidder;

    private double amount;

    private LocalDateTime biddingtime;

    public BidTransaction() {
    }

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

        this.bidder = bidder;
        this.amount = amount;

        /*
            Thời điểm đặt bid hiện tại.
        */
        this.biddingtime = LocalDateTime.now();
    }

    public User getBidder() {
        return bidder;
    }

    public double getAmount() {
        return amount;
    }

    public LocalDateTime getBidTime() {
        return biddingtime;
    }

    /*
        Setter này cần cho DAO.

        Khi lấy bid transaction từ database ra,
        object Java phải giữ lại bidTime cũ trong database.
    */
    public void setBidTime(LocalDateTime bidTime) {
        this.biddingtime = bidTime;
    }
    public void setId(int id){
        this.id=id;
    }

    @Override
    public String toString() {
        return bidder.getUsername() + " bid " + amount + " at " + biddingtime;
    }
}
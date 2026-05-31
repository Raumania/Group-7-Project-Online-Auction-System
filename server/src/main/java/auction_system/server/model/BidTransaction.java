package auction_system.server.model;

import auction_system.common.enums.UserRole;
import auction_system.server.exception.AuthorizationException;
import auction_system.server.exception.InvalidBidException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BidTransaction extends Entity {

    private User bidder;

    private BigDecimal amount;

    private LocalDateTime biddingtime;

    public BidTransaction() {
    }

    public BidTransaction(User bidder, BigDecimal amount) {
        super();

        if (bidder == null) {
            throw new NullPointerException("Bidder cannot be null");
        }

        /*
            No longer using instanceof Bidder.
            Check using role.
        */
        if (!bidder.hasRole(UserRole.BIDDER)) {
            throw new AuthorizationException("Bidder must have BIDDER role");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidBidException("Amount must be positive");
        }

        this.bidder = bidder;
        this.amount = amount != null ? amount.setScale(4, java.math.RoundingMode.HALF_UP) : null;

        /*
            Current bid time.
        */
        this.biddingtime = LocalDateTime.now();
    }

    public User getBidder() {
        return bidder;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDateTime getBidTime() {
        return biddingtime;
    }

    /*
        This setter is needed for DAO.

        When retrieving bid transaction from database,
        Java object must keep the old bidTime from the database.
    */
    public void setBidTime(LocalDateTime bidTime) {
        this.biddingtime = bidTime;
    }

    public void setId(int id) {
        super.setId(id);
    }

    @Override
    public String toString() {
        return bidder.getUsername() + " bid " + amount + " at " + biddingtime;
    }
}
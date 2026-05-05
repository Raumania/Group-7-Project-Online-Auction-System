package auction_system.model;

import auction_system.util.IdGenerator;

public class BidTransaction extends Entity {

    private Bidder bidder;
    private double amount;
    private long timestamp;

    public BidTransaction(Bidder bidder, double amount) {
        super();

        if (bidder == null) {
            throw new RuntimeException("Bidder cannot be null");
        }

        if (amount <= 0) {
            throw new RuntimeException("Amount must be positive");
        }

        this.id = IdGenerator.generationBidTransactionId();
        this.bidder = bidder;
        this.amount = amount;
        this.timestamp = System.currentTimeMillis();
    }

    public Bidder getBidder() {
        return bidder;
    }

    public double getAmount() {
        return amount;
    }

    public long getTimestamp() {
        return timestamp;
    }

    /*
        Setter này cần cho DAO.

        Khi lấy bid transaction từ database ra,
        object Java phải giữ lại timestamp cũ trong database.
    */
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return bidder.getUsername() + " bid " + amount + " at " + timestamp;
    }
}
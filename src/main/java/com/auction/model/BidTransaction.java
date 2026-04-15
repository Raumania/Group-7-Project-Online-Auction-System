package com.auction.model;

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

    @Override
    public String toString() {
        return bidder.getUsername() + " bid " + amount + " at " + timestamp;
    }
}
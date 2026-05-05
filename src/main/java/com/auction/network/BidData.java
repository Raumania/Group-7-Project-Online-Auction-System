package com.auction.network;

import java.io.Serializable;

public class BidData implements Serializable {
    private static final long serialVersionUID = 1L;
    private String auctionId;
    private double amount;
    private String bidderId;

    public BidData(String auctionId, double amount, String bidderId) {
        this.auctionId = auctionId;
        this.amount = amount;
        this.bidderId = bidderId;
    }

    public String getAuctionId() { return auctionId; }
    public double getAmount() { return amount; }
    public String getBidderId() { return bidderId; }
}
package com.auction.model;

public class BidTransaction extends Entity{
    private Bidder bidder;
    private double amount;
    private long timestamp;
    public BidTransaction(Bidder bidder,double amount){
        super();
        this.bidder=bidder;
        this.amount=amount;
        this.timestamp=System.currentTimeMillis();
    }
    public  Bidder getBidder(){
        return bidder;
    }
    public double getAmount(){
        return amount;
    }
    public long getTimestamp(){
        return timestamp;
    }
}

package com.auction.model;
import com.auction.util.IdGenerator;

import java.util.ArrayList;
import java.util.List;
public class Auction extends Entity{
    private Item item;
    private Seller seller;
    private double currentPrice;
    private Bidder highrstbidder;
    private List<BidTransaction> bidHistory;
    private AuctionStatus status;
    public Auction(Item item, Seller seller){
        this.item=item;
        this.seller=seller;
        this.currentPrice=item.getStartingprice();
        this.bidHistory=new ArrayList<>();
        this.status=AuctionStatus.OPEN;
        this.id= IdGenerator.generationAuctionId();
    }
    public void placeBid(Bidder bidder,double amount){
        if(status!=AuctionStatus.OPEN){
            throw new RuntimeException("Auction is not open");
        }
        if(amount<=currentPrice){
            throw new RuntimeException("Bid must be higher than current price");
        }
        currentPrice=amount;
        highrstbidder=bidder;
        BidTransaction transaction=new BidTransaction(bidder,amount);
        bidHistory.add(transaction);
        System.out.println("New Bid "+ amount +" by "+bidder.getUsername());
    }
    public void closeAuction(){
        status=AuctionStatus.FINISHED;
        if(highrstbidder!=null){
            System.out.println("Winner "+ highrstbidder.getUsername());
        }
        else{
            System.out.println("No bid placed");
        }
    }
    public double getCurrentPrice(){
        return currentPrice;
    }
    public Bidder getHighrstbidder(){
        return highrstbidder;
    }
    public AuctionStatus getStatus(){
        return status;
    }
    public String getId(){
        return this.id;
    }
}

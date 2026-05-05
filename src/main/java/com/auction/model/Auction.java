package com.auction.model;
import com.auction.util.IdGenerator;
import com.auction.observer.AuctionObserver;
import com.auction.observer.AuctionSubject;
import java.util.ArrayList;
import java.util.List;
public class Auction extends Entity implements AuctionSubject{
    private Item item;
    private Seller seller;
    private double currentPrice;
    private Bidder highrstbidder;
    private List<BidTransaction> bidHistory;
    private AuctionStatus status;
    private List<AuctionObserver> observers = new ArrayList<>();
    public Auction(Item item, Seller seller){
        this.item=item;
        this.seller=seller;
        this.currentPrice=item.getStartingprice();
        this.bidHistory=new ArrayList<>();
        this.status=AuctionStatus.OPEN;
        this.id= IdGenerator.generationAuctionId();
    }
    @Override
    public void attach(AuctionObserver observer) {
        if (!observers.contains(observer)) observers.add(observer);
    }

    @Override
    public void detach(AuctionObserver observer) {
        observers.remove(observer);
    }

    @Override
    public void notifyObserversBid(BidTransaction bid) {
        for (AuctionObserver obs : observers) {
            obs.onBidPlaced(bid);
        }
    }
    @Override
    public void notifyObserversClosed(String auctionId) {
        for (AuctionObserver obs : observers) {
            obs.onAuctionClosed(auctionId);
        }
    }

    @Override
    public void notifyObserversPrice(String auctionId, double newPrice) {
        for (AuctionObserver obs : observers) {
            obs.onPriceUpdated(auctionId, newPrice);
        }
    }
    public synchronized void placeBid(Bidder bidder,double amount){
        if(status!=AuctionStatus.RUNNING){
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
    public synchronized void closeAuction(){
        status=AuctionStatus.FINISHED;
        if(highrstbidder!=null){
            System.out.println("Winner "+ highrstbidder.getUsername());
        }
        else{
            System.out.println("No bid placed");
        }
    }
    public synchronized void startAuction() {
        if (status == AuctionStatus.OPEN) {
            status = AuctionStatus.RUNNING;
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

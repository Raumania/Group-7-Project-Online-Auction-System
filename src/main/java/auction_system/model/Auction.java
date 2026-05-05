package auction_system.model;

import auction_system.model.AuctionStatus;
import auction_system.model.BidTransaction;
import auction_system.model.Bidder;
import auction_system.model.Entity;
import auction_system.model.Item;
import auction_system.model.Seller;
import auction_system.observer.AuctionObserver;
import auction_system.util.IdGenerator;

import java.util.ArrayList;
import java.util.List;

public class Auction extends Entity {
    private Item item;
    private Seller seller;
    private double currentPrice;
    private Bidder highestBidder;
    private List<BidTransaction> bidHistory;
    private AuctionStatus status;
    private List<AuctionObserver> observers;

    public Auction(Item item, Seller seller) {
        super();

        if (item == null) {
            throw new RuntimeException("Item cannot be null");
        }
        if (seller == null) {
            throw new RuntimeException("Seller cannot be null");
        }

        this.item = item;
        this.seller = seller;
        this.currentPrice = item.getStartingPrice();
        this.bidHistory = new ArrayList<>();
        this.status = AuctionStatus.OPEN;
        this.id = IdGenerator.generationAuctionId();
        this.observers = new ArrayList<>();
    }

    public void addObserver(AuctionObserver observer) {
        if (observer != null) {
            observers.add(observer);
        }
    }

    public void detach(AuctionObserver observer) {
        observers.remove(observer);
    }

    private void notifyObservers(String message) {
        for (AuctionObserver observer : observers) {
            observer.update(this, message);
        }
    }

    public synchronized void placeBid(Bidder bidder, double amount) {
        if (bidder == null) {
            throw new RuntimeException("Bidder cannot be null");
        }

        if (status != AuctionStatus.OPEN) {
            throw new RuntimeException("Auction is not open");
        }

        if (amount <= currentPrice) {
            throw new RuntimeException("Bid must be higher than current price");
        }

        currentPrice = amount;
        highestBidder = bidder;

        BidTransaction transaction = new BidTransaction(bidder, amount);
        bidHistory.add(transaction);

        System.out.println("New bid " + amount + " by " + bidder.getUsername());
        notifyObservers("New bid: " + amount + " by " + bidder.getUsername());
    }

    public synchronized void startAuction() {
        if (status == AuctionStatus.OPEN) {
            status = AuctionStatus.RUNNING;
        } else {
            throw new RuntimeException("Cannot start auction from status " + status);
        }
    }

    public synchronized void closeAuction() {
        if (status == AuctionStatus.FINISHED || status == AuctionStatus.CANCELLED) {
            throw new RuntimeException("Auction already closed");
        }

        status = AuctionStatus.FINISHED;

        if (highestBidder != null) {
            System.out.println("Winner " + highestBidder.getUsername());
            notifyObservers("Auction finished. Winner: " + highestBidder.getUsername());
        } else {
            System.out.println("No bid placed");
            notifyObservers("Auction finished with no bids.");
        }
    }

    public Item getItem() {
        return item;
    }

    public Seller getSeller() {
        return seller;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public Bidder getHighestBidder() {
        return highestBidder;
    }

    public List<BidTransaction> getBidHistory() {
        return bidHistory;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "Auction{id='" + id + "', item=" + item.getName() +
                ", seller=" + seller.getUsername() +
                ", currentPrice=" + currentPrice +
                ", status=" + status + "}";
    }
}
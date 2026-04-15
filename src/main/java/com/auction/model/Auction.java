package com.auction.model;

import com.auction.util.IdGenerator;

import java.util.ArrayList;
import java.util.List;

public class Auction extends Entity {
    private Item item;
    private Seller seller;
    private double currentPrice;
    private Bidder highestBidder;
    private List<BidTransaction> bidHistory;
    private AuctionStatus status;

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
    }

    public void placeBid(Bidder bidder, double amount) {
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
    }

    public void closeAuction() {
        if (status == AuctionStatus.FINISHED || status == AuctionStatus.CANCELLED) {
            throw new RuntimeException("Auction already closed");
        }

        status = AuctionStatus.FINISHED;

        if (highestBidder != null) {
            System.out.println("Winner " + highestBidder.getUsername());
        } else {
            System.out.println("No bid placed");
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
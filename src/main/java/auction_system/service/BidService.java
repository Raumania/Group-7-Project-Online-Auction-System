package auction_system.service;

import auction_system.model.Auction;
import auction_system.model.BidTransaction;
import auction_system.model.Bidder;
import auction_system.service.AuctionManager;

import java.util.List;

public class BidService {

    private AuctionManager auctionManager;

    public BidService() {
        this.auctionManager = AuctionManager.getInstance();
    }
    //đặt bid
    public void placeBid(String auctionId, Bidder bidder, double amount) {
        Auction auction = findAuctionOrThrow(auctionId);

        if (bidder == null) {
            throw new RuntimeException("Bidder cannot be null");
        }

        if (amount <= 0) {
            throw new RuntimeException("Bid amount must be greater than 0");
        }

        auction.placeBid(bidder, amount);
    }
    //lấy lịch sử bid
    public List<BidTransaction> getBidHistory(String auctionId) {
        Auction auction = findAuctionOrThrow(auctionId);
        return auction.getBidHistory();
    }
    //lấy bid gần nhất
    public BidTransaction getLatestBid(String auctionId) {
        Auction auction = findAuctionOrThrow(auctionId);
        List<BidTransaction> bidHistory = auction.getBidHistory();

        if (bidHistory.isEmpty()) {
            throw new RuntimeException("This auction has no bids yet");
        }

        return bidHistory.get(bidHistory.size() - 1);
    }

    public Bidder getHighestBidder(String auctionId) {
        Auction auction = findAuctionOrThrow(auctionId);
        return auction.getHighestBidder();
    }

    public double getCurrentPrice(String auctionId) {
        Auction auction = findAuctionOrThrow(auctionId);
        return auction.getCurrentPrice();
    }

    public int getTotalBids(String auctionId) {
        Auction auction = findAuctionOrThrow(auctionId);
        return auction.getBidHistory().size();
    }

    public boolean hasBids(String auctionId) {
        Auction auction = findAuctionOrThrow(auctionId);
        return !auction.getBidHistory().isEmpty();
    }

    public boolean isHighestBidder(String auctionId, Bidder bidder) {
        Auction auction = findAuctionOrThrow(auctionId);

        if (bidder == null) {
            return false;
        }

        if (auction.getHighestBidder() == null) {
            return false;
        }

        return auction.getHighestBidder().equals(bidder);
    }

    public boolean auctionExists(String auctionId) {
        if (auctionId == null || auctionId.trim().isEmpty()) {
            return false;
        }

        return auctionManager.findAuctionById(auctionId) != null;
    }

    private Auction findAuctionOrThrow(String auctionId) {
        if (auctionId == null || auctionId.trim().isEmpty()) {
            throw new RuntimeException("Auction id cannot be null or empty");
        }

        Auction auction = auctionManager.findAuctionById(auctionId);

        if (auction == null) {
            throw new RuntimeException("Auction not found");
        }

        return auction;
    }
}
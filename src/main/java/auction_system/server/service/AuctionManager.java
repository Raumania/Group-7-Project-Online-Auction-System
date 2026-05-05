package auction_system.server.service;

import auction_system.server.model.Auction;
import auction_system.server.model.AuctionStatus;

import java.util.ArrayList;
import java.util.List;

public class AuctionManager {

    private static AuctionManager instance;
    private List<Auction> auctions;

    private AuctionManager() {
        auctions = new ArrayList<>();
    }

    public static AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
    }

    public void addAuction(Auction auction) {
        if (auction == null) {
            throw new RuntimeException("Auction cannot be null");
        }

        if (findAuctionById(auction.getId()) != null) {
            throw new RuntimeException("Auction already exists");
        }

        auctions.add(auction);
    }

    public Auction findAuctionById(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new RuntimeException("Auction id cannot be null or empty");
        }

        for (Auction auction : auctions) {
            if (auction.getId().equals(id)) {
                return auction;
            }
        }

        return null;
    }

    public List<Auction> getAllAuctions() {
        return new ArrayList<>(auctions);
    }

    public List<Auction> getOpenAuctions() {
        List<Auction> result = new ArrayList<>();

        for (Auction auction : auctions) {
            if (auction.getStatus() == AuctionStatus.OPEN) {
                result.add(auction);
            }
        }

        return result;
    }

    public boolean removeAuction(String id) {
        Auction auction = findAuctionById(id);

        if (auction != null) {
            auctions.remove(auction);
            return true;
        }

        return false;
    }
}
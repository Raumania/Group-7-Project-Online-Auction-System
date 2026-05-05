package auction_system.service;

import auction_system.model.Auction;
import auction_system.model.Item;
import auction_system.model.Seller;

import java.util.List;

public class AuctionService {

    private static AuctionService instance;
    private AuctionManager auctionManager;

    // Constructor private
    private AuctionService() {
        this.auctionManager = AuctionManager.getInstance();
    }

    // Singleton getInstance
    public static synchronized AuctionService getInstance() {
        if (instance == null) {
            instance = new AuctionService();
        }
        return instance;
    }

    // Các phương thức giữ nguyên y hệt
    public Auction createAuction(Item item, Seller seller) {

        if (item == null) {
            throw new RuntimeException("Item cannot be null");
        }

        if (seller == null) {
            throw new RuntimeException("Seller cannot be null");
        }

        if (item.getOwner() == null) {
            throw new RuntimeException("Item must have owner");
        }
        if (!item.getOwner().equals(seller)) {
            throw new RuntimeException("Seller does not own this item");
        }

        Auction auction = new Auction(item, seller);
        auctionManager.addAuction(auction);
        return auction;
    }

    public void closeAuction(String auctionId) {

        if (auctionId == null || auctionId.isEmpty()) {
            throw new RuntimeException("Auction id cannot be null or empty");
        }

        Auction auction = auctionManager.findAuctionById(auctionId);

        if (auction == null) {
            throw new RuntimeException("Auction not found");
        }

        auction.closeAuction();
    }

    public List<Auction> getAllAuctions() {
        return auctionManager.getAllAuctions();
    }

    public List<Auction> getOpenAuctions() {
        return auctionManager.getOpenAuctions();
    }

    public Auction getAuctionById(String id) {

        if (id == null || id.isEmpty()) {
            throw new RuntimeException("Auction id cannot be null or empty");
        }

        Auction auction = auctionManager.findAuctionById(id);

        if (auction == null) {
            throw new RuntimeException("Auction not found");
        }

        return auction;
    }

    public void removeAuction(String id) {

        if (id == null || id.isEmpty()) {
            throw new RuntimeException("Auction id cannot be null or empty");
        }

        boolean removed = auctionManager.removeAuction(id);

        if (!removed) {
            throw new RuntimeException("Auction not found to remove");
        }
    }
}
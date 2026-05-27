package auction_system.server.store;

import auction_system.server.model.Auction;
import auction_system.common.enums.AuctionStatus;
import auction_system.server.dao.AuctionDAO;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.ArrayList;

public class AuctionStore {
    private static AuctionStore instance;
    private final ConcurrentHashMap<Integer, Auction> auctions = new ConcurrentHashMap<>();
    private final AuctionDAO auctionDAO;

    private AuctionStore() {
        this.auctionDAO = AuctionDAO.getInstance();
    }

    public static synchronized AuctionStore getInstance() {
        if (instance == null) {
            instance = new AuctionStore();
        }
        return instance;
    }

    public synchronized void init() {
        auctions.clear();
        try {
            System.out.println("[AuctionStore] Loading all auctions from database...");
            List<Auction> allAuctions = auctionDAO.findAll();
            for (Auction auction : allAuctions) {
                auctions.put(auction.getId(), auction);
            }
            System.out.println("[AuctionStore] Loaded " + auctions.size() + " auctions into memory cache.");
        } catch (Exception e) {
            System.err.println("[AuctionStore] Failed to load auctions from DB: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void addAuction(Auction auction) {
        if (auction != null) {
            auctions.put(auction.getId(), auction);
        }
    }

    public void updateAuction(Auction auction) {
        addAuction(auction);
    }

    public void removeAuction(int id) {
        auctions.remove(id);
    }

    public Auction getAuctionById(int id) {
        return auctions.get(id);
    }

    public List<Auction> getAllAuctions() {
        return new ArrayList<>(auctions.values());
    }

    public List<Auction> getActiveAuctions() {
        List<Auction> active = new ArrayList<>();
        for (Auction a : auctions.values()) {
            if (a.getStatus() == AuctionStatus.OPEN || a.getStatus() == AuctionStatus.RUNNING) {
                active.add(a);
            }
        }
        return active;
    }
}
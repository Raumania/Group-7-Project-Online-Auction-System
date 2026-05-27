package auction_system.server.store;

import auction_system.server.model.AutoBid;
import auction_system.server.dao.AutoBidDAO;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.ArrayList;

public class AutoBidStore {
    private static AutoBidStore instance;
    private final ConcurrentHashMap<Integer, CopyOnWriteArrayList<AutoBid>> autoBidsByAuction = new ConcurrentHashMap<>();
    private final AutoBidDAO autoBidDAO;

    private AutoBidStore() {
        this.autoBidDAO = AutoBidDAO.getInstance();
    }

    public static synchronized AutoBidStore getInstance() {
        if (instance == null) {
            instance = new AutoBidStore();
        }
        return instance;
    }

    public synchronized void init() {
        autoBidsByAuction.clear();
        try {
            System.out.println("[AutoBidStore] Loading all auto bids from database...");
            List<AutoBid> allAutoBids = autoBidDAO.findAll();
            for (AutoBid ab : allAutoBids) {
                int auctionId = ab.getAuctionId();
                autoBidsByAuction.compute(auctionId, (id, list) -> {
                    if (list == null) {
                        list = new CopyOnWriteArrayList<>();
                    }
                    list.add(ab);
                    return list;
                });
            }
            System.out.println("[AutoBidStore] Loaded auto bids into memory cache.");
        } catch (Exception e) {
            System.err.println("[AutoBidStore] Failed to load auto bids from DB: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void addOrUpdateAutoBid(AutoBid autoBid) {
        if (autoBid == null) return;
        int auctionId = autoBid.getAuctionId();
        autoBidsByAuction.compute(auctionId, (id, list) -> {
            if (list == null) {
                list = new CopyOnWriteArrayList<>();
            }
            list.removeIf(ab -> ab.getUserId() == autoBid.getUserId());
            list.add(autoBid);
            return list;
        });
    }

    public List<AutoBid> getActiveAutoBidsByAuctionId(int auctionId) {
        CopyOnWriteArrayList<AutoBid> list = autoBidsByAuction.get(auctionId);
        if (list == null) {
            return new ArrayList<>();
        }
        List<AutoBid> active = new ArrayList<>();
        for (AutoBid ab : list) {
            if (ab.isActive()) {
                active.add(ab);
            }
        }
        return active;
    }

    public void deactivateAutoBid(int id) {
        for (CopyOnWriteArrayList<AutoBid> list : autoBidsByAuction.values()) {
            for (AutoBid ab : list) {
                if (ab.getId() == id) {
                    ab.setActive(false);
                    return;
                }
            }
        }
    }

    public void disableAutoBid(int userId, int auctionId) {
        CopyOnWriteArrayList<AutoBid> list = autoBidsByAuction.get(auctionId);
        if (list != null) {
            for (AutoBid ab : list) {
                if (ab.getUserId() == userId) {
                    ab.setActive(false);
                }
            }
        }
    }
}
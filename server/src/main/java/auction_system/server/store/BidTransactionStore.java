package auction_system.server.store;

import auction_system.server.model.BidTransaction;
import auction_system.server.model.Auction;
import auction_system.server.dao.BidTransactionDAO;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.ArrayList;

public class BidTransactionStore {
    private static BidTransactionStore instance;
    private final ConcurrentHashMap<Integer, CopyOnWriteArrayList<BidTransaction>> histories = new ConcurrentHashMap<>();
    private final BidTransactionDAO bidTransactionDAO;

    private BidTransactionStore() {
        this.bidTransactionDAO = BidTransactionDAO.getInstance();
    }

    public static synchronized BidTransactionStore getInstance() {
        if (instance == null) {
            instance = new BidTransactionStore();
        }
        return instance;
    }

    public synchronized void init() {
        histories.clear();
        try {
            System.out.println("[BidTransactionStore] Loading bid history for all auctions from database...");
            List<Auction> allAuctions = AuctionStore.getInstance().getAllAuctions();
            for (Auction a : allAuctions) {
                List<BidTransaction> transactions = bidTransactionDAO.findByAuctionId(a.getId());
                histories.put(a.getId(), new CopyOnWriteArrayList<>(transactions));
            }
            System.out.println("[BidTransactionStore] Loaded bid history cache.");
        } catch (Exception e) {
            System.err.println("[BidTransactionStore] Failed to load bid history: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void addBid(int auctionId, BidTransaction transaction) {
        if (transaction == null) return;
        histories.compute(auctionId, (id, list) -> {
            if (list == null) {
                list = new CopyOnWriteArrayList<>();
            }
            list.add(transaction);
            return list;
        });
    }

    public List<BidTransaction> getHistory(int auctionId) {
        CopyOnWriteArrayList<BidTransaction> list = histories.get(auctionId);
        return list != null ? new ArrayList<>(list) : new ArrayList<>();
    }

    public BidTransaction getLatestBid(int auctionId) {
        CopyOnWriteArrayList<BidTransaction> list = histories.get(auctionId);
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.get(list.size() - 1);
    }

    public synchronized void removeBidsByBidderAndAuction(int bidderId, int auctionId) {
        CopyOnWriteArrayList<BidTransaction> list = histories.get(auctionId);
        if (list != null) {
            list.removeIf(tx -> tx.getBidder().getId() == bidderId);
        }
    }
}
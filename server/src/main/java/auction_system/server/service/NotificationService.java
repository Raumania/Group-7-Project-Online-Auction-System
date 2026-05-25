package auction_system.server.service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

//(aucID, userID)
public class NotificationService {
    private static NotificationService instance;
    private final Map<Integer, Set<Integer>> auctions = new ConcurrentHashMap<>();

    private NotificationService() {
    }

    public Map<Integer, Set<Integer>> getAuctions() {
        return auctions;
    }

    public static NotificationService getInstance() {
        if (instance == null) {
            instance = new NotificationService();
        }
        return instance;
    }

    // Client ngắt kết nối
    public void unregister(int auctionId, String subID) {
        Set<Integer> subsID = auctions.get(auctionId);
        if (subsID != null) {
            subsID.remove(subID);
        }
    }

}

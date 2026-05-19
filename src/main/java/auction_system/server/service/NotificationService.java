package auction_system.server.service;

import auction_system.server.dao.UserDAO;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

//(aucID, userID)
public class NotificationService {
    private static NotificationService instance;
    public static final Map<Integer, Set<Integer>> auctions =
            new ConcurrentHashMap<>();

    private NotificationService() {
    }

    public static NotificationService getInstance() {
        if (instance == null) {
            instance = new NotificationService();
        }
        return instance;
    }

    // Client kết nối vào và đăng ký theo dõi phiên
    public void register(int auctionId, int subID) {
        auctions
                .computeIfAbsent(auctionId, k -> ConcurrentHashMap.newKeySet())
                .add(subID);
    }

    // Client ngắt kết nối
    public void unregister(int auctionId, String subID) {
        Set<Integer> subsID = auctions.get(auctionId);
        if (subsID != null) {
            subsID.remove(subID);
        }
    }

}

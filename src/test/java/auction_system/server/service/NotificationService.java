package auction_system.server.service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

//(aucID, userID)
public class NotificationService {
    private static NotificationService instance;
    private final Map<String, Set<String>> auctions =
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
    public void register(String auctionId, String subID) {
        auctions
                .computeIfAbsent(auctionId, k -> ConcurrentHashMap.newKeySet())
                .add(subID);
    }

    // Client ngắt kết nối
    public void unregister(String auctionId, String subID) {
        Set<String> subsID = auctions.get(auctionId);
        if (subsID != null) {
            subsID.remove(subID);
        }
    }

}

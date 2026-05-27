package auction_system.client.store;

import auction_system.common.dto.BidTransactionDTO;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BidTransactionStore {
    private static BidTransactionStore instance;

    // Maps auctionId -> ObservableList of BidTransactionDTO (newest first)
    private final Map<Integer, ObservableList<BidTransactionDTO>> histories = new ConcurrentHashMap<>();

    private BidTransactionStore() {}

    public static synchronized BidTransactionStore getInstance() {
        if (instance == null) {
            instance = new BidTransactionStore();
        }
        return instance;
    }

    // Returns the ObservableList for an auction (creates empty one if not exists)
    public ObservableList<BidTransactionDTO> getHistory(int auctionId) {
        return histories.computeIfAbsent(auctionId, id -> FXCollections.observableArrayList());
    }

    // Overwrite entire history list (called on first load from server)
    public void setHistory(int auctionId, List<BidTransactionDTO> historyList) {
        Platform.runLater(() -> {
            ObservableList<BidTransactionDTO> list = getHistory(auctionId);
            // Reverse so newest bid is at index 0 (top of table)
            java.util.List<BidTransactionDTO> reversed = new java.util.ArrayList<>(historyList);
            java.util.Collections.reverse(reversed);
            list.setAll(reversed);
        });
    }

    // Prepend a single new transaction (real-time push from EVENT_BID_PLACED socket event)
    public void addBid(int auctionId, BidTransactionDTO transaction) {
        Platform.runLater(() -> {
            ObservableList<BidTransactionDTO> list = getHistory(auctionId);
            list.add(0, transaction); // newest first
        });
    }

    // Clear cache for a specific auction (e.g. when auction ends or user navigates away)
    public void clearHistory(int auctionId) {
        histories.remove(auctionId);
    }
    public void logout() {
        histories.clear();
    }
}

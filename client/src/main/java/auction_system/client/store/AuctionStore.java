package auction_system.client.store;

import auction_system.common.dto.AuctionDTO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class AuctionStore {
    private static AuctionStore instance;
    private final ObservableList<AuctionDTO> auctions = FXCollections.observableArrayList();

    private AuctionStore() {
    }

    public static synchronized AuctionStore getInstance() {
        if (instance == null) {
            instance = new AuctionStore();
        }
        return instance;
    }

    public ObservableList<AuctionDTO> getAuctions() {
        return auctions;
    }

    public void setAuctions(java.util.List<AuctionDTO> newAuctions) {
        auctions.setAll(newAuctions);
    }

    public void addAuction(AuctionDTO auction) {
        auctions.add(auction);
    }

    public void updateAuction(AuctionDTO auction) {
        // This is a simple approach. For more complex scenarios, you might want to replace
        // the item instead of just updating properties if the AuctionDTO is not mutable.
        int index = -1;
        for (int i = 0; i < auctions.size(); i++) {
            if (auctions.get(i).getId() == auction.getId()) {
                index = i;
                break;
            }
        }
        if (index != -1) {
            auctions.set(index, auction);
        }
    }

    public void removeAuction(int auctionId) {
        auctions.removeIf(auction -> auction.getId() == auctionId);
    }
}

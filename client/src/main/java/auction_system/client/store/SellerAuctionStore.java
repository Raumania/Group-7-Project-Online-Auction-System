package auction_system.client.store;

import auction_system.common.dto.AuctionDTO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class SellerAuctionStore {
    private static SellerAuctionStore instance;
    private final ObservableList<AuctionDTO> auctions = FXCollections.observableArrayList();

    private SellerAuctionStore() {
    }

    public static synchronized SellerAuctionStore getInstance() {
        if (instance == null) {
            instance = new SellerAuctionStore();
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
    public void logout() {
        auctions.clear();
    }
}
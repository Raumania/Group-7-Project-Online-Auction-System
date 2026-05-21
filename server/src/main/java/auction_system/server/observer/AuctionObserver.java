package auction_system.server.observer;

import auction_system.server.model.Auction;
import auction_system.server.observer.BidEvent;

public interface AuctionObserver {
    void onBidPlaced(BidEvent event);
    void update(Auction auction, String message);
    void onAuctionCreated(Auction auction);
    void onAuctionEdited(Auction auction);
    void onAuctionDeleted(int auctionId);
}
package auction_system.server.observer;

import auction_system.server.model.Auction;

public interface AuctionObserver {
    void onBidPlaced(BidEvent event);
    void update(Auction auction, String message);
}
package auction_system.server.observer;

import auction_system.server.observer.BidEvent;

public interface AuctionObserver {
    void onBidPlaced(BidEvent event);
}
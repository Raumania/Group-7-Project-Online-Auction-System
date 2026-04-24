package auction_system.observer;

import auction_system.model.Auction;

public interface AuctionObserver {
    void update(Auction auction, String message);
}
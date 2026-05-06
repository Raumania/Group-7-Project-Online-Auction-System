package auction_system.server.observer;

import auction_system.server.model.Auction;

public interface AuctionObserver {
    void update(Auction auction, String message);
}
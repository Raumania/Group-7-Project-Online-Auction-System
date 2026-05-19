package auction_system.server;

import auction_system.server.observer.AuctionScheduler;

public class TestScheduler {
    void main() {
        AuctionScheduler auctionScheduler = AuctionScheduler.getInstance();
        auctionScheduler.start();
        while (true) {
        }
    }
}



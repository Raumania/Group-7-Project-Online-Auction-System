package auction_system.observer;

import auction_system.model.Auction;
import auction_system.observer.AuctionObserver;

public class ConsoleObserver implements AuctionObserver {

    private String name;

    public ConsoleObserver(String name) {
        this.name = name;
    }

    @Override
    public void update(Auction auction, String message) {
        System.out.println("[" + name + "] " + message + " | Auction ID: " + auction.getId());
    }
}
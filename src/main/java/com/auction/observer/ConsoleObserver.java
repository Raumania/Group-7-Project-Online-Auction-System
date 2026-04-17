package com.auction.observer;

import com.auction.model.Auction;

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
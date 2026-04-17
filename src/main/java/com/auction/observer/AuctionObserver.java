package com.auction.observer;

import com.auction.model.Auction;

public interface AuctionObserver {
    void update(Auction auction, String message);
}
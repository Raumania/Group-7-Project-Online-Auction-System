package com.auction.observer;

import com.auction.model.BidTransaction;

public interface AuctionSubject {
    void attach(AuctionObserver observer);
    void detach(AuctionObserver observer);
    void notifyObserversBid(BidTransaction bid);
    void notifyObserversClosed(String auctionId);
    void notifyObserversPrice(String auctionId, double newPrice);
}
package com.auction.observer;

import com.auction.model.BidTransaction;

public interface AuctionObserver {
    void onBidPlaced(BidTransaction bid);
    void onAuctionClosed(String auctionId);
    void onPriceUpdated(String auctionId, double newPrice);
}
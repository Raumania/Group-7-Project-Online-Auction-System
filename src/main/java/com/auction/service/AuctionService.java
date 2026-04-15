package com.auction.service;

import com.auction.service.AuctionManager;
import com.auction.model.Auction;
import com.auction.model.Bidder;
import com.auction.model.Item;
import com.auction.model.Seller;

import java.util.List;

public class AuctionService {

    private AuctionManager auctionManager;

    public AuctionService() {
        this.auctionManager = AuctionManager.getInstance();
    }

    public Auction createAuction(Item item, Seller seller) {
        Auction auction = new Auction(item, seller);
        auctionManager.addAuction(auction);
        return auction;
    }

    public void placeBid(String auctionId, Bidder bidder, double amount) {
        Auction auction = auctionManager.findAuctionById(auctionId);

        if (auction == null) {
            throw new RuntimeException("Auction not found");
        }

        auction.placeBid(bidder, amount);
    }

    public void closeAuction(String auctionId) {
        Auction auction = auctionManager.findAuctionById(auctionId);

        if (auction == null) {
            throw new RuntimeException("Auction not found");
        }

        auction.closeAuction();
    }

    public List<Auction> getAllAuctions() {
        return auctionManager.getAllAuctions();
    }

    public List<Auction> getOpenAuctions() {
        return auctionManager.getOpenAuctions();
    }

    public Auction getAuctionById(String id) {
        Auction auction = auctionManager.findAuctionById(id);

        if (auction == null) {
            throw new RuntimeException("Auction not found");
        }

        return auction;
    }

    public void removeAuction(String id) {
        auctionManager.removeAuction(id);
    }
}
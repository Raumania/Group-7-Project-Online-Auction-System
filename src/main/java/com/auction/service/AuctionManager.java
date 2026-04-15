package com.auction.service;

import com.auction.model.Auction;
import com.auction.model.AuctionStatus;

import java.util.ArrayList;
import java.util.List;

public class AuctionManager {

    private static AuctionManager instance;
    private List<Auction> auctions;

    private AuctionManager() {
        auctions = new ArrayList<>();
    }

    public static AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }
        return instance;
    }

    public void addAuction(Auction auction) {
        if (auction == null) {
            throw new RuntimeException("Auction cannot be null");
        }
        auctions.add(auction);
    }

    public Auction findAuctionById(String id) {
        for (Auction auction : auctions) {
            if (auction.getId().equals(id)) {
                return auction;
            }
        }
        return null;
    }

    public List<Auction> getAllAuctions() {
        return new ArrayList<>(auctions);
    }

    public List<Auction> getOpenAuctions() {
        List<Auction> result = new ArrayList<>();

        for (Auction auction : auctions) {
            if (auction.getStatus() == AuctionStatus.OPEN) {
                result.add(auction);
            }
        }

        return result;
    }

    public void removeAuction(String id) {
        Auction auction = findAuctionById(id);

        if (auction == null) {
            throw new RuntimeException("Auction not found");
        }

        auctions.remove(auction);
    }
}
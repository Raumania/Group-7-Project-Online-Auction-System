package com.auction.service;

import com.auction.model.Auction;
import com.auction.model.AuctionStatus;
import com.auction.model.Item;
import com.auction.model.Seller;

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

    public Auction createAuction(Item item, Seller seller) {
        Auction auction = new Auction(item, seller);
        auctions.add(auction);
        return auction;
    }

    public List<Auction> getAllAuctions() {
        return auctions;
    }

    public Auction findAuctionById(String id) {
        for (Auction auction : auctions) {
            if (auction.getId().equals(id)) {
                return auction;
            }
        }
        return null;
    }

    public List<Auction> getOpenAuctions() {
        List<Auction> result = new ArrayList<>();

        for (Auction auction : auctions) {
            if (auction.getStatus() == AuctionStatus.OPEN ||
                    auction.getStatus() == AuctionStatus.RUNNING) {
                result.add(auction);
            }
        }

        return result;
    }

    public void removeAuction(String id) {
        Auction auction = findAuctionById(id);

        if (auction != null) {
            auctions.remove(auction);
        }
    }

    public void closeAuction(String id) {
        Auction auction = findAuctionById(id);

        if (auction != null) {
            auction.closeAuction();
        }
    }
}
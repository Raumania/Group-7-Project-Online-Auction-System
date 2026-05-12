package auction_system;

import auction_system.server.model.Auction;
import auction_system.server.service.AuctionService;

import java.util.List;

public class TestGetAllAuctions {
    public static void main(String[] args) {
        AuctionService auctionService = AuctionService.getInstance();

        List<Auction> auctions = auctionService.getAllAuctions();

        System.out.println("Total auctions: " + auctions.size());

        for (Auction auction : auctions) {
            System.out.println("--------------------");
            System.out.println("Id: " + auction.getId());
            System.out.println("Item: " + auction.getItem().getName());
            System.out.println("Seller: " + auction.getSeller().getUsername());
            System.out.println("Current price: " + auction.getCurrentPrice());
            System.out.println("Status: " + auction.getStatus());
        }
    }
}
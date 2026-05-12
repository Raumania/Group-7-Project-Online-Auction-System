package auction_system;

import auction_system.server.model.*;
import auction_system.server.service.*;

public class TestFullAuctionFlow {
    public static void main(String[] args) {
        UserService userService = new UserService();
        ItemService itemService = new ItemService();
        AuctionService auctionService = AuctionService.getInstance();
        BidService bidService = new BidService();

        Seller seller = userService.createSeller(
                "phong",
                "123456",
                "seller_full_1@gmail.com"
        );

        Bidder bidder = userService.createBidder(
                "mo",
                "123456",
                "bidder_full_1@gmail.com"
        );
        Bidder bidder1=userService.createBidder("bach","123456","duacbanh@gmail.com");
        userService.deposit(bidder.getId(), 5000);
        userService.deposit(bidder1.getId(),36000);

        Electronics item = itemService.createElectronics(
                "Laptop Full Test",
                "Laptop dùng để test full flow",
                1000,
                seller,
                "Dell",
                "G15"
        );

        Auction auction = auctionService.createAuction(item, seller);

        auctionService.startAuction(auction.getId());

        bidService.placeBid(auction.getId(), bidder, 1500);
        bidService.placeBid(auction.getId(),bidder1,1600);
        bidService.placeBid(auction.getId(), bidder, 1800);
        bidService.placeBid(auction.getId(),bidder1,2000);

        auctionService.closeAuction(auction.getId());

        Auction finalAuction = auctionService.getAuctionById(auction.getId());
        User finalBidder = userService.getUserById(bidder.getId());

        System.out.println("Full auction flow success!");
        System.out.println("Auction id: " + finalAuction.getId());
        System.out.println("Item: " + finalAuction.getItem().getName());
        System.out.println("Final price: " + finalAuction.getCurrentPrice());
        System.out.println("Status: " + finalAuction.getStatus());
        System.out.println("Winner: " + finalAuction.getHighestBidder().getUsername());
        System.out.println("Bidder balance after bid: " + finalBidder.getBalance());
        System.out.println("Total bids: " + bidService.getTotalBids(auction.getId()));
    }
}
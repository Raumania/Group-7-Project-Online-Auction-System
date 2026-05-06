package auction_system;

import auction_system.model.Auction;
import auction_system.server.service.AuctionService;

public class TestCloseAuction {
    public static void main(String[] args) {
        AuctionService auctionService = AuctionService.getInstance();

        /*
            Đổi id này thành id thật trong bảng auctions.
            Nên dùng auction đang OPEN hoặc RUNNING.
        */
        String auctionId = "AUCTION _ 7e6ddbef-382f-43b1-840a-20afd4d47fca";

        Auction beforeAuction = auctionService.getAuctionById(auctionId);

        System.out.println("Before close:");
        System.out.println("Id: " + beforeAuction.getId());
        System.out.println("Item: " + beforeAuction.getItem().getName());
        System.out.println("Status: " + beforeAuction.getStatus());
        System.out.println("Highest bidder: " +
                (beforeAuction.getHighestBidder() == null
                        ? "none"
                        : beforeAuction.getHighestBidder().getUsername()));

        auctionService.closeAuction(auctionId);

        Auction afterAuction = auctionService.getAuctionById(auctionId);

        System.out.println("After close:");
        System.out.println("Id: " + afterAuction.getId());
        System.out.println("Item: " + afterAuction.getItem().getName());
        System.out.println("Status: " + afterAuction.getStatus());
        System.out.println("Highest bidder: " +
                (afterAuction.getHighestBidder() == null
                        ? "none"
                        : afterAuction.getHighestBidder().getUsername()));
    }
}
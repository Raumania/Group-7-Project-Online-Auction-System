package auction_system;

import auction_system.server.model.Auction;
import auction_system.server.service.AuctionService;

public class TestStartAuction {
    public static void main(String[] args) {
        AuctionService auctionService = AuctionService.getInstance();

        /*
            Đổi id này thành id thật trong bảng auctions.
            Vào Workbench chạy:
            SELECT id, item_id, current_price, status FROM auctions;
        */
        String auctionId = "AUCTION _ 7e6ddbef-382f-43b1-840a-20afd4d47fca";

        Auction beforeAuction = auctionService.getAuctionById(auctionId);

        System.out.println("Before start:");
        System.out.println("Id: " + beforeAuction.getId());
        System.out.println("Item: " + beforeAuction.getItem().getName());
        System.out.println("Status: " + beforeAuction.getStatus());

        auctionService.startAuction(auctionId);

        Auction afterAuction = auctionService.getAuctionById(auctionId);

        System.out.println("After start:");
        System.out.println("Id: " + afterAuction.getId());
        System.out.println("Item: " + afterAuction.getItem().getName());
        System.out.println("Status: " + afterAuction.getStatus());
    }
}
package auction_system;

import auction_system.server.model.Auction;
import auction_system.server.model.Electronics;
import auction_system.server.model.User;
import auction_system.server.service.AuctionService;
import auction_system.server.service.BidService;
import auction_system.server.service.ItemService;
import auction_system.server.service.UserService;

public class TestFullAuctionFlow {
    public static void main(String[] args) {
        UserService userService = new UserService();
        ItemService itemService = new ItemService();
        AuctionService auctionService = AuctionService.getInstance();
        BidService bidService = new BidService();

        /*
            1. Tạo 3 user.

            user1: người bán
            user2: bidder thứ nhất
            user3: bidder thứ hai

            Theo thiết kế mới:
            - Mỗi user mặc định có cả BIDDER và SELLER
            - id do MySQL AUTO_INCREMENT tạo
        */
        User user1 = userService.registerUser(
                "Nguyen Van Seller",
                "seller_test_01",
                "123456"
        );

        User user2 = userService.registerUser(
                "Nguyen Van Bidder 1",
                "bidder_test_01",
                "123456"
        );

        User user3 = userService.registerUser(
                "Nguyen Van Bidder 2",
                "bidder_test_02",
                "123456"
        );

        System.out.println("Seller id: " + user1.getId());
        System.out.println("Bidder 1 id: " + user2.getId());
        System.out.println("Bidder 2 id: " + user3.getId());

        /*
            2. Nạp tiền cho 2 bidder.
        */
        userService.deposit(user2.getId(), 1000000);
        userService.deposit(user3.getId(), 1000000);

        System.out.println("Bidder 1 balance after deposit: "
                + userService.getUserById(user2.getId()).getBalance());

        System.out.println("Bidder 2 balance after deposit: "
                + userService.getUserById(user3.getId()).getBalance());

        /*
            3. Seller tạo item.
        */
        Electronics item = itemService.createElectronics(
                "Laptop Dell G15",
                "Laptop gaming dùng để test auction",
                1000,
                user1,
                "Dell",
                "G15"
        );

        System.out.println("Item created:");
        System.out.println(item);
        System.out.println("Item id: " + item.getId());

        /*
            4. Seller tạo auction.
        */
        Auction auction = auctionService.createAuction(item, user1);

        System.out.println("Auction created:");
        System.out.println("Auction id: " + auction.getId());
        System.out.println("Current price: " + auction.getCurrentPrice());
        System.out.println("Status: " + auction.getStatus());

        /*
            5. Start auction.
        */
        auctionService.startAuction(auction.getId());

        System.out.println("Auction started.");

        /*
            6. Bidder 1 đặt giá 1500.
        */
        bidService.placeBid(
                auction.getId(),
                user2,
                1500
        );

        System.out.println("Bidder 1 placed bid: 1500");

        Auction afterBid1 = auctionService.getAuctionById(auction.getId());

        System.out.println("After bid 1:");
        System.out.println("Current price: " + afterBid1.getCurrentPrice());
        System.out.println("Highest bidder: " + afterBid1.getHighestBidder().getUsername());

        /*
            7. Bidder 2 đặt giá cao hơn 2000.
        */
        bidService.placeBid(
                auction.getId(),
                user3,
                2000
        );

        System.out.println("Bidder 2 placed bid: 2000");

        Auction afterBid2 = auctionService.getAuctionById(auction.getId());

        System.out.println("After bid 2:");
        System.out.println("Current price: " + afterBid2.getCurrentPrice());
        System.out.println("Highest bidder: " + afterBid2.getHighestBidder().getUsername());
        System.out.println("Total bids: " + bidService.getTotalBids(auction.getId()));

        /*
            8. Đóng auction.
        */
        auctionService.closeAuction(auction.getId());

        Auction closedAuction = auctionService.getAuctionById(auction.getId());

        System.out.println("Auction closed.");
        System.out.println("Final status: " + closedAuction.getStatus());
        System.out.println("Winner: " + closedAuction.getHighestBidder().getUsername());
        System.out.println("Final price: " + closedAuction.getCurrentPrice());

        /*
            9. Kiểm tra balance sau đấu giá.
        */
        User bidder1AfterBid = userService.getUserById(user2.getId());
        User bidder2AfterBid = userService.getUserById(user3.getId());

        System.out.println("Bidder 1 balance after auction: " + bidder1AfterBid.getBalance());
        System.out.println("Bidder 2 balance after auction: " + bidder2AfterBid.getBalance());
    }
}
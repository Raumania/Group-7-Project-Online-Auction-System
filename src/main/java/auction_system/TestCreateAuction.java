package auction_system;

import auction_system.model.Auction;
import auction_system.model.Item;
import auction_system.model.Seller;
import auction_system.model.User;
import auction_system.server.service.AuctionService;
import auction_system.server.service.ItemService;
import auction_system.server.service.UserService;

public class TestCreateAuction {
    public static void main(String[] args) {
        UserService userService = new UserService();
        ItemService itemService = new ItemService();

        /*
            Vì AuctionService đã là Singleton,
            không dùng new AuctionService() nữa.
        */
        AuctionService auctionService = AuctionService.getInstance();

        /*
            Lấy seller đã có trong database.
            Nếu seller của bạn không phải seller1 thì đổi username.
        */
        User user = userService.getUserByUsername("seller1");
        Seller seller = (Seller) user;

        /*
            Tạo item mới để đưa vào auction.
            Dùng tên khác mỗi lần test để dễ nhìn trong database.
        */
        Item item = itemService.createElectronics(
                "Laptop Auction Test 1",
                "Laptop dùng để test tạo auction",
                700,
                seller,
                "Dell",
                "XPS"
        );

        /*
            Tạo auction.
            Hàm này sẽ lưu vào bảng auctions qua AuctionDAO.
        */
        Auction auction = auctionService.createAuction(item, seller);

        System.out.println("Create auction successfully!");
        System.out.println("Id: " + auction.getId());
        System.out.println("Item: " + auction.getItem().getName());
        System.out.println("Seller: " + auction.getSeller().getUsername());
        System.out.println("Current price: " + auction.getCurrentPrice());
        System.out.println("Status: " + auction.getStatus());
    }
}
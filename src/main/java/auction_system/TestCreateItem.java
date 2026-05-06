package auction_system;

import auction_system.model.Electronics;
import auction_system.model.Seller;
import auction_system.model.User;
import auction_system.server.service.ItemService;
import auction_system.server.service.UserService;

public class TestCreateItem {
    public static void main(String[] args) {
        UserService userService = new UserService();
        ItemService itemService = new ItemService();

        /*
            Lấy seller đã có trong database.
            Đổi "seller1" nếu username seller của bạn khác.
        */
        User user = userService.getUserByUsername("seller1");

        /*
            Ép kiểu User về Seller.
        */
        Seller seller = (Seller) user;

        /*
            Tạo Electronics qua ItemService.
            Service sẽ tự gọi ItemDAO.save().
        */
        Electronics item = itemService.createElectronics(
                "Laptop Dell",
                "Laptop gaming cũ còn tốt",
                500,
                seller,
                "Dell",
                "G15"
        );

        System.out.println("Create item successfully!");
        System.out.println("Id: " + item.getId());
        System.out.println("Name: " + item.getName());
        System.out.println("Description: " + item.getDescription());
        System.out.println("Starting price: " + item.getStartingPrice());
        System.out.println("Owner: " + item.getOwner().getUsername());
        System.out.println("Type: " + item.getType());
        System.out.println("Brand: " + item.getBrand());
        System.out.println("Model: " + item.getModel());
    }
}
package auction_system;

import auction_system.server.model.Seller;
import auction_system.server.service.UserService;

public class TestCreateSeller {
    public static void main(String[] args) {
        UserService userService = new UserService();

        Seller seller = userService.createSeller(
                "duy",
                "123456",
                "seller1@gmail.com"
        );

        System.out.println("Create seller successfully!");
        System.out.println("Id: " + seller.getId());
        System.out.println("Username: " + seller.getUsername());
        System.out.println("Email: " + seller.getEmail());
        System.out.println("Role: " + seller.getRole());
        System.out.println("Balance: " + seller.getBalance());
    }
}
package auction_system;

import auction_system.model.Bidder;
import auction_system.model.User;
import auction_system.service.UserService;

public class TestRegister {
    public static void main(String[] args) {
        UserService userService = new UserService();

        Bidder bidder = userService.createBidder(
                "chuong",
                "123456",
                "chuong@gmail.com"
        );

        System.out.println("Register successfully!");
        System.out.println("Before deposit:");
        System.out.println("Id: " + bidder.getId());
        System.out.println("Username: " + bidder.getUsername());
        System.out.println("Balance: " + bidder.getBalance());

        userService.deposit(bidder.getId(), 1000);

        User updatedUser = userService.getUserById(bidder.getId());

        System.out.println("After deposit:");
        System.out.println("Id: " + updatedUser.getId());
        System.out.println("Username: " + updatedUser.getUsername());
        System.out.println("Balance: " + updatedUser.getBalance());
    }
}
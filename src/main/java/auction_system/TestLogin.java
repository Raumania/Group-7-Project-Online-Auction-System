package auction_system;

import auction_system.server.model.Bidder;
import auction_system.server.model.User;
import auction_system.server.service.UserService;

public class TestLogin {
    public static void main(String[] args) {
        UserService userService = new UserService();

        User user = userService.login("taixiu", "123456");

        System.out.println("Login successfully!");
        System.out.println("Id: " + user.getId());
        System.out.println("Username: " + user.getUsername());
        System.out.println("Email: " + user.getEmail());
        System.out.println("Role: " + user.getRole());
        System.out.println("Balance: " + user.getBalance());
        /*
            Bước 2: nạp tiền cho bidder.
            Hàm deposit sẽ:
            - lấy user từ database
            - cộng tiền vào balance
            - update balance lại vào MySQL
        */
        userService.withdraw(user.getId(), 100);

        /*
            Bước 3: lấy lại user từ database để kiểm tra balance mới.
        */
        Bidder updatedBidder = (Bidder) userService.getUserById(user.getId());

        System.out.println("After deposit:");
        System.out.println("Id: " + updatedBidder.getId());
        System.out.println("Username: " + updatedBidder.getUsername());
        System.out.println("Balance: " + updatedBidder.getBalance());
    }
}
package auction_system;

import auction_system.server.service.UserService;

public class TestRemoveUser {
    public static void main(String[] args) {
        UserService userService = new UserService();

        /*
            Đổi id này thành id thật đang có trong bảng users.
            Ví dụ: BIDDER1, USER1, SELLER1...
        */
        String id = "BID _ 17136f64-1b6c-4256-a590-40938292f884";

        userService.removeUser(id);

        System.out.println("Remove user successfully!");
    }
}
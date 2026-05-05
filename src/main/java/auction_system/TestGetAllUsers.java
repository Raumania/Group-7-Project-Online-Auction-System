package auction_system;

import auction_system.model.User;
import auction_system.service.UserService;

import java.util.List;

public class TestGetAllUsers {
    public static void main(String[] args) {
        UserService userService = new UserService();

        /*
            Gọi service để lấy toàn bộ user trong database.
        */
        List<User> users = userService.getAllUsers();

        /*
            In số lượng user lấy được.
        */
        System.out.println("Total users: " + users.size());

        /*
            Duyệt từng user và in thông tin ra màn hình.
        */
        for (User user : users) {
            System.out.println("--------------------");
            System.out.println("Id: " + user.getId());
            System.out.println("Username: " + user.getUsername());
            System.out.println("Email: " + user.getEmail());
            System.out.println("Role: " + user.getRole());
            System.out.println("Balance: " + user.getBalance());
        }
    }
}
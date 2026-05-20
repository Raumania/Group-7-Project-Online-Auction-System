//package auction_system;
//
//import auction_system.server.model.User;
//import auction_system.server.service.UserService;
//
//public class TestCreateUser {
//    public static void main(String[] args) {
//        UserService userService = new UserService();
//
//        /*
//            Tạo user mới.
//
//            Theo thiết kế mới:
//            - Không truyền email nữa
//            - Có fullname
//            - User mặc định có role BIDDER và SELLER
//            - id của user do MySQL AUTO_INCREMENT tự sinh
//        */
//        User user = userService.registerUser(
//                "Nguyen Van A",
//                "vana",
//                "123456"
//        );
//
//        System.out.println("Create user successfully");
//        System.out.println("User id: " + user.getId());
//        System.out.println("Fullname: " + user.getFullname());
//        System.out.println("Username: " + user.getUsername());
//        System.out.println("Roles: " + user.getRoles());
//        System.out.println("Balance: " + user.getBalance());
//    }
//}
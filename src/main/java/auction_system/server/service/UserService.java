package auction_system.server.service;

import auction_system.server.model.Bidder;
import auction_system.server.model.Seller;
import auction_system.server.model.User;
import auction_system.server.dao.UserDAO;

import java.util.List;

public class UserService {

    private UserDAO userDAO;

    public UserService() {
        this.userDAO = new UserDAO();
    }

    /*
        Tạo Seller mới.

        Trước đây:
        - tạo Seller
        - lưu vào UserManager/List

        Bây giờ:
        - tạo Seller
        - kiểm tra username đã tồn tại chưa
        - lưu vào database qua UserDAO
    */
    public Seller createSeller(String username, String password, String email) {
        User existingUser = userDAO.findByUsername(username);

        if (existingUser != null) {
            throw new RuntimeException("Username already exists");
        }

        Seller seller = new Seller(username, password, email);
        userDAO.save(seller);

        return seller;
    }

    /*
        Tạo Bidder mới.
    */
    public Bidder createBidder(String username, String password, String email) {
        User existingUser = userDAO.findByUsername(username);

        if (existingUser != null) {
            throw new RuntimeException("Username already exists");
        }

        Bidder bidder = new Bidder(username, password, email);
        userDAO.save(bidder);

        return bidder;
    }

    /*
        Tìm user theo id.

        Nếu DAO trả về null nghĩa là database không có user đó.
    */
    public User getUserById(String id) {
        User user = userDAO.findById(id);

        if (user == null) {
            throw new RuntimeException("User not found");
        }

        return user;
    }

    /*
        Tìm user theo username.
    */
    public User getUserByUsername(String username) {
        User user = userDAO.findByUsername(username);

        if (user == null) {
            throw new RuntimeException("User not found");
        }

        return user;
    }

    /*
        Đăng nhập.

        Bước 1: tìm user theo username.
        Bước 2: kiểm tra password.
        Bước 3: đúng thì trả về user.
    */
    public User login(String username, String password) {
        User user = userDAO.findByUsername(username);

        if (user == null) {
            throw new RuntimeException("User not found");
        }

        if (!user.getPassword().equals(password)) {
            throw new RuntimeException("Wrong password");
        }

        return user;
    }

    /*
        Lấy tất cả user.

        Muốn dùng hàm này thì trong UserDAO cần có findAll().
    */
    public List<User> getAllUsers() {
        return userDAO.findAll();
    }

    /*
        Xóa user theo id.

        Muốn biết xóa thành công hay không thì UserDAO nên trả về boolean.
        Nếu UserDAO của bạn đang là void deleteById(String id),
        mình khuyên sửa thành boolean.
    */
    public void removeUser(String id) {
        boolean removed = userDAO.deleteById(id);

        if (!removed) {
            throw new RuntimeException("User not found");
        }
    }
    public void deposit(String userId, double amount) {
        User user = getUserById(userId);

        user.deposit(amount);

        boolean updated = userDAO.updateBalance(user.getId(), user.getBalance());

        if (!updated) {
            throw new RuntimeException("Cannot update balance");
        }
    }

    public void withdraw(String userId, double amount) {
        User user = getUserById(userId);

        user.withdraw(amount);

        boolean updated = userDAO.updateBalance(user.getId(), user.getBalance());

        if (!updated) {
            throw new RuntimeException("Cannot update balance");
        }
    }
}
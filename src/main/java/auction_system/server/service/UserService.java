package auction_system.server.service;

import auction_system.server.dao.UserDAO;
import auction_system.server.model.User;
import auction_system.server.model.UserRole;

import java.util.List;
import java.util.Set;

public class UserService {

    private UserDAO userDAO;

    public UserService() {
        this.userDAO = new UserDAO();
    }

    /*
        Tạo User mới.

        Trước đây:
        - createSeller(...) tạo Seller
        - createBidder(...) tạo Bidder
        - mỗi tài khoản chỉ có 1 role

        Bây giờ:
        - tạo User
        - 1 tài khoản có thể có nhiều role
        - ví dụ: BIDDER, SELLER
        - kiểm tra username đã tồn tại chưa
        - lưu vào database qua UserDAO
    */
    public User registerUser(String username, String password, String email, Set<UserRole> roles) {
        User existingUser = userDAO.findByUsername(username);

        if (existingUser != null) {
            throw new RuntimeException("Username already exists");
        }

        if (roles == null || roles.isEmpty()) {
            throw new RuntimeException("User must have at least one role");
        }

        User user = new User(username, password, email, roles);

        userDAO.save(user);

        return user;
    }

    /*
        Tạo Seller mới.

        Hàm này giữ lại để code cũ chưa bị lỗi ngay.

        Nhưng bản chất bây giờ không tạo Seller object nữa,
        mà tạo User có role SELLER.
    */
    public User createSeller(String username, String password, String email) {
        return registerUser(
                username,
                password,
                email,
                Set.of(UserRole.SELLER)
        );
    }

    /*
        Tạo Bidder mới.

        Hàm này giữ lại để code cũ chưa bị lỗi ngay.

        Nhưng bản chất bây giờ không tạo Bidder object nữa,
        mà tạo User có role BIDDER.
    */
    public User createBidder(String username, String password, String email) {
        return registerUser(
                username,
                password,
                email,
                Set.of(UserRole.BIDDER)
        );
    }

    /*
        Tạo user vừa là Bidder vừa là Seller.

        Dùng khi 1 tài khoản vừa có thể đấu giá,
        vừa có thể đăng bán sản phẩm.
    */
    public User createBidderAndSeller(String username, String password, String email) {
        return registerUser(
                username,
                password,
                email,
                Set.of(UserRole.BIDDER, UserRole.SELLER)
        );
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
        Lấy user theo id và kiểm tra user đó có role SELLER hay không.

        Trước đây:
        - dùng instanceof Seller

        Bây giờ:
        - không dùng instanceof nữa
        - dùng hasRole(UserRole.SELLER)
    */
    public User getSellerById(String id) {
        User user = getUserById(id);

        if (user.hasRole(UserRole.SELLER)) {
            return user;
        }

        throw new RuntimeException("User with id " + id + " is not a seller");
    }

    /*
        Lấy user theo id và kiểm tra user đó có role BIDDER hay không.
    */
    public User getBidderById(String id) {
        User user = getUserById(id);

        if (user.hasRole(UserRole.BIDDER)) {
            return user;
        }

        throw new RuntimeException("User with id " + id + " is not a bidder");
    }

    /*
        Lấy user theo id và kiểm tra user đó có role ADMIN hay không.
    */
    public User getAdminById(String id) {
        User user = getUserById(id);

        if (user.hasRole(UserRole.ADMIN)) {
            return user;
        }

        throw new RuntimeException("User with id " + id + " is not an admin");
    }

    /*
        Thêm role cho user.

        Ví dụ:
        - user ban đầu chỉ là BIDDER
        - sau đó muốn user đó đăng bán sản phẩm
        - thì thêm role SELLER
    */
    public void addRole(String userId, UserRole role) {
        User user = getUserById(userId);

        user.addRole(role);

        userDAO.update(user);
    }

    /*
        Xóa role của user.

        Ví dụ:
        - xóa quyền SELLER
        - nhưng user vẫn còn BIDDER

        Trong class User nên chặn trường hợp xóa hết role.
    */
    public void removeRole(String userId, UserRole role) {
        User user = getUserById(userId);

        user.removeRole(role);

        userDAO.update(user);
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
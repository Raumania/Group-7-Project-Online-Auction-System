package auction_system.server.service;

import auction_system.common.enums.UserRole;
import auction_system.server.dao.UserDAO;
import auction_system.server.exception.AuthorizationException;
import auction_system.server.exception.ControllerException.*;
import auction_system.server.model.User;
import auction_system.server.util.HashUtil;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public class UserService {
    private static UserService instance;
    private final UserDAO userDAO;

    public UserService() {
        this.userDAO = UserDAO.getInstance();
    }

    public static UserService getInstance() {
        if (instance == null) {
            instance = new UserService();
        }
        return instance;
    }

    public User registerUser(String fullname, String username, String password) {
        return registerUser(
                fullname,
                username,
                password,
                Set.of(UserRole.BIDDER, UserRole.SELLER)
        );
    }

    public User registerUser(String fullname, String username, String password, Set<UserRole> roles) {
        User existingUser = userDAO.findByUsername(username);

        if (existingUser != null) {
            throw new DuplicateResourceException("Username already exists");
        }

        if (roles == null || roles.isEmpty()) {
            throw new InvalidInputException("User must have at least one role");
        }
        String hashpassword = HashUtil.hashPassword(password);
        User user = new User(fullname, username, hashpassword, roles);

        userDAO.save(user);

        return user;
    }

    /*
        Tạo Seller mới.

        Hàm này giữ lại để code cũ chưa bị lỗi ngay.

        Nhưng bản chất bây giờ không tạo Seller object nữa,
        mà tạo User có role SELLER.
    */
    public User createSeller(String fullname, String username, String password) {
        return registerUser(
                fullname,
                username,
                password,
                Set.of(UserRole.SELLER)
        );
    }

    /*
        Tạo Bidder mới.

        Hàm này giữ lại để code cũ chưa bị lỗi ngay.

        Nhưng bản chất bây giờ không tạo Bidder object nữa,
        mà tạo User có role BIDDER.
    */
    public User createBidder(String fullname, String username, String password) {
        return registerUser(
                fullname,
                username,
                password,
                Set.of(UserRole.BIDDER)
        );
    }

    /*
        Tạo user vừa là Bidder vừa là Seller.

        Dùng khi 1 tài khoản vừa có thể đấu giá,
        vừa có thể đăng bán sản phẩm.

        Thực ra hàm này bây giờ giống registerUser(fullname, username, password).
        Giữ lại để code dễ đọc.
    */
    public User createBidderAndSeller(String fullname, String username, String password) {
        return registerUser(fullname, username, password);
    }

    /*
        Tạo Admin.

        Không nên cho mọi user mặc định là ADMIN.
        Admin nên tạo riêng bằng hàm này hoặc bằng seed data.
    */
    public User createAdmin(String fullname, String username, String password) {
        return registerUser(
                fullname,
                username,
                password,
                Set.of(UserRole.ADMIN)
        );
    }

    /*
        Tạo Admin có cả quyền BIDDER và SELLER.

        Dùng nếu bạn muốn admin cũng có thể mua/bán.
    */
    public User createFullAdmin(String fullname, String username, String password) {
        return registerUser(
                fullname,
                username,
                password,
                Set.of(UserRole.ADMIN, UserRole.BIDDER, UserRole.SELLER)
        );
    }

    /*
        Tìm user theo id.

        Nếu DAO trả về null nghĩa là database không có user đó.
    */
    public User getUserById(int id) {
        User user = userDAO.findById(id);

        if (user == null) {
            throw new UserNotFoundException(id);
        }

        return user;
    }

    /*
        Tìm user theo username.
    */
    public User getUserByUsername(String username) {
        User user = userDAO.findByUsername(username);

        if (user == null) {
            throw new UserNotFoundException(username);
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
    public User getSellerById(int id) {
        User user = getUserById(id);

        if (user.hasRole(UserRole.SELLER)) {
            return user;
        }

        throw new AuthorizationException("User with id " + id + " is not a seller");
    }

    /*
        Lấy user theo id và kiểm tra user đó có role ADMIN hay không.
    */
    public User getAdminById(int id) {
        User user = getUserById(id);

        if (user.hasRole(UserRole.ADMIN)) {
            return user;
        }

        throw new AuthorizationException("User with id " + id + " is not an admin");
    }

    /*
        Thêm role cho user.

        Ví dụ:
        - user ban đầu chỉ là BIDDER
        - sau đó muốn user đó đăng bán sản phẩm
        - thì thêm role SELLER
    */
    public void addRole(int userId, UserRole role) {
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
    public void removeRole(int userId, UserRole role) {
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
            throw new AuthenticationException("Username does not exist");
        }

        boolean correctPassword = HashUtil.checkPassword(password, user.getPassword());

        if (!correctPassword) {
            throw new AuthenticationException("Wrong password");
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
    public void removeUser(int id) {
        boolean removed = userDAO.deleteById(id);

        if (!removed) {
            throw new UserNotFoundException(id);
        }
    }

    public void deposit(int userId, BigDecimal amount) {
        User user = getUserById(userId);

        user.deposit(amount);

        boolean updated = userDAO.updateBalance(user.getId(), user.getBalance());

        if (!updated) {
            throw new DatabaseException("Cannot update balance");
        }
    }

    public void withdraw(int userId, BigDecimal amount) {
        User user = getUserById(userId);

        user.withdraw(amount);

        boolean updated = userDAO.updateBalance(user.getId(), user.getBalance());

        if (!updated) {
            throw new DatabaseException("Cannot update balance");
        }
    }
}
package auction_system.server.service;

import auction_system.common.enums.UserRole;
import auction_system.server.dao.UserDAO;
import auction_system.server.exception.AuthorizationException;
import auction_system.server.exception.ControllerException.*;
import auction_system.server.model.User;
import auction_system.server.store.UserStore;
import auction_system.server.util.HashUtil;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

public class UserService {
    private static UserService instance;
    private final UserDAO userDAO;
    private final UserStore userStore;

    private UserService() {
        this.userDAO = UserDAO.getInstance();
        this.userStore = UserStore.getInstance();
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
        User existingUser = userStore.getUserByUsername(username);

        if (existingUser != null) {
            throw new DuplicateResourceException("Username already exists");
        }

        if (roles == null || roles.isEmpty()) {
            throw new InvalidInputException("User must have at least one role");
        }
        String hashpassword = HashUtil.hashPassword(password);
        User user = new User(fullname, username, hashpassword, roles);

        userDAO.save(user);
        
        // Sync with memory cache
        userStore.addUser(user);

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
        Nếu RAM Store trả về null nghĩa là cache không có user đó.
    */
    public User getUserById(int id) {
        User user = userStore.getUserById(id);

        if (user == null) {
            throw new UserNotFoundException(id);
        }

        return user;
    }

    /*
        Tìm user theo username.
    */
    public User getUserByUsername(String username) {
        User user = userStore.getUserByUsername(username);

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
        
        // Sync cache
        userStore.updateUser(user);
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
        
        // Sync cache
        userStore.updateUser(user);
    }

    /*
        Đăng nhập.
        Bước 1: tìm user theo username.
        Bước 2: kiểm tra password.
        Bước 3: đúng thì trả về user.
    */
    public User login(String username, String password) {
        User user = userStore.getUserByUsername(username);

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
    */
    public List<User> getAllUsers() {
        return userStore.getAllUsers();
    }

    /*
        Xóa user theo id.
    */
    public void removeUser(int id) {
        boolean removed = userDAO.deleteById(id);

        if (!removed) {
            throw new UserNotFoundException(id);
        }
        
        // Sync cache
        userStore.removeUser(id);
    }

    public void deposit(int userId, BigDecimal amount) {
        User user = getUserById(userId);

        user.deposit(amount);

        boolean updated = userDAO.updateBalance(user.getId(), user.getBalance());

        if (!updated) {
            throw new DatabaseException("Cannot update balance");
        }
        
        // Sync cache
        userStore.updateUser(user);
    }

    public void withdraw(int userId, BigDecimal amount) {
        User user = getUserById(userId);

        user.withdraw(amount);

        boolean updated = userDAO.updateBalance(user.getId(), user.getBalance());

        if (!updated) {
            throw new DatabaseException("Cannot update balance");
        }
        
        // Sync cache
        userStore.updateUser(user);
    }
}
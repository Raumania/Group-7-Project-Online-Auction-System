package auction_system.server.dao;

import auction_system.server.model.User;
import auction_system.server.model.UserRole;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UserDAO {

    /*
        Hàm save dùng để lưu 1 User vào bảng users trong MySQL.

        Bảng users cũ của bạn đang có các cột:
        id, username, password, email, role

        Sau khi sửa sang mô hình 1 user có nhiều role:

        Bảng users sẽ lưu thông tin chính của user:
        id, username, password, email, balance

        Bảng user_roles sẽ lưu các role của user:
        user_id, role

        Ví dụ:
        users:
        U001, chuong, 123456, chuong@gmail.com, 5000

        user_roles:
        U001, BIDDER
        U001, SELLER
    */
    public void save(User user) {
        String sql = "INSERT INTO users(id, username, password, email, balance) VALUES (?, ?, ?, ?, ?)";

        /*
            try-with-resources:
            Sau khi chạy xong, Connection và PreparedStatement sẽ tự đóng.
            Như vậy tránh bị rò rỉ kết nối database.
        */
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            /*
                Gán dữ liệu vào từng dấu ? trong câu SQL.

                ? thứ 1 -> id
                ? thứ 2 -> username
                ? thứ 3 -> password
                ? thứ 4 -> email
                ? thứ 5 -> balance

                Role không còn lưu trực tiếp trong bảng users nữa.
                Role sẽ được lưu ở bảng user_roles.
            */
            statement.setString(1, user.getId());
            statement.setString(2, user.getUsername());
            statement.setString(3, user.getPassword());
            statement.setString(4, user.getEmail());
            statement.setDouble(5, user.getBalance());

            /*
                executeUpdate dùng cho các lệnh:
                INSERT, UPDATE, DELETE.
            */
            statement.executeUpdate();

            /*
                Sau khi lưu thông tin user vào bảng users,
                ta tiếp tục lưu danh sách role của user vào bảng user_roles.
            */
            saveRoles(user.getId(), user.getRoles());

        } catch (SQLException e) {
            throw new RuntimeException("Cannot save user", e);
        }
    }

    /*
        Hàm saveRoles dùng để lưu nhiều role của 1 user.

        Ví dụ user có id là U001 và có roles:
        BIDDER, SELLER

        Thì bảng user_roles sẽ có 2 dòng:
        U001 BIDDER
        U001 SELLER
    */
    private void saveRoles(String userId, Set<UserRole> roles) {
        String sql = "INSERT INTO user_roles(user_id, role) VALUES (?, ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            for (UserRole role : roles) {
                /*
                    ? thứ 1 -> user_id
                    ? thứ 2 -> role
                */
                statement.setString(1, userId);

                /*
                    role là enum UserRole.
                    .name() chuyển enum thành String.

                    Ví dụ:
                    UserRole.BIDDER.name() -> "BIDDER"
                */
                statement.setString(2, role.name());

                /*
                    addBatch dùng để gom nhiều câu INSERT lại.
                    Sau vòng lặp sẽ chạy executeBatch một lần.
                */
                statement.addBatch();
            }

            statement.executeBatch();

        } catch (SQLException e) {
            throw new RuntimeException("Cannot save user roles", e);
        }
    }

    /*
        Hàm findByUsername dùng để tìm user theo username.

        Dùng khi:
        - login
        - kiểm tra username đã tồn tại chưa
    */
    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            /*
                Gán username vào dấu ? trong câu SQL.
            */
            statement.setString(1, username);

            /*
                executeQuery dùng cho SELECT.
                Kết quả trả về là ResultSet.
            */
            ResultSet resultSet = statement.executeQuery();

            /*
                resultSet.next():
                - true nếu có 1 dòng dữ liệu
                - false nếu không tìm thấy user
            */
            if (resultSet.next()) {
                return mapResultSetToUser(resultSet);
            }

            return null;

        } catch (SQLException e) {
            throw new RuntimeException("Cannot find user by username", e);
        }
    }

    /*
        Hàm findById dùng để tìm user theo id.
    */
    public User findById(String id) {
        String sql = "SELECT * FROM users WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, id);

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return mapResultSetToUser(resultSet);
            }

            return null;

        } catch (SQLException e) {
            throw new RuntimeException("Cannot find user by id", e);
        }
    }

    /*
        Hàm update dùng để cập nhật thông tin user trong database.

        Ví dụ dùng khi:
        - đổi email
        - đổi password
        - đổi username
        - đổi role
        - đổi balance

        Vì bây giờ 1 user có nhiều role nên:
        - thông tin chính cập nhật trong bảng users
        - role cập nhật trong bảng user_roles

        Cách làm đơn giản:
        1. UPDATE bảng users
        2. Xóa role cũ của user trong bảng user_roles
        3. Insert lại danh sách role mới
    */
    public void update(User user) {
        String sql = "UPDATE users SET username = ?, password = ?, email = ?, balance = ? WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, user.getUsername());
            statement.setString(2, user.getPassword());
            statement.setString(3, user.getEmail());
            statement.setDouble(4, user.getBalance());
            statement.setString(5, user.getId());

            statement.executeUpdate();

            /*
                Cập nhật lại roles.

                Vì role nằm ở bảng user_roles,
                nên ta xóa role cũ trước rồi lưu role mới sau.
            */
            deleteRolesByUserId(user.getId());
            saveRoles(user.getId(), user.getRoles());

        } catch (SQLException e) {
            throw new RuntimeException("Cannot update user", e);
        }
    }

    /*
        Hàm deleteById dùng để xóa user theo id.

        Vì user_roles có khóa ngoại user_id trỏ về users.id,
        nên nên xóa role của user trước,
        sau đó mới xóa user.
    */
    public boolean deleteById(String id) {
        try {
            /*
                Xóa các role của user trước.
            */
            deleteRolesByUserId(id);

            String sql = "DELETE FROM users WHERE id = ?";

            try (Connection connection = DatabaseConnection.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {

                // thay dau ? thu nhat bang id
                statement.setString(1, id);

                /*
                    Chỉ gọi executeUpdate 1 lần.
                    Code cũ của bạn đang gọi executeUpdate 2 lần,
                    như vậy dễ bị sai logic.
                */
                int affectedRows = statement.executeUpdate();

                return affectedRows > 0;
            }

        } catch (SQLException e) {
            throw new RuntimeException("Cannot delete user", e);
        }
    }

    /*
        Hàm deleteRolesByUserId dùng để xóa toàn bộ role của 1 user.

        Dùng khi:
        - update lại role
        - delete user
    */
    private void deleteRolesByUserId(String userId) {
        String sql = "DELETE FROM user_roles WHERE user_id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, userId);
            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Cannot delete user roles", e);
        }
    }

    /*
        Hàm findRolesByUserId dùng để lấy danh sách role của 1 user.

        Ví dụ bảng user_roles có:

        user_id     role
        U001        BIDDER
        U001        SELLER

        Thì hàm này trả về:
        Set<UserRole> gồm BIDDER và SELLER.
    */
    private Set<UserRole> findRolesByUserId(String userId) {
        String sql = "SELECT role FROM user_roles WHERE user_id = ?";

        Set<UserRole> roles = new HashSet<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, userId);

            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                String roleText = resultSet.getString("role");

                /*
                    Chuyển String role trong database thành enum UserRole.

                    "BIDDER" -> UserRole.BIDDER
                    "SELLER" -> UserRole.SELLER
                    "ADMIN"  -> UserRole.ADMIN
                */
                UserRole role = UserRole.valueOf(roleText);
                roles.add(role);
            }

            return roles;

        } catch (SQLException e) {
            throw new RuntimeException("Cannot find user roles", e);
        }
    }

    /*
        Hàm updateBalance dùng để cập nhật số dư của user.

        Dùng khi:
        - nạp tiền
        - rút tiền
        - đặt bid
        - thanh toán
    */
    public boolean updateBalance(String userId, double newBalance) {
        String sql = "UPDATE users SET balance = ? WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setDouble(1, newBalance);
            statement.setString(2, userId);

            int affectedRows = statement.executeUpdate();

            return affectedRows > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Cannot update balance", e);
        }
    }

    /*
        Hàm mapResultSetToUser dùng để chuyển dữ liệu từ database
        thành object User trong Java.

        Ví dụ database có dòng trong bảng users:

        id       username    password    email              balance
        U001     chuong      123456      chuong@gmail.com   5000

        Và bảng user_roles có:

        user_id     role
        U001        BIDDER
        U001        SELLER

        Hàm này sẽ tạo ra object:
        new User("chuong", "123456", "chuong@gmail.com", roles)

        Trong đó roles gồm:
        BIDDER, SELLER
    */
    private User mapResultSetToUser(ResultSet resultSet) throws SQLException {
        String id = resultSet.getString("id");
        String username = resultSet.getString("username");
        String password = resultSet.getString("password");
        String email = resultSet.getString("email");
        double balance = resultSet.getDouble("balance");

        /*
            Role không còn lấy từ cột role trong bảng users nữa.

            Code cũ:
            String roleText = resultSet.getString("role");
            UserRole role = UserRole.valueOf(roleText);

            Code mới:
            Lấy toàn bộ role từ bảng user_roles.
        */
        Set<UserRole> roles = findRolesByUserId(id);

        /*
            Nếu database không có role nào cho user này,
            thì dữ liệu đang bị lỗi vì mỗi user nên có ít nhất 1 role.
        */
        if (roles == null || roles.isEmpty()) {
            throw new RuntimeException("User has no role");
        }

        /*
            Vì User không còn là abstract class nữa,
            nên có thể tạo trực tiếp object User.
        */
        User user = new User(username, password, email, roles);

        /*
            Rất quan trọng:

            Khi new User, constructor sẽ tự sinh id mới.
            Nhưng user này đang lấy từ database ra,
            nên mình phải set lại id cũ trong database.
        */
        user.setId(id);
        user.setBalance(balance);

        return user;
    }

    /*
        Lấy tất cả user trong bảng users.
    */
    public List<User> findAll() {
        String sql = "SELECT * FROM users";

        List<User> users = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                User user = mapResultSetToUser(resultSet);
                users.add(user);
            }

            return users;

        } catch (SQLException e) {
            throw new RuntimeException("Cannot find all users", e);
        }
    }
}
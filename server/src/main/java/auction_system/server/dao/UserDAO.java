package auction_system.server.dao;

import auction_system.common.enums.UserRole;
import auction_system.server.model.User;

import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UserDAO {
    private static UserDAO instance;

    private UserDAO() {
    }

    public static UserDAO getInstance() {
        if (instance == null) {
            instance = new UserDAO();
        }
        return instance;
    }

    /*
        Hàm save dùng để lưu 1 User vào bảng users trong MySQL.

        Bảng users hiện tại:
        id INT AUTO_INCREMENT PRIMARY KEY
        fullname
        username
        password
        roles
        balance

        Không còn email.
        Không còn bảng user_roles.
    */
    public void save(User user) {
        String sql = "INSERT INTO users(fullname, username, password, roles, balance) VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, user.getFullname());
            statement.setString(2, user.getUsername());
            statement.setString(3, user.getPassword());
            statement.setString(4, rolesToString(user.getRoles()));
            statement.setBigDecimal(5, user.getBalance());

            statement.executeUpdate();

            // Lấy ID vừa được DB sinh ra
            ResultSet keys = statement.getGeneratedKeys();
            if (keys.next()) {
                user.setId(keys.getInt(1)); // ← gán ngược lại vào object
            }
        } catch (SQLException e) {
            throw new RuntimeException("Cannot save user", e);
        }
    }

    /*
        Chuyển Set<UserRole> thành String để lưu vào cột roles.

        Ví dụ:
        [BIDDER, SELLER] -> "BIDDER,SELLER"
    */
    private String rolesToString(Set<UserRole> roles) {
        StringBuilder result = new StringBuilder();

        for (UserRole role : roles) {
            if (!result.isEmpty()) {
                result.append(",");
            }

            result.append(role.name());
        }

        return result.toString();
    }

    /*
        Chuyển String trong database thành Set<UserRole>.

        Ví dụ:
        "BIDDER,SELLER" -> Set gồm BIDDER và SELLER
    */
    private Set<UserRole> stringToRoles(String rolesText) {
        Set<UserRole> roles = new HashSet<>();

        if (rolesText == null || rolesText.trim().isEmpty()) {
            return roles;
        }

        String[] parts = rolesText.split(",");

        for (String part : parts) {
            roles.add(UserRole.valueOf(part.trim()));
        }

        return roles;
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

            statement.setString(1, username);

            ResultSet resultSet = statement.executeQuery();

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
    public User findById(int id) {
        String sql = "SELECT * FROM users WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);

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

        Không còn email.
        roles nằm trực tiếp trong bảng users.
    */
    public void update(User user) {
        String sql = "UPDATE users SET fullname = ?, username = ?, password = ?, roles = ?, balance = ? WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, user.getFullname());
            statement.setString(2, user.getUsername());
            statement.setString(3, user.getPassword());
            statement.setString(4, rolesToString(user.getRoles()));
            statement.setBigDecimal(5, user.getBalance());
            statement.setInt(6, user.getId());

            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Cannot update user", e);
        }
    }

    /*
        Hàm deleteById dùng để xóa user theo id.

        Không còn bảng user_roles,
        nên chỉ cần xóa trong bảng users.
    */
    public boolean deleteById(int id) {
        String sql = "DELETE FROM users WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);

            int affectedRows = statement.executeUpdate();

            return affectedRows > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Cannot delete user", e);
        }
    }

    /*
        Hàm updateBalance dùng để cập nhật số dư của user.
    */
    public boolean updateBalance(int userId, BigDecimal newBalance) {
        String sql = "UPDATE users SET balance = ? WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setBigDecimal(1, newBalance);
            statement.setInt(2, userId);

            int affectedRows = statement.executeUpdate();

            return affectedRows > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Cannot update balance", e);
        }
    }

    /*
        Hàm mapResultSetToUser dùng để chuyển dữ liệu từ database
        thành object User trong Java.

        Ví dụ database có dòng:

        id    fullname        username    password    roles           balance
        1     Nguyen Van A    chuong      123456      BIDDER,SELLER   5000

        Hàm này sẽ tạo:
        new User("Nguyen Van A", "chuong", "123456", roles)
    */
    private User mapResultSetToUser(ResultSet resultSet) throws SQLException {
        int id = resultSet.getInt("id");
        String fullname = resultSet.getString("fullname");
        String username = resultSet.getString("username");
        String password = resultSet.getString("password");
        String rolesText = resultSet.getString("roles");
        BigDecimal balance = resultSet.getBigDecimal("balance");

        Set<UserRole> roles = stringToRoles(rolesText);

        if (roles.isEmpty()) {
            throw new RuntimeException("User has no role");
        }

        User user = new User(fullname, username, password, roles);

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
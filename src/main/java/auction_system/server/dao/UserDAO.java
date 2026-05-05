package auction_system.server.dao;

import auction_system.model.Admin;
import auction_system.model.Bidder;
import auction_system.model.Seller;
import auction_system.model.User;
import auction_system.model.UserRole;
import java.util.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {

    /*
        Hàm save dùng để lưu 1 User vào bảng users trong MySQL.

        Bảng users của bạn đang có các cột:
        id, username, password, email, role
    */
    public void save(User user) {
        String sql = "INSERT INTO users(id, username, password, email, role, balance) VALUES (?, ?, ?, ?, ?, ?)";
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
                ? thứ 5 -> role
            */
            statement.setString(1, user.getId());
            statement.setString(2, user.getUsername());
            statement.setString(3, user.getPassword());
            statement.setString(4, user.getEmail());
            statement.setDouble(6, user.getBalance());

            /*
                user.getRole() là enum UserRole.
                .name() chuyển enum thành String.

                Ví dụ:
                UserRole.BIDDER.name() -> "BIDDER"
            */
            statement.setString(5, user.getRole().name());

            /*
                executeUpdate dùng cho các lệnh:
                INSERT, UPDATE, DELETE.
            */
            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Cannot save user", e);
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
    */
    public void update(User user) {
        String sql = "UPDATE users SET username = ?, password = ?, email = ?, role = ? WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, user.getUsername());
            statement.setString(2, user.getPassword());
            statement.setString(3, user.getEmail());
            statement.setString(4, user.getRole().name());
            statement.setString(5, user.getId());

            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Cannot update user", e);
        }
    }

    /*
        Hàm deleteById dùng để xóa user theo id.
    */
    public boolean deleteById(String id) {
        String sql = "DELETE FROM users WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            //thay dau ? thu nhat bang id
            statement.setString(1, id);

            statement.executeUpdate();
            int affectedRows=statement.executeUpdate();
            return affectedRows>0;

        } catch (SQLException e) {
            throw new RuntimeException("Cannot delete user", e);
        }
    }

    /*
        Hàm mapResultSetToUser dùng để chuyển dữ liệu từ database
        thành object User trong Java.

        Ví dụ database có dòng:

        id       username    password    email              role
        U001     chuong      123456      chuong@gmail.com   BIDDER

        Hàm này sẽ tạo ra object:
        new Bidder("chuong", "123456", "chuong@gmail.com")
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

    private User mapResultSetToUser(ResultSet resultSet) throws SQLException {
        String id = resultSet.getString("id");
        String username = resultSet.getString("username");
        String password = resultSet.getString("password");
        String email = resultSet.getString("email");
        String roleText = resultSet.getString("role");
        double balance = resultSet.getDouble("balance");
        /*
            Chuyển String role trong database thành enum UserRole.

            "BIDDER" -> UserRole.BIDDER
            "SELLER" -> UserRole.SELLER
            "ADMIN"  -> UserRole.ADMIN
        */
        UserRole role = UserRole.valueOf(roleText);

        User user;

        /*
            Vì User là abstract class nên không thể:
            new User(...)

            Phải kiểm tra role rồi tạo object con tương ứng.
        */
        if (role == UserRole.BIDDER) {
            user = new Bidder(username, password, email);
        } else if (role == UserRole.SELLER) {
            user = new Seller(username, password, email);
        } else if (role == UserRole.ADMIN) {
            user = new Admin(username, password, email);
        } else {
            throw new RuntimeException("Invalid user role");
        }

        /*
            Rất quan trọng:

            Khi new Bidder/Seller/Admin, constructor sẽ tự sinh id mới.
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
package auction_system.server.dao;

import auction_system.server.model.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class ItemDAO {

    private UserDAO userDAO;

    public ItemDAO() {
        this.userDAO = new UserDAO();
    }

    /*
        Lưu item vào bảng items: name, description, owner_id, type, starting_time, ending_time.
    */
    public void save(Item item) {
        // CẬP NHẬT: Chỉ còn 6 dấu ? tương ứng với các trường cốt lõi
        String sql = "INSERT INTO items(name, description, owner_id, type, starting_time, ending_time) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            if (item.getOwner() == null) {
                throw new RuntimeException("Owner cannot be null");
            }

            if (!item.getOwner().hasRole(UserRole.SELLER)) {
                throw new RuntimeException("Owner must have SELLER role");
            }

            statement.setString(1, item.getName());
            statement.setString(2, item.getDescription());
            statement.setInt(3, Integer.parseInt(item.getOwner().getId()));
            statement.setString(4, item.getType().name());

            // Lưu thời gian đấu giá
            statement.setTimestamp(5, Timestamp.valueOf(item.getStartingTime()));
            statement.setTimestamp(6, Timestamp.valueOf(item.getEndingTime()));

            statement.executeUpdate();

            ResultSet generatedKeys = statement.getGeneratedKeys();
            if (generatedKeys.next()) {
                int generatedId = generatedKeys.getInt(1);
                item.setId(String.valueOf(generatedId));
            } else {
                throw new RuntimeException("Cannot get generated item id");
            }

        } catch (SQLException e) {
            throw new RuntimeException("Cannot save item", e);
        }
    }

    public Item findById(String id) {
        String sql = "SELECT * FROM items WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, Integer.parseInt(id));
            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return mapResultSetToItem(resultSet);
            }
            return null;

        } catch (SQLException e) {
            throw new RuntimeException("Cannot find item by id", e);
        }
    }

    private Item mapResultSetToItem(ResultSet resultSet) throws SQLException {
        String id = String.valueOf(resultSet.getInt("id"));
        String name = resultSet.getString("name");
        String description = resultSet.getString("description");
        String ownerId = String.valueOf(resultSet.getInt("owner_id"));
        String typeText = resultSet.getString("type");

        // Lấy dữ liệu thời gian
        LocalDateTime startingTime = resultSet.getTimestamp("starting_time").toLocalDateTime();
        LocalDateTime endingTime = resultSet.getTimestamp("ending_time").toLocalDateTime();

        User owner = userDAO.findById(ownerId);
        if (owner == null) {
            throw new RuntimeException("Owner not found");
        }

        ItemType type = ItemType.valueOf(typeText);
        Item item;

        /*
            LƯU Ý: Vì DB không còn lưu brand, year, model...
            nên khi khởi tạo các object con, mình sẽ truyền giá trị mặc định (null hoặc 0).
        */
        if (type == ItemType.ELECTRONICS) {
            item = new Electronics(name, description, owner, startingTime, endingTime);
        } else if (type == ItemType.ART) {
            item = new Art(name, description, owner, startingTime, endingTime);
        } else if (type == ItemType.VEHICLE) {
            item = new Vehicle(name, description, owner, startingTime, endingTime);
        } else {
            throw new RuntimeException("Invalid item type");
        }

        item.setId(id);
        return item;
    }
}
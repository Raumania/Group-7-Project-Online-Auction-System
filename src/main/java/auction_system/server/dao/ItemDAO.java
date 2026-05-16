package auction_system.server.dao;

import auction_system.server.model.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;

public class ItemDAO {

    private final UserDAO userDAO;

    public ItemDAO() {
        this.userDAO = new UserDAO();
    }

    /*
        Dùng khi tạo auction trước, sau đó lưu item.
        items.id = auctions.id
    */
    public void saveForAuction(Connection connection, String auctionId, Item item) {
        String sql = """
                INSERT INTO items(id, name, description, type)
                VALUES (?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, Integer.parseInt(auctionId));
            statement.setString(2, item.getName());
            statement.setString(3, item.getDescription());
            statement.setString(4, item.getType().name());

            statement.executeUpdate();

            item.setId(auctionId);

        } catch (SQLException e) {
            throw new RuntimeException("Cannot save item for auction", e);
        }
    }

    /*
        Vì items.id = auctions.id,
        nên tìm item theo auctionId.

        Phải JOIN auctions để lấy:
        - seller_id => owner
        - starting_time
        - ending_time
    */
    public Item findById(String id) {
        String sql = """
                SELECT 
                    i.id,
                    i.name,
                    i.description,
                    i.type,
                    a.seller_id,
                    a.starting_time,
                    a.ending_time
                FROM items i
                JOIN auctions a ON i.id = a.id
                WHERE i.id = ?
                """;

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
        String typeText = resultSet.getString("type");

        String sellerId = String.valueOf(resultSet.getInt("seller_id"));

        LocalDateTime startTime = resultSet
                .getTimestamp("starting_time")
                .toLocalDateTime();

        LocalDateTime endTime = resultSet
                .getTimestamp("ending_time")
                .toLocalDateTime();

        User owner = userDAO.findById(sellerId);

        if (owner == null) {
            throw new RuntimeException("Owner not found for item id = " + id);
        }

        if (!owner.hasRole(UserRole.SELLER)) {
            throw new RuntimeException("Owner must have SELLER role");
        }

        ItemType type = ItemType.valueOf(typeText);

        Item item;

        if (type == ItemType.ELECTRONICS) {
            item = new Electronics(name, description, startTime, endTime);
        } else if (type == ItemType.ART) {
            item = new Art(name, description, startTime, endTime);
        } else if (type == ItemType.VEHICLE) {
            item = new Vehicle(name, description,  startTime, endTime);
        } else {
            throw new RuntimeException("Invalid item type: " + typeText);
        }

        item.setId(id);

        return item;
    }
}
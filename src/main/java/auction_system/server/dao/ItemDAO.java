package auction_system.server.dao;

import auction_system.common.enums.ItemType;
import auction_system.common.enums.UserRole;
import auction_system.server.model.*;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ItemDAO {

    private final UserDAO userDAO;

    public ItemDAO() {
        this.userDAO = new UserDAO();
    }

    public void save(Auction auction, int id) {
        String sql = "INSERT INTO items(id, name, description, type) VALUES (?, ?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);
            statement.setString(2, auction.getName());
            statement.setString(3, auction.getDescription());
            statement.setString(4, auction.getType().name());

            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Cannot save item for auction", e);
        }
    }

    public void update(Auction auction) {
        String sql = "UPDATE items SET name = ?, description = ?, type = ? WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, auction.getName());
            statement.setString(2, auction.getDescription());
            statement.setString(3, auction.getType().name());
            statement.setInt(4, auction.getId());

            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Cannot update item", e);
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM items WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);

            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Cannot delete item", e);
        }
    }

    public Item findById(int id) {
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

            statement.setInt(1, id);

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return mapResultSetToItem(resultSet);
            }

            return null;

        } catch (SQLException e) {
            throw new RuntimeException("Cannot find item by id", e);
        }
    }

    public List<Item> findByType(ItemType type) {
        List<Item> items = new ArrayList<>();

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
                WHERE i.type = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, type.name());

            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                Item item = mapResultSetToItem(resultSet);
                items.add(item);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Cannot find items by type", e);
        }

        return items;
    }
    public List<Item> findByItemName(String name) {
        List<Item> items = new ArrayList<>();

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
                WHERE i.name = ?
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, name);
            ResultSet resultSet= statement.executeQuery();
            while (resultSet.next()) {
                Item item = mapResultSetToItem(resultSet);
                items.add(item);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Cannot find items by name", e);
        }
        return items;
    }

    private Item mapResultSetToItem(ResultSet resultSet) throws SQLException {
        int id = resultSet.getInt("id");
        String name = resultSet.getString("name");
        String description = resultSet.getString("description");
        String typeText = resultSet.getString("type");

        int sellerId = resultSet.getInt("seller_id");

        Timestamp startTimestamp = resultSet.getTimestamp("starting_time");
        Timestamp endTimestamp = resultSet.getTimestamp("ending_time");

        LocalDateTime startTime = null;
        LocalDateTime endTime = null;

        if (startTimestamp != null) {
            startTime = startTimestamp.toLocalDateTime();
        }

        if (endTimestamp != null) {
            endTime = endTimestamp.toLocalDateTime();
        }

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
            item = new Vehicle(name, description, startTime, endTime);
        } else {
            throw new RuntimeException("Invalid item type: " + typeText);
        }

        item.setId(id);

        return item;
    }
}
package auction_system.server.dao;

import auction_system.server.model.Art;
import auction_system.server.model.Electronics;
import auction_system.server.model.Item;
import auction_system.server.model.ItemType;
import auction_system.server.model.Seller;
import auction_system.server.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ItemDAO {

    private UserDAO userDAO;

    public ItemDAO() {
        this.userDAO = new UserDAO();
    }

    /*
        Lưu item vào bảng items.

        Vì Item là abstract, object thật truyền vào sẽ là:
        - Electronics
        - Art
        - Vehicle
    */
    public void save(Item item) {
        String sql = "INSERT INTO items(id, name, description, starting_price, owner_id, type, brand, model, artist, year) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, item.getId());
            statement.setString(2, item.getName());
            statement.setString(3, item.getDescription());
            statement.setDouble(4, item.getStartingPrice());
            statement.setString(5, item.getOwner().getId());
            statement.setString(6, item.getType().name());

            /*
                Cột phụ tùy theo loại item.
            */
            if (item instanceof Electronics electronics) {
                statement.setString(7, electronics.getBrand());
                statement.setString(8, electronics.getModel());
                statement.setString(9, null);
                statement.setObject(10, null);
            } else if (item instanceof Art art) {
                statement.setString(7, null);
                statement.setString(8, null);
                statement.setString(9, art.getArtist());
                statement.setInt(10, art.getYear());
            } else if (item instanceof auction_system.server.model.Vehicle vehicle) {
                statement.setString(7, vehicle.getBrand());
                statement.setString(8, null);
                statement.setString(9, null);
                statement.setInt(10, vehicle.getYear());
            } else {
                throw new RuntimeException("Invalid item type");
            }

            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Cannot save item", e);
        }
    }

    /*
        Tìm item theo id.
    */
    public Item findById(String id) {
        String sql = "SELECT * FROM items WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, id);

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return mapResultSetToItem(resultSet);
            }

            return null;

        } catch (SQLException e) {
            throw new RuntimeException("Cannot find item by id", e);
        }
    }

    /*
        Chuyển 1 dòng trong bảng items thành object Item.
    */
    private Item mapResultSetToItem(ResultSet resultSet) throws SQLException {
        String id = resultSet.getString("id");
        String name = resultSet.getString("name");
        String description = resultSet.getString("description");
        double startingPrice = resultSet.getDouble("starting_price");
        String ownerId = resultSet.getString("owner_id");
        String typeText = resultSet.getString("type");

        String brand = resultSet.getString("brand");
        String model = resultSet.getString("model");
        String artist = resultSet.getString("artist");
        int year = resultSet.getInt("year");

        /*
            Lấy owner từ bảng users.
        */
        User user = userDAO.findById(ownerId);

        if (user == null) {
            throw new RuntimeException("Owner not found");
        }

        if (!(user instanceof Seller)) {
            throw new RuntimeException("Owner is not seller");
        }

        Seller owner = (Seller) user;

        ItemType type = ItemType.valueOf(typeText);

        Item item;

        if (type == ItemType.ELECTRONICS) {
            item = new Electronics(name, description, startingPrice, owner, brand, model);
        } else if (type == ItemType.ART) {
            item = new Art(name, description, startingPrice, owner, artist, year);
        } else if (type == ItemType.VEHICLE) {
            item = new auction_system.server.model.Vehicle(name, description, startingPrice, owner, brand, year);
        } else {
            throw new RuntimeException("Invalid item type");
        }

        /*
            Constructor tạo id mới, nên phải set lại id cũ trong database.
        */
        item.setId(id);

        return item;
    }
}
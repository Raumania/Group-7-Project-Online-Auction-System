package auction_system.server.dao;

import auction_system.server.model.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

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

        Lưu ý mới:
        - owner không còn bắt buộc là object Seller nữa
        - owner bây giờ là User có role SELLER
        - item id bây giờ do MySQL AUTO_INCREMENT sinh ra
        - sau khi insert xong, ItemDAO lấy id vừa sinh rồi set lại vào object item
    */
    public void save(Item item) {
        String sql = "INSERT INTO items(name, description, starting_price, owner_id, type, brand, model, artist, year) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            /*
                Kiểm tra owner phải có role SELLER.
                Vì bây giờ không dùng instanceof Seller nữa.
            */
            if (item.getOwner() == null) {
                throw new RuntimeException("Owner cannot be null");
            }

            if (!item.getOwner().hasRole(UserRole.SELLER)) {
                throw new RuntimeException("Owner must have SELLER role");
            }

            /*
                Gán dữ liệu vào từng dấu ? trong câu SQL.

                ? thứ 1 -> name
                ? thứ 2 -> description
                ? thứ 3 -> starting_price
                ? thứ 4 -> owner_id
                ? thứ 5 -> type
                ? thứ 6 -> brand
                ? thứ 7 -> model
                ? thứ 8 -> artist
                ? thứ 9 -> year
            */
            statement.setString(1, item.getName());
            statement.setString(2, item.getDescription());
            statement.setDouble(3, item.getStartingPrice());

            /*
                owner_id trong database là INT vì users.id là INT AUTO_INCREMENT.
                Nhưng Entity.id trong Java vẫn là String nên parse sang int.
            */
            statement.setInt(4, Integer.parseInt(item.getOwner().getId()));

            statement.setString(5, item.getType().name());

            /*
                Cột phụ tùy theo loại item.
            */
            if (item instanceof Electronics electronics) {
                statement.setString(6, electronics.getBrand());
                statement.setString(7, electronics.getModel());
                statement.setString(8, null);
                statement.setObject(9, null);
            } else if (item instanceof Art art) {
                statement.setString(6, null);
                statement.setString(7, null);
                statement.setString(8, art.getArtist());
                statement.setInt(9, art.getYear());
            } else if (item instanceof auction_system.server.model.Vehicle vehicle) {
                statement.setString(6, vehicle.getBrand());
                statement.setString(7, null);
                statement.setString(8, null);
                statement.setInt(9, vehicle.getYear());
            } else {
                throw new RuntimeException("Invalid item type");
            }

            statement.executeUpdate();

            /*
                Lấy id AUTO_INCREMENT mà MySQL vừa sinh cho item.
                Entity.id trong Java đang là String nên convert int -> String.
            */
            ResultSet generatedKeys = statement.getGeneratedKeys();

            if (generatedKeys.next()) {
                int generatedId = generatedKeys.getInt(1);
                item.setId(String.valueOf(generatedId));
            } else {
                throw new RuntimeException("Cannot get generated item id");
            }

        }  catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Cannot save item: " + e.getMessage(), e);
    }}

    /*
        Tìm item theo id.

        Vì items.id trong MySQL là INT AUTO_INCREMENT,
        nhưng Entity.id trong Java đang là String,
        nên phải parse String -> int khi query.
    */
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

    /*
        Chuyển 1 dòng trong bảng items thành object Item.
    */
    private Item mapResultSetToItem(ResultSet resultSet) throws SQLException {
        /*
            items.id là INT trong database,
            Entity.id là String trong Java,
            nên convert int -> String.
        */
        String id = String.valueOf(resultSet.getInt("id"));

        String name = resultSet.getString("name");
        String description = resultSet.getString("description");
        double startingPrice = resultSet.getDouble("starting_price");

        /*
            owner_id là INT trong database,
            UserDAO.findById đang nhận String,
            nên convert int -> String.
        */
        String ownerId = String.valueOf(resultSet.getInt("owner_id"));

        String typeText = resultSet.getString("type");

        String brand = resultSet.getString("brand");
        String model = resultSet.getString("model");
        String artist = resultSet.getString("artist");
        int year = resultSet.getInt("year");

        /*
            Lấy owner từ bảng users.

            Trước đây:
            - lấy User
            - kiểm tra instanceof Seller
            - ép kiểu Seller

            Bây giờ:
            - lấy User
            - kiểm tra user có role SELLER không
            - không ép kiểu Seller nữa
        */
        User owner = userDAO.findById(ownerId);

        if (owner == null) {
            throw new RuntimeException("Owner not found");
        }

        if (!owner.hasRole(UserRole.SELLER)) {
            throw new RuntimeException("Owner is not seller");
        }

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
            Constructor không tạo id nữa.
            Nhưng item này đang lấy từ database ra,
            nên mình set lại id thật trong database.
        */
        item.setId(id);

        return item;
    }
}
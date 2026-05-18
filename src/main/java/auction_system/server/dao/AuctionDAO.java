package auction_system.server.dao;

import auction_system.common.enums.AuctionStatus;
import auction_system.common.enums.ItemType;
import auction_system.server.model.Auction;
import auction_system.server.observer.AuctionScheduler;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AuctionDAO {
    private static AuctionDAO instance;

    private AuctionDAO() {
    }

    public static AuctionDAO getInstance() {
        if (instance == null) {
            instance = new AuctionDAO();
        }
        return instance;
    }

    /*
        Hàm save bình thường.
        Dùng khi bạn chỉ muốn lưu auction riêng lẻ.
    */
    public int save(Auction auction) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return save(connection, auction);
        } catch (SQLException e) {
            throw new RuntimeException("Cannot save auction", e);
        }
    }

    /*
        Hàm save dùng chung connection.
        Dùng trong transaction ở AuctionService.createAuction().
    */
    public int save(Connection connection, Auction auction) {
        String sql = """
                INSERT INTO auctions
                (seller_id, starting_price, current_price, highest_bidder_id, status, starting_time, ending_time)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            statement.setInt(1, auction.getSellerId());
            statement.setDouble(2, auction.getStartingPrice());
            statement.setDouble(3, auction.getCurrentPrice());

            if (auction.getHighestBidderId() == null) {
                statement.setNull(4, Types.INTEGER);
            } else {
                statement.setInt(4, auction.getHighestBidderId());
            }

            statement.setString(5, auction.getStatus().name());
            statement.setTimestamp(6, Timestamp.valueOf(auction.getStartTime()));
            statement.setTimestamp(7, Timestamp.valueOf(auction.getEndTime()));

            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int generatedId = generatedKeys.getInt(1);
                    auction.setId(generatedId);
                    return generatedId;
                }

                throw new SQLException("Creating auction failed, no ID obtained.");
            }

        } catch (SQLException e) {
            throw new RuntimeException("Cannot save auction", e);
        }
    }

    /*
        Tìm auction bình thường.
    */
    public Auction findById(int id) {
        String sql = "SELECT * FROM auctions WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return mapResultSetToAuction(resultSet);
            }

            return null;

        } catch (SQLException e) {
            throw new RuntimeException("Cannot find auction by id", e);
        }
    }
    /*
        Tìm auction và khóa dòng trong database.
        Chỉ dùng trong transaction:
        - placeBid()
        - closeAuction()
        - editAuction()
        - deleteAuction()
       */
    public Auction findByIdForUpdate(Connection connection, int id) {
        String sql = "SELECT * FROM auctions WHERE id = ? FOR UPDATE";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return mapResultSetToAuction(resultSet);
            }

            return null;

        } catch (SQLException e) {
            throw new RuntimeException("Cannot find auction by id for update", e);
        }
    }

    public List<Auction> getAllOpenAuctions() {
        String sql = "SELECT * FROM auctions WHERE status IN (?, ?)";

        List<Auction> auctions = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, AuctionStatus.OPEN.name());
            statement.setString(2, AuctionStatus.RUNNING.name());

            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                auctions.add(mapResultSetToAuction(resultSet));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Cannot find all open auctions", e);
        }

        return auctions;
    }

    public List<Auction> findAll() {
        String sql = "SELECT * FROM auctions";

        List<Auction> auctions = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                auctions.add(mapResultSetToAuction(resultSet));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Cannot find all auctions", e);
        }

        return auctions;
    }

    public List<Auction> findAllBySellerId(int sellerId) {
        String sql = """
                SELECT
                    a.id,
                    a.seller_id,
                    i.name,
                    i.description,
                    i.type,
                    a.status,
                    a.starting_price,
                    a.current_price,
                    a.highest_bidder_id,
                    a.starting_time,
                    a.ending_time
                FROM auctions a
                INNER JOIN items i ON a.id = i.id
                WHERE a.seller_id = ?
                """;

        List<Auction> auctions = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, sellerId);

            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                auctions.add(mapResultSetToAuctionWithItem(resultSet));
            }

        } catch (SQLException e) {
            throw new RuntimeException("Cannot find auctions for seller ID: " + sellerId, e);
        }

        return auctions;
    }

    /*
        Update bình thường.
        Không dùng transaction bên ngoài.
    */
    public void update(Auction auction) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            update(connection, auction);
        } catch (SQLException e) {
            throw new RuntimeException("Cannot update auction", e);
        }
    }

    /*
        Update dùng chung connection.
        Dùng trong transaction ở Service.
    */
    public void update(Connection connection, Auction auction) {
        String sql = """
                UPDATE auctions
                SET seller_id = ?,
                    status = ?,
                    starting_price = ?,
                    current_price = ?,
                    highest_bidder_id = ?,
                    starting_time = ?,
                    ending_time = ?
                WHERE id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, auction.getSellerId());
            statement.setString(2, auction.getStatus().name());
            statement.setDouble(3, auction.getStartingPrice());
            statement.setDouble(4, auction.getCurrentPrice());

            if (auction.getHighestBidderId() == null) {
                statement.setNull(5, Types.INTEGER);
            } else {
                statement.setInt(5, auction.getHighestBidderId());
            }

            statement.setTimestamp(6, Timestamp.valueOf(auction.getStartTime()));
            statement.setTimestamp(7, Timestamp.valueOf(auction.getEndTime()));
            statement.setInt(8, auction.getId());

            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Cannot update auction", e);
        }
    }

    /*
        Delete bình thường.
    */
    public void delete(int id) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            delete(connection, id);
        } catch (SQLException e) {
            throw new RuntimeException("Cannot delete auction", e);
        }
    }

    /*
        Delete dùng chung connection.
        Dùng trong transaction ở Service.
    */
    public void delete(Connection connection, int id) {
        String sql = "DELETE FROM auctions WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);
            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Cannot delete auction", e);
        }
    }

    public List<Auction> findbystatus(String status) {
        String sql = "SELECT * FROM auctions where status = ?";
        List<Auction> auctions = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                auctions.add(mapResultSetToAuction(resultSet));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Cannot find all auctions", e);
        }
        return auctions;
    }
    private Auction mapResultSetToAuction(ResultSet resultSet) throws SQLException {
        Auction auction = new Auction();

        auction.setId(resultSet.getInt("id"));
        auction.setSellerId(resultSet.getInt("seller_id"));
        auction.setStartingPrice(resultSet.getDouble("starting_price"));
        auction.setCurrentPrice(resultSet.getDouble("current_price"));

        auction.setHighestBidderId(resultSet.getObject("highest_bidder_id", Integer.class));

        String statusStr = resultSet.getString("status");
        if (statusStr != null) {
            auction.setStatus(AuctionStatus.valueOf(statusStr.toUpperCase()));
        }

        Timestamp startTimestamp = resultSet.getTimestamp("starting_time");
        if (startTimestamp != null) {
            auction.setStartTime(startTimestamp.toLocalDateTime());
        }

        Timestamp endTimestamp = resultSet.getTimestamp("ending_time");
        if (endTimestamp != null) {
            auction.setEndTime(endTimestamp.toLocalDateTime());
        }

        return auction;
    }

    private Auction mapResultSetToAuctionWithItem(ResultSet resultSet) throws SQLException {
        Auction auction = new Auction();

        auction.setId(resultSet.getInt("id"));
        auction.setSellerId(resultSet.getInt("seller_id"));
        auction.setName(resultSet.getString("name"));
        auction.setDescription(resultSet.getString("description"));
        auction.setStartingPrice(resultSet.getDouble("starting_price"));
        auction.setCurrentPrice(resultSet.getDouble("current_price"));

        auction.setHighestBidderId(resultSet.getObject("highest_bidder_id", Integer.class));

        String typeStr = resultSet.getString("type");
        if (typeStr != null) {
            auction.setType(ItemType.valueOf(typeStr.toUpperCase()));
        }

        String statusStr = resultSet.getString("status");
        if (statusStr != null) {
            auction.setStatus(AuctionStatus.valueOf(statusStr.toUpperCase()));
        }

        Timestamp startTimestamp = resultSet.getTimestamp("starting_time");
        if (startTimestamp != null) {
            auction.setStartTime(startTimestamp.toLocalDateTime());
        }

        Timestamp endTimestamp = resultSet.getTimestamp("ending_time");
        if (endTimestamp != null) {
            auction.setEndTime(endTimestamp.toLocalDateTime());
        }

        return auction;
    }
}
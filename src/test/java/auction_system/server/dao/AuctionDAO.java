package auction_system.server.dao;

import auction_system.common.enums.AuctionStatus;
import auction_system.common.enums.ItemType;
import auction_system.server.exception.daoException.DeletingException;
import auction_system.server.exception.daoException.FindingException;
import auction_system.server.exception.daoException.SavingException;
import auction_system.server.exception.daoException.UpdatingException;
import auction_system.server.model.Auction;

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
        Hàm save dùng chung connection.
        Dùng trong transaction ở AuctionService.createAuction().
    */
    public int save(Connection connection,Auction auction,String path){

        String sql = """
                INSERT INTO auctions
                (seller_id, starting_price, current_price, highest_bidder_id, highest_bidder_username, status, starting_time, ending_time, path)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?,?)
                """;
        try (
                PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)){
            statement.setInt(1, auction.getSellerId());
            statement.setDouble(2, auction.getStartingPrice());
            statement.setDouble(3, auction.getCurrentPrice());

            if (auction.getHighestBidderId() == null) {
                statement.setNull(4, Types.INTEGER);
                statement.setNull(5, Types.VARCHAR);
            } else {
                statement.setInt(4, auction.getHighestBidderId());
                statement.setString(5, auction.getHighestBidderUsername());
            }

            statement.setString(6, auction.getStatus().name());
            statement.setTimestamp(7, Timestamp.valueOf(auction.getStartTime()));
            statement.setTimestamp(8, Timestamp.valueOf(auction.getEndTime()));
            statement.setString(9, path);

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
            throw new SavingException("Cannot save auction");
        }
    }
    public int save(Connection connection,Auction auction) {
        String sql = """
                INSERT INTO auctions
                (seller_id, starting_price, current_price, highest_bidder_id, highest_bidder_username, status, starting_time, ending_time)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)){
            statement.setInt(1, auction.getSellerId());
            statement.setDouble(2, auction.getStartingPrice());
            statement.setDouble(3, auction.getCurrentPrice());

            if (auction.getHighestBidderId() == null) {
                statement.setNull(4, Types.INTEGER);
                statement.setNull(5, Types.VARCHAR);
            } else {
                statement.setInt(4, auction.getHighestBidderId());
                statement.setString(5, auction.getHighestBidderUsername());
            }

            statement.setString(6, auction.getStatus().name());
            statement.setTimestamp(7, Timestamp.valueOf(auction.getStartTime()));
            statement.setTimestamp(8, Timestamp.valueOf(auction.getEndTime()));

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
            throw new SavingException("Cannot save auction");
        }
    }

    /*
        Tìm auction bình thường.
    */
    public Auction findById(int id) {
        String sql = "SELECT a.*, u.username as highest_bidder_username FROM auctions a LEFT JOIN users u ON a.highest_bidder_id = u.id WHERE a.id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return mapResultSetToAuction(resultSet);
            }

            return null;

        } catch (SQLException e) {
            throw new FindingException("Cannot find auction by id");
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
        String sql = "SELECT a.*, u.username as highest_bidder_username FROM auctions a LEFT JOIN users u ON a.highest_bidder_id = u.id WHERE a.id = ? FOR UPDATE";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return mapResultSetToAuction(resultSet);
            }

            return null;

        } catch (SQLException e) {
            throw new FindingException("Cannot find auction by id for update");
        }
    }

    public List<Auction> findAllOpenAuctions() {
        String sql = "SELECT a.*, u.username as highest_bidder_username FROM auctions a LEFT JOIN users u ON a.highest_bidder_id = u.id WHERE status IN (?, ?)";

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
            throw new FindingException("Cannot find all open auctions");
        }

        return auctions;
    }

    public List<Auction> findAll() {
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
                u.username AS highest_bidder_username,
                a.starting_time,
                a.ending_time
            FROM auctions a
            INNER JOIN items i ON a.id = i.id
            LEFT JOIN users u ON a.highest_bidder_id = u.id
            """;

        List<Auction> auctions = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                auctions.add(mapResultSetToAuctionWithItem(resultSet));
            }

        } catch (SQLException e) {
            throw new FindingException("Cannot find all auctions");
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
                u.username AS highest_bidder_username,
                a.starting_time,
                a.ending_time
            FROM auctions a
            INNER JOIN items i ON a.id = i.id
            LEFT JOIN users u ON a.highest_bidder_id = u.id
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
            throw new FindingException("Cannot find auctions for seller ID: " + sellerId);
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
            throw new UpdatingException("Cannot update auction");
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
                    highest_bidder_username = ?,
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
                statement.setNull(6, Types.VARCHAR);
            } else {
                statement.setInt(5, auction.getHighestBidderId());
                statement.setString(6, auction.getHighestBidderUsername());
            }

            statement.setTimestamp(7, Timestamp.valueOf(auction.getStartTime()));
            statement.setTimestamp(8, Timestamp.valueOf(auction.getEndTime()));
            statement.setInt(9, auction.getId());

            statement.executeUpdate();

        } catch (SQLException e) {
            throw new UpdatingException("Cannot update auction");
        }
    }

    /*
        Delete bình thường.
    */
    public void delete(int id) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            delete(connection, id);
        } catch (SQLException e) {
            throw new DeletingException("Cannot delete auction");
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
            throw new DeletingException("Cannot delete auction");
        }
    }

    public List<Auction> findbystatus(String status) {
        String sql = "SELECT a.*, u.username as highest_bidder_username FROM auctions a LEFT JOIN users u ON a.highest_bidder_id = u.id where status = ?";
        List<Auction> auctions = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                auctions.add(mapResultSetToAuction(resultSet));
            }
        } catch (SQLException e) {
            throw new FindingException("Cannot find all auctions");
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
        auction.setHighestBidderUsername(resultSet.getString("highest_bidder_username"));

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
        auction.setHighestBidderUsername(resultSet.getString("highest_bidder_username"));
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
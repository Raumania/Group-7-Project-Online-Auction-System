package auction_system.server.dao;

import auction_system.common.enums.AuctionStatus;
import auction_system.common.enums.ItemType;
import auction_system.server.exception.daoException.deletingException;
import auction_system.server.exception.daoException.findingException;
import auction_system.server.exception.daoException.savingException;
import auction_system.server.exception.daoException.updatingException;
import auction_system.server.model.Auction;
import auction_system.server.service.ImageService;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuctionDAO {
    private static AuctionDAO instance;
    private final ImageService imageService = ImageService.getInstance();

    private AuctionDAO() {
    }

    public static AuctionDAO getInstance() {
        if (instance == null) {
            instance = new AuctionDAO();
        }
        return instance;
    }

    public int save(Connection connection, Auction auction, int itemId, String path) throws SQLException {
        String sql = """
                INSERT INTO auctions
                (item_id, seller_id, starting_price, current_price, highest_bidder_id, highest_bidder_username, status, starting_time, ending_time, path)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, itemId);
            statement.setInt(2, auction.getSellerId());
            statement.setBigDecimal(3, auction.getStartingPrice());
            
            if (auction.getCurrentPrice() == null) {
                statement.setNull(4, Types.DECIMAL);
            } else {
                statement.setBigDecimal(4, auction.getCurrentPrice());
            }

            if (auction.getHighestBidderId() == null) {
                statement.setNull(5, Types.INTEGER);
                statement.setNull(6, Types.VARCHAR);
            } else {
                statement.setInt(5, auction.getHighestBidderId());
                statement.setString(6, auction.getHighestBidderUsername());
            }

            statement.setString(7, auction.getStatus().name());
            statement.setTimestamp(8, Timestamp.valueOf(auction.getStartTime()));
            statement.setTimestamp(9, Timestamp.valueOf(auction.getEndTime()));
            statement.setString(10, path);

            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int generatedId = generatedKeys.getInt(1);
                    auction.setId(generatedId);
                    return generatedId;
                }
                throw new SQLException("Creating auction failed, no ID obtained.");
            }
        }
    }
    
    public int save(Connection connection, Auction auction, int itemId) throws SQLException {
        return save(connection, auction, itemId, null);
    }
    public LocalDateTime getAuctionEndTime(int auctionid) {
        String selectQuery = "SELECT ending_time FROM auctions WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement selectStmt = connection.prepareStatement(selectQuery)) {

            // Assign auctionid to question mark (?)
            selectStmt.setInt(1, auctionid);

            try (ResultSet rs = selectStmt.executeQuery()) {
                if (rs.next()) {
                    // Get time as Timestamp from SQL
                    Timestamp dbEndTime = rs.getTimestamp("ending_time");
                    if (dbEndTime != null) {
                        // Convert Timestamp to LocalDateTime and return
                        return dbEndTime.toLocalDateTime();
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error when getting auction end time: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    public Auction findById(int id) {
        String sql = "SELECT a.*, i.name, i.description, i.type, u.username as highest_bidder_username, s.username as seller_username FROM auctions a JOIN items i ON a.item_id = i.id LEFT JOIN users u ON a.highest_bidder_id = u.id JOIN users s ON a.seller_id = s.id WHERE a.id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return mapResultSetToAuctionWithItem(resultSet);
            }

            return null;

        } catch (SQLException e) {
            throw new findingException("Cannot find auction by id");
        }
    }

    public Auction findByIdForUpdate(Connection connection, int id) {
        String sql = "SELECT a.*, i.name, i.description, i.type, u.username as highest_bidder_username, s.username as seller_username FROM auctions a JOIN items i ON a.item_id = i.id LEFT JOIN users u ON a.highest_bidder_id = u.id JOIN users s ON a.seller_id = s.id WHERE a.id = ? FOR UPDATE";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return mapResultSetToAuctionWithItem(resultSet);
            }

            return null;

        } catch (SQLException e) {
            throw new findingException("Cannot find auction by id for update");
        }
    }

    public List<Auction> findAll() {
        String sql = """
            SELECT
                a.*, i.name, i.description, i.type,
                u.username AS highest_bidder_username,
                s.username AS seller_username
            FROM auctions a
            INNER JOIN items i ON a.item_id = i.id
            LEFT JOIN users u ON a.highest_bidder_id = u.id
            INNER JOIN users s ON a.seller_id = s.id
            """;

        List<Auction> auctions = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                auctions.add(mapResultSetToAuctionWithItem(resultSet));
            }

        } catch (SQLException e) {
            throw new findingException("Cannot find all auctions");
        }

        return auctions;
    }

    public List<Auction> findAllBySellerId(int sellerId) {
        String sql = """
            SELECT
                a.*, i.name, i.description, i.type,
                u.username AS highest_bidder_username,
                s.username AS seller_username
            FROM auctions a
            INNER JOIN items i ON a.item_id = i.id
            LEFT JOIN users u ON a.highest_bidder_id = u.id
            INNER JOIN users s ON a.seller_id = s.id
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
            throw new findingException("Cannot find auctions for seller ID: " + sellerId);
        }

        return auctions;
    }

    public List<Auction> findAllOpenAuctions() {
        String sql = """
            SELECT
                a.*, i.name, i.description, i.type,
                u.username AS highest_bidder_username,
                s.username AS seller_username
            FROM auctions a
            INNER JOIN items i ON a.item_id = i.id
            LEFT JOIN users u ON a.highest_bidder_id = u.id
            INNER JOIN users s ON a.seller_id = s.id
            WHERE a.status = 'OPEN' OR a.status = 'RUNNING'
            """;

        List<Auction> auctions = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                auctions.add(mapResultSetToAuctionWithItem(resultSet));
            }

        } catch (SQLException e) {
            throw new findingException("Cannot find open/running auctions");
        }

        return auctions;
    }

    public List<Auction> findActiveAuctionsBySeller(Connection connection, int sellerId) {
        String sql = """
            SELECT
                a.*, i.name, i.description, i.type,
                u.username AS highest_bidder_username,
                s.username AS seller_username
            FROM auctions a
            INNER JOIN items i ON a.item_id = i.id
            LEFT JOIN users u ON a.highest_bidder_id = u.id
            INNER JOIN users s ON a.seller_id = s.id
            WHERE a.seller_id = ? AND (a.status = 'OPEN' OR a.status = 'RUNNING' OR a.status = 'FINISHED')
            """;
        List<Auction> auctions = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, sellerId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    auctions.add(mapResultSetToAuctionWithItem(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new findingException("Cannot find active auctions by seller ID: " + sellerId);
        }
        return auctions;
    }

    public List<Auction> findActiveAuctionsByHighestBidder(Connection connection, int bidderId) {
        String sql = """
            SELECT
                a.*, i.name, i.description, i.type,
                u.username AS highest_bidder_username,
                s.username AS seller_username
            FROM auctions a
            INNER JOIN items i ON a.item_id = i.id
            LEFT JOIN users u ON a.highest_bidder_id = u.id
            INNER JOIN users s ON a.seller_id = s.id
            WHERE a.highest_bidder_id = ? AND (a.status = 'OPEN' OR a.status = 'RUNNING' OR a.status = 'FINISHED')
            """;
        List<Auction> auctions = new ArrayList<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, bidderId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    auctions.add(mapResultSetToAuctionWithItem(resultSet));
                }
            }
        } catch (SQLException e) {
            throw new findingException("Cannot find active auctions by highest bidder ID: " + bidderId);
        }
        return auctions;
    }

    /*
        Normal update.
        Do not use external transaction.
    */
    public void update(Auction auction) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            update(connection, auction);
        } catch (SQLException e) {
            throw new updatingException("Cannot update auction", e);
        }
    }

    /*
        Update using shared connection.
        Used in transaction at Service.
    */
    public void update(Connection connection, Auction auction) throws SQLException {
        // If highest_bidder_id has a value but username is null (old data is missing),
        // lookup username from DB to avoid overwriting existing correct data with NULL.
        if (auction.getHighestBidderId() != null && auction.getHighestBidderUsername() == null) {
            try {
                String lookupSql = "SELECT username FROM users WHERE id = ?";
                try (PreparedStatement ps = connection.prepareStatement(lookupSql)) {
                    ps.setInt(1, auction.getHighestBidderId());
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            auction.setHighestBidderUsername(rs.getString("username"));
                        }
                    }
                }
            } catch (SQLException ignored) {
                // Do not block main update if secondary lookup fails
            }
        }

        String sql = """
                UPDATE auctions
                SET seller_id = ?,
                    status = ?,
                    starting_price = ?,
                    current_price = ?,
                    highest_bidder_id = ?,
                    highest_bidder_username = ?,
                    starting_time = ?,
                    ending_time = ?,
                    path = ?
                WHERE id = ?
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, auction.getSellerId());
            statement.setString(2, auction.getStatus().name());
            statement.setBigDecimal(3, auction.getStartingPrice());
            statement.setBigDecimal(4, auction.getCurrentPrice());

            if (auction.getHighestBidderId() == null) {
                statement.setNull(5, Types.INTEGER);
                statement.setNull(6, Types.VARCHAR);
            } else {
                statement.setInt(5, auction.getHighestBidderId());
                statement.setString(6, auction.getHighestBidderUsername());
            }

            statement.setTimestamp(7, Timestamp.valueOf(auction.getStartTime()));
            statement.setTimestamp(8, Timestamp.valueOf(auction.getEndTime()));
            statement.setString(9, auction.getImagePath());
            statement.setInt(10, auction.getId());

            statement.executeUpdate();

        }
    }

    public void delete(Connection connection, int id) {
        String sql = "DELETE FROM auctions WHERE id = ?";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);
            statement.executeUpdate();

        } catch (SQLException e) {
            throw new deletingException("Cannot delete auction: " + e.getMessage(), e);
        }
    }
    
    // ... other methods ...

    private Auction mapResultSetToAuctionWithItem(ResultSet resultSet) throws SQLException {
        Auction auction = new Auction();

        auction.setId(resultSet.getInt("id"));
        auction.setItemId(resultSet.getInt("item_id"));
        auction.setSellerId(resultSet.getInt("seller_id"));
        auction.setSellerUsername(resultSet.getString("seller_username"));
        auction.setName(resultSet.getString("name"));
        auction.setDescription(resultSet.getString("description"));
        auction.setStartingPrice(resultSet.getBigDecimal("starting_price"));
        auction.setCurrentPrice(resultSet.getBigDecimal("current_price"));

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
        String imagePath = resultSet.getString("path");
        auction.setImagePath(imagePath);
        if (imagePath != null) {
            auction.setImageBase64(imageService.getBase64Image(imagePath));
        }

        return auction;
    }
}
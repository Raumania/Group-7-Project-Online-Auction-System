package auction_system.server.dao;

import auction_system.server.model.AutoBid;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AutoBidDAO {
    private static AutoBidDAO instance;

    private AutoBidDAO() {
    }

    public static AutoBidDAO getInstance() {
        if (instance == null) {
            instance = new AutoBidDAO();
        }
        return instance;
    }

    public void saveOrUpdate(Connection connection, AutoBid autoBid) throws SQLException {
        String sql = """
                INSERT INTO auto_bids (user_id, auction_id, max_bid, bid_increment, is_active, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    max_bid = VALUES(max_bid),
                    bid_increment = VALUES(bid_increment),
                    is_active = VALUES(is_active),
                    created_at = VALUES(created_at)
                """;

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, autoBid.getUserId());
            statement.setInt(2, autoBid.getAuctionId());
            statement.setBigDecimal(3, autoBid.getMaxBid());
            
            if (autoBid.getBidIncrement() == null) {
                statement.setNull(4, Types.DECIMAL);
            } else {
                statement.setBigDecimal(4, autoBid.getBidIncrement());
            }
            
            statement.setBoolean(5, autoBid.isActive());
            statement.setTimestamp(6, Timestamp.valueOf(autoBid.getCreatedAt()));

            statement.executeUpdate();

            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    autoBid.setId(generatedKeys.getInt(1));
                }
            }
        }
    }

    public void deactivate(Connection connection, int id) throws SQLException {
        String sql = "UPDATE auto_bids SET is_active = 0 WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    public void disableAutoBid(Connection connection, int userId, int auctionId) throws SQLException {
        String sql = "UPDATE auto_bids SET is_active = 0 WHERE user_id = ? AND auction_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, auctionId);
            statement.executeUpdate();
        }
    }

    public List<AutoBid> findActiveByAuctionId(Connection connection, int auctionId) throws SQLException {
        String sql = "SELECT * FROM auto_bids WHERE auction_id = ? AND is_active = 1";
        List<AutoBid> results = new ArrayList<>();
        
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, auctionId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    results.add(mapResultSetToAutoBid(rs));
                }
            }
        }
        return results;
    }

    public AutoBid findByUserAndAuction(Connection connection, int userId, int auctionId) throws SQLException {
        String sql = "SELECT * FROM auto_bids WHERE user_id = ? AND auction_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, userId);
            statement.setInt(2, auctionId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToAutoBid(rs);
                }
            }
        }
        return null;
    }

    private AutoBid mapResultSetToAutoBid(ResultSet rs) throws SQLException {
        AutoBid autoBid = new AutoBid();
        autoBid.setId(rs.getInt("id"));
        autoBid.setUserId(rs.getInt("user_id"));
        autoBid.setAuctionId(rs.getInt("auction_id"));
        autoBid.setMaxBid(rs.getBigDecimal("max_bid"));
        autoBid.setBidIncrement(rs.getBigDecimal("bid_increment"));
        autoBid.setActive(rs.getBoolean("is_active"));
        
        Timestamp createdAtTimestamp = rs.getTimestamp("created_at");
        if (createdAtTimestamp != null) {
            autoBid.setCreatedAt(createdAtTimestamp.toLocalDateTime());
        }
        
        return autoBid;
    }

    public List<AutoBid> findAll() {
        String sql = "SELECT * FROM auto_bids";
        List<AutoBid> results = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                results.add(mapResultSetToAutoBid(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Cannot find all auto bids", e);
        }
        return results;
    }
}


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
                    is_active = VALUES(is_active)
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

            // Lấy ID của record vừa INSERT hoặc UPDATE
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int generatedId = generatedKeys.getInt(1);
                    if (generatedId > 0) {
                        // INSERT mới: dùng generated key
                        autoBid.setId(generatedId);
                    } else {
                        // ON DUPLICATE KEY UPDATE: query lại để lấy ID thực
                        fetchAndSetId(connection, autoBid);
                    }
                } else {
                    fetchAndSetId(connection, autoBid);
                }
            }
        }
    }

    // Helper: query id theo (user_id, auction_id) khi generated key không khả dụng
    private void fetchAndSetId(Connection connection, AutoBid autoBid) throws SQLException {
        String query = "SELECT id FROM auto_bids WHERE user_id = ? AND auction_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, autoBid.getUserId());
            ps.setInt(2, autoBid.getAuctionId());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    autoBid.setId(rs.getInt("id"));
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

    // Trả về AutoBid bất kể active hay inactive (dùng cho admin/audit)
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

    // Chỉ trả về AutoBid đang active (dùng cho getAutoBidConfig)
    public AutoBid findActiveByUserAndAuction(Connection connection, int userId, int auctionId) throws SQLException {
        String sql = "SELECT * FROM auto_bids WHERE user_id = ? AND auction_id = ? AND is_active = 1";
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


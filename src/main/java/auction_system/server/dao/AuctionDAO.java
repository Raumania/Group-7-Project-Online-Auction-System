package auction_system.server.dao;

import auction_system.common.dto.AuctionDTO;
import auction_system.common.enums.AuctionStatus;
import auction_system.common.enums.ItemType;
import auction_system.server.model.Auction;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AuctionDAO {

    public int save(Auction auction) {
        String sql = """
                INSERT INTO auctions
                (seller_id, starting_price, current_price, highest_bidder_id, status, starting_time, ending_time)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            // ĐÃ SỬA LẠI THỨ TỰ CHO KHỚP VỚI CÂU LỆNH SQL
            statement.setInt(1, auction.getSellerId());
            statement.setDouble(2, auction.getStartingPrice());
            statement.setDouble(3, auction.getCurrentPrice());

            // Xử lý null cho highest_bidder_id (tránh lỗi nếu chưa có ai bid)
            if (auction.getHighestBidderId() == null) {
                statement.setNull(4, Types.INTEGER);
            } else {
                statement.setInt(4, auction.getHighestBidderId());
            }

            statement.setString(5, auction.getStatus().name());
            statement.setTimestamp(6, Timestamp.valueOf(auction.getStartTime()));
            statement.setTimestamp(7, Timestamp.valueOf(auction.getEndTime()));

            // Thực thi lệnh INSERT
            statement.executeUpdate();

            // LẤY ID VỪA ĐƯỢỢC TẠO RA
            try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int generatedId = generatedKeys.getInt(1);
                    auction.setId(generatedId); // Set ngược lại vào object nếu cần
                    return generatedId;         // Trả về ID để dùng tạo Item
                } else {
                    throw new SQLException("Creating auction failed, no ID obtained.");
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Cannot save auction", e);
        }
    }

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
    public List<Auction>getallopenAuction(){
        String sql="SELECT * FROM auctions where status = ? or where status= ? ";
        List<Auction> auctions=new ArrayList<>();
        try(Connection connection=DatabaseConnection.getConnection();
            PreparedStatement statement=connection.prepareStatement(sql)){
            statement.setString(1,AuctionStatus.OPEN.name());
            statement.setString(2,AuctionStatus.RUNNING.name());
            ResultSet resultSet=statement.executeQuery();
            while (resultSet.next()){
                auctions.add(mapResultSetToAuction(resultSet));
            }
        }
        catch (SQLException e){
            throw new RuntimeException("Cannot find all open auctions",e);

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
        // SQL JOIN 2 bảng auctions (a) và items (i) dựa trên id trùng nhau
        String sql = "SELECT a.id, a.seller_id, i.name, i.description, i.type, a.status, " +
                "a.starting_price, a.current_price, a.highest_bidder_id, a.starting_time, a.ending_time " +
                "FROM auctions a " +
                "INNER JOIN items i ON a.id = i.id " +
                "WHERE a.seller_id = ?";

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
    public void update(Auction auction) {
        String sql = """
                UPDATE auctions
                SET seller_id = ?, status = ?,
                    starting_price = ?, current_price = ?, highest_bidder_id = ?,
                    starting_time = ?, ending_time = ?
                WHERE id = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

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

    public void delete(int id) {
        String sql = "DELETE FROM auctions WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, id);
            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Cannot delete auction", e);
        }
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

        java.sql.Timestamp startTimestamp = resultSet.getTimestamp("starting_time");
        if (startTimestamp != null) {
            auction.setStartTime(startTimestamp.toLocalDateTime());
        }

        java.sql.Timestamp endTimestamp = resultSet.getTimestamp("ending_time");
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

        java.sql.Timestamp startTimestamp = resultSet.getTimestamp("starting_time");
        if (startTimestamp != null) {
            auction.setStartTime(startTimestamp.toLocalDateTime());
        }

        java.sql.Timestamp endTimestamp = resultSet.getTimestamp("ending_time");
        if (endTimestamp != null) {
            auction.setEndTime(endTimestamp.toLocalDateTime());
        }

        return auction;
    }
}
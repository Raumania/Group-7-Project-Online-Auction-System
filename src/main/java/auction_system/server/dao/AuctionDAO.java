package auction_system.server.dao;

import auction_system.server.model.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

public class AuctionDAO {

    private ItemDAO itemDAO;
    private UserDAO userDAO;

    public AuctionDAO() {
        this.itemDAO = new ItemDAO();
        this.userDAO = new UserDAO();
    }

    /*
        Lưu auction mới vào bảng auctions.
    */
    public void save(Auction auction) {
        // CẬP NHẬT: Thêm starting_price vào câu lệnh INSERT
        String sql = "INSERT INTO auctions(id, item_id, seller_id, starting_price, current_price, highest_bidder_id, status) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            if (auction.getSeller() == null) {
                throw new RuntimeException("Seller cannot be null");
            }

            if (!auction.getSeller().hasRole(UserRole.SELLER)) {
                throw new RuntimeException("Seller must have SELLER role");
            }

            if (auction.getHighestBidder() != null &&
                    !auction.getHighestBidder().hasRole(UserRole.BIDDER)) {
                throw new RuntimeException("Highest bidder must have BIDDER role");
            }

            statement.setString(1, auction.getId());
            statement.setInt(2, Integer.parseInt(auction.getItem().getId()));
            statement.setInt(3, Integer.parseInt(auction.getSeller().getId()));

            // CẬP NHẬT: Truyền giá trị starting_price vào vị trí số 4
            statement.setDouble(4, auction.getStartingPrice());

            // Đẩy các tham số còn lại lùi xuống 1 vị trí
            statement.setDouble(5, auction.getCurrentPrice());

            if (auction.getHighestBidder() == null) {
                statement.setNull(6, Types.INTEGER);
            } else {
                statement.setInt(6, Integer.parseInt(auction.getHighestBidder().getId()));
            }

            statement.setString(7, auction.getStatus().name());

            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Cannot save auction", e);
        }
    }

    public Auction findById(String id) {
        String sql = "SELECT * FROM auctions WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, id);

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return mapResultSetToAuction(resultSet);
            }

            return null;

        } catch (SQLException e) {
            throw new RuntimeException("Cannot find auction by id", e);
        }
    }

    public List<Auction> findAll() {
        String sql = "SELECT * FROM auctions";

        List<Auction> auctions = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                Auction auction = mapResultSetToAuction(resultSet);
                auctions.add(auction);
            }

            return auctions;

        } catch (SQLException e) {
            throw new RuntimeException("Cannot find all auctions", e);
        }
    }

    public List<Auction> findOpenAuctions() {
        String sql = "SELECT * FROM auctions WHERE status = ? OR status = ?";

        List<Auction> auctions = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, AuctionStatus.OPEN.name());
            statement.setString(2, AuctionStatus.RUNNING.name());

            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                Auction auction = mapResultSetToAuction(resultSet);
                auctions.add(auction);
            }

            return auctions;

        } catch (SQLException e) {
            throw new RuntimeException("Cannot find open auctions", e);
        }
    }

    public boolean update(Auction auction) {
        // Cập nhật trạng thái không cần update lại starting_price vì đây thường là thông tin cố định từ đầu
        String sql = "UPDATE auctions SET current_price = ?, highest_bidder_id = ?, status = ? WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            if (auction.getHighestBidder() != null &&
                    !auction.getHighestBidder().hasRole(UserRole.BIDDER)) {
                throw new RuntimeException("Highest bidder must have BIDDER role");
            }

            statement.setDouble(1, auction.getCurrentPrice());

            if (auction.getHighestBidder() == null) {
                statement.setNull(2, Types.INTEGER);
            } else {
                statement.setInt(2, Integer.parseInt(auction.getHighestBidder().getId()));
            }

            statement.setString(3, auction.getStatus().name());
            statement.setString(4, auction.getId());

            int affectedRows = statement.executeUpdate();

            return affectedRows > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Cannot update auction", e);
        }
    }

    public boolean deleteById(String id) {
        String sql = "DELETE FROM auctions WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, id);

            int affectedRows = statement.executeUpdate();

            return affectedRows > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Cannot delete auction", e);
        }
    }

    private Auction mapResultSetToAuction(ResultSet resultSet) throws SQLException {
        String id = resultSet.getString("id");
        String itemId = String.valueOf(resultSet.getInt("item_id"));
        String sellerId = String.valueOf(resultSet.getInt("seller_id"));

        // CẬP NHẬT: Lấy thêm giá trị starting_price
        double startingPrice = resultSet.getDouble("starting_price");
        double currentPrice = resultSet.getDouble("current_price");

        String highestBidderId = null;
        int highestBidderInt = resultSet.getInt("highest_bidder_id");

        if (!resultSet.wasNull()) {
            highestBidderId = String.valueOf(highestBidderInt);
        }

        String statusText = resultSet.getString("status");

        Item item = itemDAO.findById(itemId);
        if (item == null) {
            throw new RuntimeException("Item not found for auction");
        }

        User seller = userDAO.findById(sellerId);
        if (seller == null) {
            throw new RuntimeException("Seller not found for auction");
        }

        if (!seller.hasRole(UserRole.SELLER)) {
            throw new RuntimeException("Seller user does not have SELLER role");
        }

        Auction auction = new Auction(item, seller);
        auction.setId(id);

        // CẬP NHẬT: Set lại startingPrice cho đối tượng auction
        auction.setStartingPrice(startingPrice);
        auction.setCurrentPrice(currentPrice);
        auction.setStatus(AuctionStatus.valueOf(statusText));

        if (highestBidderId != null) {
            User bidder = userDAO.findById(highestBidderId);

            if (bidder == null) {
                throw new RuntimeException("Highest bidder not found for auction");
            }

            if (!bidder.hasRole(UserRole.BIDDER)) {
                throw new RuntimeException("Highest bidder user does not have BIDDER role");
            }

            auction.setHighestBidder(bidder);
        }

        return auction;
    }
}
package auction_system.server.dao;

import auction_system.server.model.BidTransaction;
import auction_system.server.model.User;
import auction_system.server.model.UserRole;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BidTransactionDAO {

    private UserDAO userDAO;

    public BidTransactionDAO() {
        this.userDAO = new UserDAO();
    }

    /* Lưu một bid transaction vào database.

        Lưu ý mới:
        - bidder không còn là object Bidder nữa
        - bidder là User có role BIDDER
        - bidder_id trong database là INT vì users.id là INT AUTO_INCREMENT
        - auction_id bây giờ là INT
    */
    public void save(int auctionId, BidTransaction transaction) {
        String sql = "INSERT INTO bid_transactions(id, auction_id, bidder_id, amount, timestamp) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            if (transaction.getBidder() == null) {
                throw new RuntimeException("Bidder cannot be null");
            }

            if (!transaction.getBidder().hasRole(UserRole.BIDDER)) {
                throw new RuntimeException("Bidder must have BIDDER role");
            }

            statement.setString(1, transaction.getId());

            /*
                CẬP NHẬT: auction_id là INT trong database
            */
            statement.setInt(2, auctionId);

            /*
                bidder_id là INT trong database.
                Entity.id trong Java vẫn là String nên parse sang int.
            */
            statement.setInt(3, Integer.parseInt(transaction.getBidder().getId()));

            statement.setDouble(4, transaction.getAmount());
            statement.setLong(5, transaction.getTimestamp());

            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Cannot save bid transaction", e);
        }
    }

    /*
        Lấy lịch sử bid của một auction.
    */
    public List<BidTransaction> findByAuctionId(int auctionId) {
        String sql = "SELECT * FROM bid_transactions WHERE auction_id = ? ORDER BY timestamp ASC";

        List<BidTransaction> transactions = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            // CẬP NHẬT: setInt thay vì setString
            statement.setInt(1, auctionId);

            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                BidTransaction transaction = mapResultSetToBidTransaction(resultSet);
                transactions.add(transaction);
            }

            return transactions;

        } catch (SQLException e) {
            throw new RuntimeException("Cannot find bid history", e);
        }
    }

    /*
        Lấy bid mới nhất của auction.
    */
    public BidTransaction findLatestByAuctionId(int auctionId) {
        String sql = "SELECT * FROM bid_transactions WHERE auction_id = ? ORDER BY timestamp DESC LIMIT 1";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            // CẬP NHẬT: setInt thay vì setString
            statement.setInt(1, auctionId);

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return mapResultSetToBidTransaction(resultSet);
            }

            return null;

        } catch (SQLException e) {
            throw new RuntimeException("Cannot find latest bid", e);
        }
    }

    /*
        Đếm tổng số bid của một auction.
    */
    public int countByAuctionId(int auctionId) {
        String sql = "SELECT COUNT(*) FROM bid_transactions WHERE auction_id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            // CẬP NHẬT: setInt thay vì setString
            statement.setInt(1, auctionId);

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getInt(1);
            }

            return 0;

        } catch (SQLException e) {
            throw new RuntimeException("Cannot count bids", e);
        }
    }

    /*
        Chuyển một dòng trong bảng bid_transactions thành object BidTransaction.
    */
    private BidTransaction mapResultSetToBidTransaction(ResultSet resultSet) throws SQLException {
        String id = resultSet.getString("id");

        /*
            bidder_id trong database là INT.
            UserDAO.findById đang nhận String,
            nên convert int -> String.
        */
        String bidderId = String.valueOf(resultSet.getInt("bidder_id"));

        double amount = resultSet.getDouble("amount");
        long timestamp = resultSet.getLong("timestamp");

        /*
            Trước đây:
            - lấy User
            - kiểm tra instanceof Bidder
            - ép kiểu Bidder

            Bây giờ:
            - lấy User
            - kiểm tra user có role BIDDER không
            - không ép kiểu nữa
        */
        User bidder = userDAO.findById(bidderId);

        if (bidder == null) {
            throw new RuntimeException("Bidder not found");
        }

        if (!bidder.hasRole(UserRole.BIDDER)) {
            throw new RuntimeException("User does not have BIDDER role");
        }

        BidTransaction transaction = new BidTransaction(bidder, amount);

        /*
            Constructor tạo id và timestamp mới,
            nên khi đọc từ database phải set lại giá trị cũ.
        */
        transaction.setId(id);
        transaction.setTimestamp(timestamp);

        return transaction;
    }
}
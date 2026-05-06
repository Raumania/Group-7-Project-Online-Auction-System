package auction_system.server.dao;

import auction_system.server.model.BidTransaction;
import auction_system.server.model.Bidder;
import auction_system.server.model.User;

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

    /*
        Lưu một bid transaction vào database.
    */
    public void save(String auctionId, BidTransaction transaction) {
        String sql = "INSERT INTO bid_transactions(id, auction_id, bidder_id, amount, timestamp) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, transaction.getId());
            statement.setString(2, auctionId);
            statement.setString(3, transaction.getBidder().getId());
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
    public List<BidTransaction> findByAuctionId(String auctionId) {
        String sql = "SELECT * FROM bid_transactions WHERE auction_id = ? ORDER BY timestamp ASC";

        List<BidTransaction> transactions = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, auctionId);

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
    public BidTransaction findLatestByAuctionId(String auctionId) {
        String sql = "SELECT * FROM bid_transactions WHERE auction_id = ? ORDER BY timestamp DESC LIMIT 1";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, auctionId);

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
    public int countByAuctionId(String auctionId) {
        String sql = "SELECT COUNT(*) FROM bid_transactions WHERE auction_id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, auctionId);

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
        String bidderId = resultSet.getString("bidder_id");
        double amount = resultSet.getDouble("amount");
        long timestamp = resultSet.getLong("timestamp");

        User user = userDAO.findById(bidderId);

        if (!(user instanceof Bidder)) {
            throw new RuntimeException("Bidder not found");
        }

        Bidder bidder = (Bidder) user;

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
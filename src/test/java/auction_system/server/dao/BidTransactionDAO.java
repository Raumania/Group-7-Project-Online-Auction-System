package auction_system.server.dao;

import auction_system.common.enums.UserRole;
import auction_system.server.model.BidTransaction;
import auction_system.server.model.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BidTransactionDAO {

    private UserDAO userDAO;

    private static BidTransactionDAO instance;

    private BidTransactionDAO() {
        UserDAO.getInstance();
    }

    public static BidTransactionDAO getInstance() {
        if (instance == null) {
            instance = new BidTransactionDAO();
        }
        return instance;
    }

    /*
        Lưu một bid transaction vào database.

        Lưu ý:
        - id của bid_transactions là INT AUTO_INCREMENT
        - không insert id từ Java nữa
        - sau khi insert xong, lấy id tự tăng từ database set lại vào object
        - auction_id là INT
        - bidder_id là INT
        - bidtime là DATETIME
    */
    public void save(int auctionId, BidTransaction transaction) {
        String sql = "INSERT INTO bid_transactions(auction_id, bidder_id, amount, biddingtime) " +
                "VALUES (?, ?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            if (transaction.getBidder() == null) {
                throw new RuntimeException("Bidder cannot be null");
            }

            if (!transaction.getBidder().hasRole(UserRole.BIDDER)) {
                throw new RuntimeException("Bidder must have BIDDER role");
            }

            /*
                auction_id là INT trong database
            */
            statement.setInt(1, auctionId);

            /*
                bidder_id là INT trong database.
            */
            statement.setInt(2, transaction.getBidder().getId());

            statement.setDouble(3, transaction.getAmount());

            /*
                LocalDateTime trong Java -> DATETIME trong MySQL
            */
            statement.setObject(4, transaction.getBidTime());

            statement.executeUpdate();

            /*
                Lấy id tự tăng do MySQL sinh ra.
            */
            ResultSet generatedKeys = statement.getGeneratedKeys();

            if (generatedKeys.next()) {
                int generatedId = generatedKeys.getInt(1);
                transaction.setId(generatedId);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Cannot save bid transaction", e);
        }
    }

    /*
        Lấy lịch sử bid của một auction.
    */
    public List<BidTransaction> findByAuctionId(int auctionId) {
        String sql = "SELECT * FROM bid_transactions WHERE auction_id = ? ORDER BY bidtime ASC";

        List<BidTransaction> transactions = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

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
        String sql = "SELECT * FROM bid_transactions WHERE auction_id = ? ORDER BY bidtime DESC LIMIT 1";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

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
        int id = resultSet.getInt("id");

        int bidderId = resultSet.getInt("bidder_id");

        double amount = resultSet.getDouble("amount");

        /*
            bidtime trong database là DATETIME.
            Trong Java lấy ra thành LocalDateTime.
        */
        LocalDateTime bidTime = resultSet.getObject("bidtime", LocalDateTime.class);

        User bidder = userDAO.findById(bidderId);

        if (bidder == null) {
            throw new RuntimeException("Bidder not found");
        }

        if (!bidder.hasRole(UserRole.BIDDER)) {
            throw new RuntimeException("User does not have BIDDER role");
        }

        BidTransaction transaction = new BidTransaction(bidder, amount);

        /*
            Constructor tạo id và bidTime mới.
            Khi đọc từ database ra phải set lại id và bidTime cũ.
        */
        transaction.setId(id);
        transaction.setBidTime(bidTime);

        return transaction;
    }
}

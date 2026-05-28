package auction_system.server.dao;

import auction_system.common.enums.UserRole;
import auction_system.common.enums.UserStatus;
import auction_system.server.model.BidTransaction;
import auction_system.server.model.User;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.List;

public class BidTransactionDAO {

    private final UserDAO userDAO;

    private static BidTransactionDAO instance;

    private BidTransactionDAO() {
        userDAO = UserDAO.getInstance();
    }

    public static BidTransactionDAO getInstance() {
        if (instance == null) {
            instance = new BidTransactionDAO();
        }
        return instance;
    }

    /*
        Save a bid transaction into the database.

        Note:
        - id of bid_transactions is INT AUTO_INCREMENT
        - do not insert id from Java anymore
        - after inserting, retrieve the auto-generated id from database and set it to the object
        - auction_id is INT
        - bidder_id is INT
        - bidtime is DATETIME
    */
    public void save(Connection connection, int auctionId, BidTransaction transaction) {
        String sql = "INSERT INTO bid_transactions(auction_id, bidder_id, amount, biddingtime) " +
                "VALUES (?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            if (transaction.getBidder() == null) {
                throw new RuntimeException("Bidder cannot be null");
            }

            if (!transaction.getBidder().hasRole(UserRole.BIDDER)) {
                throw new RuntimeException("Bidder must have BIDDER role");
            }

            /*
                auction_id is INT in the database
            */
            statement.setInt(1, auctionId);

            /*
                bidder_id is INT in the database.
            */
            statement.setInt(2, transaction.getBidder().getId());

            statement.setBigDecimal(3, transaction.getAmount());

            /*
                LocalDateTime in Java -> DATETIME in MySQL
            */
            statement.setObject(4, transaction.getBidTime());

            statement.executeUpdate();

            /*
                Retrieve the auto-generated id created by MySQL.
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
        Retrieve the bid history of an auction.
    */
    public List<BidTransaction> findByAuctionId(int auctionId) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return findByAuctionId(connection, auctionId);
        } catch (SQLException e) {
            throw new RuntimeException("Cannot find bid history", e);
        }
    }

    public List<BidTransaction> findByAuctionId(Connection connection, int auctionId) {
        String sql = "SELECT bt.*, u.fullname, u.username, u.password, u.roles, u.available_balance, u.frozen_balance, u.status " +
                     "FROM bid_transactions bt " +
                     "INNER JOIN users u ON bt.bidder_id = u.id " +
                     "WHERE bt.auction_id = ? " +
                     "ORDER BY bt.biddingtime ASC";

        List<BidTransaction> transactions = new ArrayList<>();

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, auctionId);

            ResultSet resultSet = statement.executeQuery();

            while (resultSet.next()) {
                BidTransaction transaction = mapResultSetToBidTransactionWithUser(resultSet);
                transactions.add(transaction);
            }

            return transactions;

        } catch (SQLException e) {
            throw new RuntimeException("Cannot find bid history in transaction", e);
        }
     }

    /*
        Retrieve the latest bid of the auction.
    */
    public BidTransaction findLatestByAuctionId(int auctionId) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            return findLatestByAuctionId(connection, auctionId);
        } catch (SQLException e) {
            throw new RuntimeException("Cannot find latest bid", e);
        }
    }

    public BidTransaction findLatestByAuctionId(Connection connection, int auctionId) {
        String sql = "SELECT bt.*, u.fullname, u.username, u.password, u.roles, u.available_balance, u.frozen_balance, u.status " +
                     "FROM bid_transactions bt " +
                     "INNER JOIN users u ON bt.bidder_id = u.id " +
                     "WHERE bt.auction_id = ? " +
                     "ORDER BY bt.biddingtime DESC LIMIT 1";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, auctionId);

            ResultSet resultSet = statement.executeQuery();

            if (resultSet.next()) {
                return mapResultSetToBidTransactionWithUser(resultSet);
            }

            return null;

        } catch (SQLException e) {
            throw new RuntimeException("Cannot find latest bid in transaction", e);
        }
    }

    /*
        Count total bids of an auction.
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
        Map a row in the bid_transactions table (with joined user columns) to a BidTransaction object.
    */
    private BidTransaction mapResultSetToBidTransactionWithUser(ResultSet resultSet) throws SQLException {
        int id = resultSet.getInt("id");
        int bidderId = resultSet.getInt("bidder_id");
        BigDecimal amount = resultSet.getBigDecimal("amount");
        LocalDateTime bidTime = resultSet.getObject("biddingtime", LocalDateTime.class);

        // Fetch User fields directly from JOINed columns to prevent N+1 query problem!
        String fullname = resultSet.getString("fullname");
        String username = resultSet.getString("username");
        String password = resultSet.getString("password");
        String rolesText = resultSet.getString("roles");
        BigDecimal availableBalance = resultSet.getBigDecimal("available_balance");
        BigDecimal frozenBalance = resultSet.getBigDecimal("frozen_balance");
        String statusText = resultSet.getString("status");

        java.util.Set<UserRole> roles = new java.util.HashSet<>();
        if (rolesText != null && !rolesText.trim().isEmpty()) {
            for (String part : rolesText.split(",")) {
                roles.add(UserRole.valueOf(part.trim()));
            }
        }

        if (roles.isEmpty()) {
            throw new RuntimeException("User has no role");
        }

        User bidder = new User(fullname, username, password, roles);
        bidder.setId(bidderId);
        bidder.setAvailableBalance(availableBalance);
        if (frozenBalance != null) {
            bidder.setFrozenBalance(frozenBalance);
        }
        
        UserStatus status = UserStatus.ACTIVE;
        if (statusText != null) {
            try {
                status = UserStatus.valueOf(statusText);
            } catch (IllegalArgumentException e) {
                status = UserStatus.ACTIVE;
            }
        }
        bidder.setStatus(status);

        BidTransaction transaction = new BidTransaction(bidder, amount);
        transaction.setId(id);
        transaction.setBidTime(bidTime);

        return transaction;
    }

    /*
        Map a raw row in the bid_transactions table to a BidTransaction object.
        Forces sequential connection through UserDAO if used without JOIN.
    */
    private BidTransaction mapResultSetToBidTransaction(ResultSet resultSet) throws SQLException {
        int id = resultSet.getInt("id");

        int bidderId = resultSet.getInt("bidder_id");

        BigDecimal amount = resultSet.getBigDecimal("amount");

        /*
            bidtime in the database is DATETIME.
            In Java, retrieved as LocalDateTime.
        */
        LocalDateTime bidTime = resultSet.getObject("biddingtime", LocalDateTime.class);

        User bidder = userDAO.findById(bidderId);

        if (bidder == null) {
            throw new RuntimeException("Bidder not found");
        }

        if (!bidder.hasRole(UserRole.BIDDER)) {
            throw new RuntimeException("User does not have BIDDER role");
        }

        BidTransaction transaction = new BidTransaction(bidder, amount);

        /*
            Constructor creates a new id and bidTime.
            When reading from database, must set back the old id and bidTime.
        */
        transaction.setId(id);
        transaction.setBidTime(bidTime);

        return transaction;
    }
}
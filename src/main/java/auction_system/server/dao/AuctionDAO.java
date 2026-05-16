package auction_system.server.dao;

import auction_system.server.model.*;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuctionDAO {

    private final ItemDAO itemDAO;
    private final UserDAO userDAO;

    public AuctionDAO() {
        this.itemDAO = new ItemDAO();
        this.userDAO = new UserDAO();
    }

    /*
        Lưu auction mới vào bảng auctions trước.
        Sau đó lấy auctionId vừa sinh ra để lưu item.
        Theo DB mới:
            items.id = auctions.id
    */
    public void save(Auction auction, LocalDateTime startTime, LocalDateTime endTime) {
        String sql = """
                INSERT INTO auctions
                (seller_id, starting_price, current_price, highest_bidder_id, status, starting_time, ending_time)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);

            try (PreparedStatement statement =
                         connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

                validateAuctionBeforeSave(auction);

                statement.setInt(1, Integer.parseInt(auction.getSeller().getId()));
                statement.setDouble(2, auction.getStartingPrice());

                if (auction.getCurrentPrice() == null) {
                    statement.setNull(3, Types.DOUBLE);
                } else {
                    statement.setDouble(3, auction.getCurrentPrice());
                }

                if (auction.getHighestBidder() == null) {
                    statement.setNull(4, Types.INTEGER);
                } else {
                    statement.setInt(4, Integer.parseInt(auction.getHighestBidder().getId()));
                }

                statement.setString(5, auction.getStatus().name());
                statement.setTimestamp(6, Timestamp.valueOf(startTime));
                statement.setTimestamp(7, Timestamp.valueOf(endTime));

                statement.executeUpdate();

                ResultSet generatedKeys = statement.getGeneratedKeys();

                if (!generatedKeys.next()) {
                    throw new RuntimeException("Cannot get generated auction id");
                }

                String auctionId = String.valueOf(generatedKeys.getInt(1));
                auction.setId(auctionId);

                /*
                    Lưu item sau khi đã có auctionId.
                */
                itemDAO.saveForAuction(connection, auctionId, auction.getItem());

                connection.commit();

            } catch (Exception e) {
                connection.rollback();
                throw e;
            }

        } catch (Exception e) {
            throw new RuntimeException("Cannot save auction", e);
        }
    }

    public Auction findById(String id) {
        String sql = "SELECT * FROM auctions WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setInt(1, Integer.parseInt(id));

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
        String sql = "SELECT * FROM auctions WHERE status = ?";

        List<Auction> auctions = new ArrayList<>();

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, AuctionStatus.RUNNING.name());

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

    /*
        Dùng khi có người bid hoặc khi đổi trạng thái auction.
        Cập nhật:
            current_price
            highest_bidder_id
            status
    */
    public boolean update(Auction auction) {
        String sql = """
                UPDATE auctions
                SET current_price = ?, highest_bidder_id = ?, status = ?
                WHERE id = ?
                """;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            if (auction.getHighestBidder() != null &&
                    !auction.getHighestBidder().hasRole(UserRole.BIDDER)) {
                throw new RuntimeException("Highest bidder must have BIDDER role");
            }

            if (auction.getCurrentPrice() == null) {
                statement.setNull(1, Types.DOUBLE);
            } else {
                statement.setDouble(1, auction.getCurrentPrice());
            }

            if (auction.getHighestBidder() == null) {
                statement.setNull(2, Types.INTEGER);
            } else {
                statement.setInt(2, Integer.parseInt(auction.getHighestBidder().getId()));
            }

            statement.setString(3, auction.getStatus().name());
            statement.setInt(4, Integer.parseInt(auction.getId()));

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

            statement.setInt(1, Integer.parseInt(id));

            int affectedRows = statement.executeUpdate();

            return affectedRows > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Cannot delete auction", e);
        }
    }

    private Auction mapResultSetToAuction(ResultSet resultSet) throws SQLException {
        String id = String.valueOf(resultSet.getInt("id"));
        String sellerId = String.valueOf(resultSet.getInt("seller_id"));

        /*
            Vì items.id = auctions.id,
            nên muốn lấy item thì dùng chính auction id.
        */
        Item item = itemDAO.findById(id);

        if (item == null) {
            throw new RuntimeException("Item not found for auction id = " + id);
        }

        User seller = userDAO.findById(sellerId);

        if (seller == null) {
            throw new RuntimeException("Seller not found for auction id = " + id);
        }

        if (!seller.hasRole(UserRole.SELLER)) {
            throw new RuntimeException("Seller user does not have SELLER role");
        }

        double startingPrice = resultSet.getDouble("starting_price");

        /*
            current_price trong DB có thể NULL,
            nên phải đọc bằng getObject(..., Double.class).
        */
        Double currentPrice = resultSet.getObject("current_price", Double.class);

        String highestBidderId = null;
        int highestBidderInt = resultSet.getInt("highest_bidder_id");

        if (!resultSet.wasNull()) {
            highestBidderId = String.valueOf(highestBidderInt);
        }

        String statusText = resultSet.getString("status");

        Auction auction = new Auction(item, seller);
        auction.setId(id);
        auction.setStartingPrice(startingPrice);
        auction.setCurrentPrice(currentPrice);
        auction.setStatus(AuctionStatus.valueOf(statusText));

        if (highestBidderId != null) {
            User highestBidder = userDAO.findById(highestBidderId);

            if (highestBidder == null) {
                throw new RuntimeException("Highest bidder not found for auction id = " + id);
            }

            if (!highestBidder.hasRole(UserRole.BIDDER)) {
                throw new RuntimeException("Highest bidder user does not have BIDDER role");
            }

            auction.setHighestBidder(highestBidder);
        }

        return auction;
    }

    private void validateAuctionBeforeSave(Auction auction) {
        if (auction == null) {
            throw new RuntimeException("Auction cannot be null");
        }

        if (auction.getSeller() == null) {
            throw new RuntimeException("Seller cannot be null");
        }

        if (!auction.getSeller().hasRole(UserRole.SELLER)) {
            throw new RuntimeException("Seller must have SELLER role");
        }

        if (auction.getItem() == null) {
            throw new RuntimeException("Item cannot be null");
        }

        if (auction.getHighestBidder() != null &&
                !auction.getHighestBidder().hasRole(UserRole.BIDDER)) {
            throw new RuntimeException("Highest bidder must have BIDDER role");
        }
    }
}
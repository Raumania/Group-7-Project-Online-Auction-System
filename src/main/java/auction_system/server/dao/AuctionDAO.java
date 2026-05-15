package auction_system.server.dao;

import auction_system.server.model.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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

        Auction mới có:
        - item_id
        - seller_id
        - current_price = item.getStartingPrice()
        - highest_bidder_id = null
        - status = OPEN

        Lưu ý mới:
        - auction.id là INT AUTO_INCREMENT trong database
        - item_id là INT vì items.id là INT AUTO_INCREMENT
        - seller_id là INT vì users.id là INT AUTO_INCREMENT
        - highest_bidder_id là INT vì users.id là INT AUTO_INCREMENT
        - trong Java Entity.id vẫn là String nên khi lấy id từ DB thì convert int -> String
    */
    public void save(Auction auction) {
        String sql = "INSERT INTO auctions(item_id, seller_id, current_price, highest_bidder_id, status) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

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

            /*
                item_id là INT trong database.
            */
            statement.setInt(1, Integer.parseInt(auction.getItem().getId()));

            /*
                seller_id là INT trong database.
            */
            statement.setInt(2, Integer.parseInt(auction.getSeller().getId()));

            statement.setDouble(3, auction.getCurrentPrice());

            /*
                highest_bidder_id có thể null.
                Nếu null thì dùng setNull với Types.INTEGER.
            */
            if (auction.getHighestBidder() == null) {
                statement.setNull(4, Types.INTEGER);
            } else {
                statement.setInt(4, Integer.parseInt(auction.getHighestBidder().getId()));
            }

            statement.setString(5, auction.getStatus().name());

            statement.executeUpdate();

            /*
                Vì auction.id là AUTO_INCREMENT,
                nên sau khi insert xong phải lấy id mà database vừa tự tạo.
            */
            ResultSet generatedKeys = statement.getGeneratedKeys();

            if (generatedKeys.next()) {
                int newId = generatedKeys.getInt(1);
                auction.setId(String.valueOf(newId));
            } else {
                throw new RuntimeException("Cannot get generated auction id");
            }

        } catch (SQLException e) {
            throw new RuntimeException("Cannot save auction", e);
        }
    }

    /*
        Tìm auction theo id.

        auction.id trong database hiện là INT AUTO_INCREMENT.
        Nhưng trong Java Entity.id vẫn là String,
        nên khi query phải parse String -> int.
    */
    public Auction findById(String id) {
        String sql = "SELECT * FROM auctions WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             //object chuyển lệnh từ java sang MySQL
             PreparedStatement statement = connection.prepareStatement(sql)) {

            //truyền giá trị vào dấu ? đầu tiên
            statement.setInt(1, Integer.parseInt(id));

            // sau khi gửi lệnh đến MySQL thì sẽ đc trả dữ liệu về 1 cái bảng theo kiểu 1 dòng
            ResultSet resultSet = statement.executeQuery();

            //khi goi resultSet.next() thì ns sẽ tự động di chuyển xuống dòng đầu tiên và bắt đầu tạo luôn

            if (resultSet.next()) {
                return mapResultSetToAuction(resultSet);
            }

            return null;

        } catch (SQLException e) {
            throw new RuntimeException("Cannot find auction by id", e);
        }
    }

    /*
        Lấy tất cả auction.
    */
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

    /*
        Lấy auction đang mở hoặc đang chạy.

        Vì Auction có cả OPEN và RUNNING,
        mình cho lấy cả 2 trạng thái này.
    */
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

    /*
        Cập nhật toàn bộ trạng thái chính của auction.

        Hàm này dùng sau khi:
        - startAuction()
        - closeAuction()
        - cancelAuction()
        - placeBid()
    */
    public boolean update(Auction auction) {
        String sql = "UPDATE auctions SET current_price = ?, highest_bidder_id = ?, status = ? WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            if (auction.getHighestBidder() != null &&
                    !auction.getHighestBidder().hasRole(UserRole.BIDDER)) {
                throw new RuntimeException("Highest bidder must have BIDDER role");
            }

            statement.setDouble(1, auction.getCurrentPrice());

            /*
                highest_bidder_id là INT trong database.
            */
            if (auction.getHighestBidder() == null) {
                statement.setNull(2, Types.INTEGER);
            } else {
                statement.setInt(2, Integer.parseInt(auction.getHighestBidder().getId()));
            }

            statement.setString(3, auction.getStatus().name());

            /*
                auction.id là INT trong database.
                Nhưng trong Java vẫn là String nên parse sang int.
            */
            statement.setInt(4, Integer.parseInt(auction.getId()));

            int affectedRows = statement.executeUpdate();

            return affectedRows > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Cannot update auction", e);
        }
    }

    /*
        Xóa auction theo id.

        auction.id trong database hiện là INT AUTO_INCREMENT.
    */
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

    /*
        Chuyển một dòng trong bảng auctions thành object Auction.
    */
    private Auction mapResultSetToAuction(ResultSet resultSet) throws SQLException {
        /*
            id là INT trong database.
            Nhưng Entity.id trong Java vẫn là String,
            nên lấy ra bằng getInt rồi convert sang String.
        */
        String id = String.valueOf(resultSet.getInt("id"));

        /*
            item_id là INT trong database.
            ItemDAO.findById đang nhận String,
            nên convert int -> String.
        */
        String itemId = String.valueOf(resultSet.getInt("item_id"));

        /*
            seller_id là INT trong database.
            UserDAO.findById đang nhận String,
            nên convert int -> String.
        */
        String sellerId = String.valueOf(resultSet.getInt("seller_id"));

        double currentPrice = resultSet.getDouble("current_price");

        /*
            highest_bidder_id có thể null.
            resultSet.getInt(...) nếu gặp NULL sẽ trả 0,
            nên phải kiểm tra resultSet.wasNull().
        */
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

        /*
            Trước đây:
            User sellerUser = userDAO.findById(sellerId);
            if (!(sellerUser instanceof Seller)) ...
            Seller seller = (Seller) sellerUser;

            Bây giờ:
            seller là User có role SELLER.
        */
        User seller = userDAO.findById(sellerId);

        if (seller == null) {
            throw new RuntimeException("Seller not found for auction");
        }

        if (!seller.hasRole(UserRole.SELLER)) {
            throw new RuntimeException("Seller user does not have SELLER role");
        }

        Auction auction = new Auction(item, seller);

        /*
            Constructor tạo id mới, nên phải set lại id thật trong database.
        */
        auction.setId(id);
        auction.setCurrentPrice(currentPrice);
        auction.setStatus(AuctionStatus.valueOf(statusText));

        /*
            highest_bidder_id có thể null nếu chưa ai bid.

            Trước đây:
            kiểm tra instanceof Bidder rồi ép kiểu Bidder.

            Bây giờ:
            highest bidder là User có role BIDDER.
        */
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
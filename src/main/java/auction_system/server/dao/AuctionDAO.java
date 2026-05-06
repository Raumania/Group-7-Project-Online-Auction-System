package auction_system.server.dao;

import auction_system.server.model.Auction;
import auction_system.server.model.AuctionStatus;
import auction_system.server.model.Bidder;
import auction_system.server.model.Item;
import auction_system.server.model.Seller;
import auction_system.server.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
    */
    public void save(Auction auction) {
        String sql = "INSERT INTO auctions(id, item_id, seller_id, current_price, highest_bidder_id, status) " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, auction.getId());
            statement.setString(2, auction.getItem().getId());
            statement.setString(3, auction.getSeller().getId());
            statement.setDouble(4, auction.getCurrentPrice());

            if (auction.getHighestBidder() == null) {
                statement.setString(5, null);
            } else {
                statement.setString(5, auction.getHighestBidder().getId());
            }

            statement.setString(6, auction.getStatus().name());

            statement.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Cannot save auction", e);
        }
    }

    /*
        Tìm auction theo id.
    */
    public Auction findById(String id) {
        String sql = "SELECT * FROM auctions WHERE id = ?";

        try (Connection connection = DatabaseConnection.getConnection();
             //object chuyển lệnh từ java sang MySQL
             PreparedStatement statement = connection.prepareStatement(sql)) {

            //truyền giá trị vào dấu ? đầu tiên
            statement.setString(1, id);

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

            statement.setDouble(1, auction.getCurrentPrice());

            if (auction.getHighestBidder() == null) {
                statement.setString(2, null);
            } else {
                statement.setString(2, auction.getHighestBidder().getId());
            }

            statement.setString(3, auction.getStatus().name());
            statement.setString(4, auction.getId());

            int affectedRows = statement.executeUpdate();

            return affectedRows > 0;

        } catch (SQLException e) {
            throw new RuntimeException("Cannot update auction", e);
        }
    }

    /*
        Xóa auction theo id.
    */
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

    /*
        Chuyển một dòng trong bảng auctions thành object Auction.
    */
    private Auction mapResultSetToAuction(ResultSet resultSet) throws SQLException {
        String id = resultSet.getString("id");
        String itemId = resultSet.getString("item_id");
        String sellerId = resultSet.getString("seller_id");
        double currentPrice = resultSet.getDouble("current_price");
        String highestBidderId = resultSet.getString("highest_bidder_id");
        String statusText = resultSet.getString("status");

        Item item = itemDAO.findById(itemId);

        if (item == null) {
            throw new RuntimeException("Item not found for auction");
        }

        User sellerUser = userDAO.findById(sellerId);

        if (!(sellerUser instanceof Seller)) {
            throw new RuntimeException("Seller not found for auction");
        }

        Seller seller = (Seller) sellerUser;

        Auction auction = new Auction(item, seller);

        /*
            Constructor tạo id mới, nên phải set lại id thật trong database.
        */
        auction.setId(id);
        auction.setCurrentPrice(currentPrice);
        auction.setStatus(AuctionStatus.valueOf(statusText));

        /*
            highest_bidder_id có thể null nếu chưa ai bid.
        */
        if (highestBidderId != null) {
            User bidderUser = userDAO.findById(highestBidderId);

            if (!(bidderUser instanceof Bidder)) {
                throw new RuntimeException("Highest bidder not found for auction");
            }

            auction.setHighestBidder((Bidder) bidderUser);
        }

        return auction;
    }
}
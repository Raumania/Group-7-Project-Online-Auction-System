package auction_system.server.service;

import auction_system.common.dto.AuctionDTO;
import auction_system.common.enums.ItemType;
import auction_system.common.enums.UserRole;
import auction_system.server.dao.DatabaseConnection;
import auction_system.server.model.Auction;
import auction_system.server.model.User;
import auction_system.server.model.AutoBid;
import auction_system.server.store.AutoBidStore;
import auction_system.server.exception.InvalidBidException;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Integration Tests for Bid and Auto-Bid Workflows")
public class AutoBidIntegrationTest {

    private static UserService userService;
    private static AuctionService auctionService;
    private static BidService bidService;
    private static AutoBidService autoBidService;

    private static String uniqueUsername() {
        return "user_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static User createBidder(BigDecimal balance) {
        String username = uniqueUsername();
        userService.registerUser("Test Bidder", username, "password123", Set.of(UserRole.BIDDER));
        User user = userService.getUserByUsername(username);
        userService.deposit(user.getId(), balance);
        return userService.getUserByUsername(username);
    }

    private static User createSeller() {
        String username = uniqueUsername();
        userService.registerUser("Test Seller", username, "password123", Set.of(UserRole.SELLER));
        return userService.getUserByUsername(username);
    }

    private Auction createRunningAuction(User seller, BigDecimal startingPrice) {
        LocalDateTime start = LocalDateTime.now().minusHours(3);
        LocalDateTime end = LocalDateTime.now().plusHours(6);
        AuctionDTO auctionDTO = new AuctionDTO("Test Item", "Test Desc", ItemType.ELECTRONICS, seller.getId(), startingPrice, start, end);
        Auction auction = new Auction(auctionDTO);
        auctionService.createAuction(auction);
        return auctionService.getAuctionById(auction.getId());
    }

    private Auction createFinishedAuction(User seller, BigDecimal startingPrice) {
        LocalDateTime start = LocalDateTime.now().minusHours(10);
        LocalDateTime end = LocalDateTime.now().minusHours(5);
        AuctionDTO auctionDTO = new AuctionDTO("Test Item", "Test Desc", ItemType.ELECTRONICS, seller.getId(), startingPrice, start, end);
        Auction auction = new Auction(auctionDTO);
        auctionService.createAuction(auction);
        return auctionService.getAuctionById(auction.getId());
    }

    private static void cleanUpAuction(Auction auction) {
        if (auction == null || auction.getId() <= 0) return;
        try { auctionService.cancelAuction(auction.getId()); } catch (RuntimeException ignored) {}
        try { auctionService.deleteAuction(auction.getId()); return; } catch (RuntimeException ignored) {}
        deleteAuctionRows(auction);
    }

    private static void deleteAuctionRows(Auction auction) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            deleteById(connection, "DELETE FROM auto_bid_config WHERE auction_id = ?", auction.getId());
            deleteById(connection, "DELETE FROM bid_transactions WHERE auction_id = ?", auction.getId());
            deleteById(connection, "DELETE FROM auctions WHERE id = ?", auction.getId());
            if (auction.getItemId() > 0) {
                deleteById(connection, "DELETE FROM items WHERE id = ?", auction.getItemId());
            }
        } catch (SQLException ignored) {}
    }

    private static void deleteById(Connection connection, String sql, int id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    private static void cleanUpUser(User user) {
        if (user == null || user.getId() <= 0) return;
        try { userService.removeUser(user.getId()); } catch (RuntimeException ignored) {}
    }

    @BeforeAll
    static void setUp() {
        userService = UserService.getInstance();
        auctionService = AuctionService.getInstance();
        bidService = BidService.getInstance();
        autoBidService = AutoBidService.getInstance();
    }

    private User seller;
    private Auction auction;

    @BeforeEach
    void initAuction() {
        seller = createSeller();
    }

    @AfterEach
    void tearDown() {
        cleanUpAuction(auction);
        cleanUpUser(seller);
    }

    // --- 1. Test Case: Người bán và Người mua không dẫm chân lên nhau ---

    @Test
    @DisplayName("Test 1: Người bán không bị ảnh hưởng tài chính khi có người đặt giá")
    void testSellerBalanceNotAffectedByBid() throws SQLException {
        auction = createRunningAuction(seller, new BigDecimal("100"));
        BigDecimal sellerAvailBefore = userService.getUserById(seller.getId()).getAvailableBalance();
        BigDecimal sellerFrozenBefore = userService.getUserById(seller.getId()).getFrozenBalance();

        User bidder = createBidder(new BigDecimal("1000"));
        bidService.placeBid(auction.getId(), bidder, new BigDecimal("100"));

        User sellerAfter = userService.getUserById(seller.getId());
        assertEquals(0, sellerAvailBefore.compareTo(sellerAfter.getAvailableBalance()));
        assertEquals(0, sellerFrozenBefore.compareTo(sellerAfter.getFrozenBalance()));

        cleanUpUser(bidder);
    }

    @Test
    @DisplayName("Test 2: Người bán không được tự buff giá")
    void testSellerCannotBidOnOwnAuction() {
        auction = createRunningAuction(seller, new BigDecimal("100"));
        
        Exception exception = assertThrows(RuntimeException.class, () -> 
            bidService.placeBid(auction.getId(), seller, new BigDecimal("100"))
        );
        // Expecting exception related to not being able to bid
        assertNotNull(exception);
    }

    // --- 2. Các Edge Cases cho Manual Bid ---

    @Test
    @DisplayName("Test 3: Số dư không đủ (Thiếu tiền vẫn đòi bid)")
    void testBidWithInsufficientBalance() {
        auction = createRunningAuction(seller, new BigDecimal("100"));
        User bidder = createBidder(new BigDecimal("100"));
        
        // Cố tình bid 200k khi số dư chỉ có 100k
        Exception exception = assertThrows(RuntimeException.class, () -> 
            bidService.placeBid(auction.getId(), bidder, new BigDecimal("200"))
        );
        
        User bidderAfter = userService.getUserById(bidder.getId());
        assertEquals(0, new BigDecimal("100").compareTo(bidderAfter.getAvailableBalance()));
        assertEquals(0, BigDecimal.ZERO.compareTo(bidderAfter.getFrozenBalance()));
        assertNull(auctionService.getAuctionById(auction.getId()).getCurrentPrice());

        cleanUpUser(bidder);
    }

    @Test
    @DisplayName("Test 4: Tự đấu giá với chính mình (Self-bidding)")
    void testSelfBidding() throws SQLException {
        auction = createRunningAuction(seller, new BigDecimal("100"));
        User bidder = createBidder(new BigDecimal("1000"));
        
        bidService.placeBid(auction.getId(), bidder, new BigDecimal("100"));
        
        // Tự đấu giá thêm phát nữa
        Exception exception = assertThrows(RuntimeException.class, () -> 
            bidService.placeBid(auction.getId(), bidder, new BigDecimal("110"))
        );
        
        cleanUpUser(bidder);
    }

    @Test
    @DisplayName("Test 5: Đặt giá thấp hơn luật sàn")
    void testBidLowerThanMinIncrement() throws SQLException {
        auction = createRunningAuction(seller, new BigDecimal("100"));
        User bidder1 = createBidder(new BigDecimal("1000"));
        User bidder2 = createBidder(new BigDecimal("1000"));
        
        bidService.placeBid(auction.getId(), bidder1, new BigDecimal("100"));
        // Current price 100 -> increment 2.5 (from getBidIncrement) -> min is 102.5
        // Bidder 2 cố tình bid 101
        Exception exception = assertThrows(RuntimeException.class, () -> 
            bidService.placeBid(auction.getId(), bidder2, new BigDecimal("101"))
        );

        cleanUpUser(bidder1);
        cleanUpUser(bidder2);
    }

    @Test
    @DisplayName("Test 6: Cố chấp bid khi phòng đã đóng")
    void testBidOnFinishedAuction() {
        auction = createFinishedAuction(seller, new BigDecimal("100"));
        User bidder = createBidder(new BigDecimal("1000"));
        
        Exception exception = assertThrows(RuntimeException.class, () -> 
            bidService.placeBid(auction.getId(), bidder, new BigDecimal("150"))
        );

        cleanUpUser(bidder);
    }

    // --- 3. Các Edge Cases cho Auto-Bid ---

    @Test
    @DisplayName("Test 7: Bật Auto-bid nhưng số dư không đủ đóng băng")
    void testAutoBidInsufficientBalance() {
        auction = createRunningAuction(seller, new BigDecimal("100"));
        User bidder = createBidder(new BigDecimal("500"));
        
        // Cố setup Max Bid = 1000 trong khi balance chỉ 500
        Exception exception = assertThrows(RuntimeException.class, () -> 
            autoBidService.setAutoBid(bidder.getId(), auction.getId(), new BigDecimal("1000"), new BigDecimal("10"))
        );
        
        AutoBid config = autoBidService.getAutoBidConfig(bidder.getId(), auction.getId());
        assertNull(config, "Cấu hình AutoBid không được phép lưu");

        cleanUpUser(bidder);
    }

    @Test
    @DisplayName("Test 8: Kịch bản Thắng an toàn (B giữ nguyên tiền đóng băng, C bị outbid)")
    void testAutoBidSafeWin() throws SQLException {
        auction = createRunningAuction(seller, new BigDecimal("100"));
        User bidderB = createBidder(new BigDecimal("1000"));
        User bidderC = createBidder(new BigDecimal("1000"));
        
        // Bật AutoBid cho B
        autoBidService.setAutoBid(bidderB.getId(), auction.getId(), new BigDecimal("500"), new BigDecimal("10"));
        
        // C vào bid 200
        bidService.placeBid(auction.getId(), bidderC, new BigDecimal("200"));
        
        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}

        Auction updatedAuction = auctionService.getAuctionById(auction.getId());
        // C bị outbid, B lên Top 1 với 200 + 10 = 210 (hoặc tương tự theo luật)
        assertEquals(bidderB.getId(), updatedAuction.getHighestBidderId());
        
        User bAfter = userService.getUserById(bidderB.getId());
        // Tiền đóng băng của B vẫn giữ nguyên 500
        assertEquals(0, new BigDecimal("500").compareTo(bAfter.getFrozenBalance()));

        cleanUpUser(bidderB);
        cleanUpUser(bidderC);
    }

    @Test
    @DisplayName("Test 9: Chạm đúng ngưỡng Max Bid (Hết đạn)")
    void testAutoBidHitMaxLimit() throws SQLException {
        auction = createRunningAuction(seller, new BigDecimal("100"));
        User bidderB = createBidder(new BigDecimal("1000"));
        User bidderC = createBidder(new BigDecimal("1000"));
        
        autoBidService.setAutoBid(bidderB.getId(), auction.getId(), new BigDecimal("500"), new BigDecimal("50"));
        
        // C bid 480
        // 480 + 50 = 530 > 500. Theo lý thuyết mới, kiểm tra minIncrement. 480 + 5 = 485 <= 500. Đặt 485 và tắt Auto-bid.
        bidService.placeBid(auction.getId(), bidderC, new BigDecimal("480"));
        
        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}

        Auction updatedAuction = auctionService.getAuctionById(auction.getId());
        assertEquals(bidderB.getId(), updatedAuction.getHighestBidderId());
        assertEquals(0, new BigDecimal("485").compareTo(updatedAuction.getCurrentPrice()));
        
        AutoBid configB = autoBidService.getAutoBidConfig(bidderB.getId(), auction.getId());
        assertNull(configB, "Cấu hình Auto-bid của B phải bị tắt (tức là không còn trong active list)");

        cleanUpUser(bidderB);
        cleanUpUser(bidderC);
    }

    @Test
    @DisplayName("Test 10: Rã đông tiền thành công khi bị vượt mặt")
    void testAutoBidUnfreezeWhenOutbid() throws SQLException {
        auction = createRunningAuction(seller, new BigDecimal("100"));
        User bidderB = createBidder(new BigDecimal("1000")); // Có sẵn 1000, sau khi autobid 500 -> frozen = 500, avail = 500
        User bidderC = createBidder(new BigDecimal("1000"));
        
        autoBidService.setAutoBid(bidderB.getId(), auction.getId(), new BigDecimal("500"), new BigDecimal("10"));
        
        // C bid hẳn 600
        bidService.placeBid(auction.getId(), bidderC, new BigDecimal("600"));
        
        try { Thread.sleep(500); } catch (InterruptedException ignored) {}

        Auction updatedAuction = auctionService.getAuctionById(auction.getId());
        assertEquals(bidderC.getId(), updatedAuction.getHighestBidderId());
        
        AutoBid configB = autoBidService.getAutoBidConfig(bidderB.getId(), auction.getId());
        assertNull(configB, "Cấu hình Auto-bid của B phải bị tắt (tức là không còn trong active list)");
        
        User bAfter = userService.getUserById(bidderB.getId());
        assertEquals(0, BigDecimal.ZERO.compareTo(bAfter.getFrozenBalance()));
        assertEquals(0, new BigDecimal("1000").compareTo(bAfter.getAvailableBalance()));

        cleanUpUser(bidderB);
        cleanUpUser(bidderC);
    }

    @Test
    @DisplayName("Test 11: Hai Auto-bidder đụng độ - Khoảng cách sát nút")
    void testTwoAutoBiddersClash() throws SQLException {
        auction = createRunningAuction(seller, new BigDecimal("100"));
        User bidderA = createBidder(new BigDecimal("2000"));
        User bidderB = createBidder(new BigDecimal("2000"));
        
        autoBidService.setAutoBid(bidderA.getId(), auction.getId(), new BigDecimal("1000"), new BigDecimal("10"));
        autoBidService.setAutoBid(bidderB.getId(), auction.getId(), new BigDecimal("1025"), new BigDecimal("10"));
        
        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}

        Auction updatedAuction = auctionService.getAuctionById(auction.getId());
        assertEquals(bidderB.getId(), updatedAuction.getHighestBidderId());
        assertEquals(0, new BigDecimal("1025").compareTo(updatedAuction.getCurrentPrice()));
        
        AutoBid configA = autoBidService.getAutoBidConfig(bidderA.getId(), auction.getId());
        AutoBid configB = autoBidService.getAutoBidConfig(bidderB.getId(), auction.getId());
        
        assertNull(configA, "Cấu hình của A phải bị tắt");
        assertNull(configB, "Cấu hình của B phải bị tắt");
        
        User aAfter = userService.getUserById(bidderA.getId());
        assertEquals(0, BigDecimal.ZERO.compareTo(aAfter.getFrozenBalance()));
        assertEquals(0, new BigDecimal("2000").compareTo(aAfter.getAvailableBalance()));

        cleanUpUser(bidderA);
        cleanUpUser(bidderB);
    }
}

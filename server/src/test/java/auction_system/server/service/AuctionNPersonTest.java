package auction_system.server.service;

import auction_system.common.dto.AuctionDTO;
import auction_system.common.enums.ItemType;
import auction_system.common.enums.UserRole;
import auction_system.server.dao.DatabaseConnection;
import auction_system.server.model.Auction;
import auction_system.server.model.User;
import org.junit.jupiter.api.*;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("N-Person and Complex Auction Logic Tests")
public class AuctionNPersonTest {

    private static UserService userService;
    private static AuctionService auctionService;
    private static BidService bidService;
    private static AutoBidService autoBidService;

    private static String uniqueUsername() {
        return "user_n_" + UUID.randomUUID().toString().substring(0, 6);
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
        AuctionDTO auctionDTO = new AuctionDTO("Test N-Person", "Desc", ItemType.ELECTRONICS, seller.getId(), startingPrice, start, end);
        Auction auction = new Auction(auctionDTO);
        auctionService.createAuction(auction);
        return auctionService.getAuctionById(auction.getId());
    }

    private static void cleanUpAuction(Auction auction) {
        if (auction == null || auction.getId() <= 0) return;
        try { auctionService.cancelAuction(auction.getId()); } catch (RuntimeException ignored) {}
        try { auctionService.deleteAuction(auction.getId()); return; } catch (RuntimeException ignored) {}
        try (Connection connection = DatabaseConnection.getConnection()) {
            try (PreparedStatement s = connection.prepareStatement("DELETE FROM auto_bid_config WHERE auction_id = ?")) { s.setInt(1, auction.getId()); s.executeUpdate(); }
            try (PreparedStatement s = connection.prepareStatement("DELETE FROM bid_transactions WHERE auction_id = ?")) { s.setInt(1, auction.getId()); s.executeUpdate(); }
            try (PreparedStatement s = connection.prepareStatement("DELETE FROM auctions WHERE id = ?")) { s.setInt(1, auction.getId()); s.executeUpdate(); }
        } catch (SQLException ignored) {}
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

    private void sleepEngine() {
        try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
    }

    @Test
    @DisplayName("N=3: 3 Autobidders đấu tay 3 -> Mạnh nhất thắng, giá bằng runner-up")
    void test_3Autobids() throws SQLException {
        auction = createRunningAuction(seller, new BigDecimal("10"));
        User weak = createBidder(new BigDecimal("1000")); // max 300, inc 10
        User mid = createBidder(new BigDecimal("1000"));  // max 500, inc 20
        User strong = createBidder(new BigDecimal("1000"));// max 800, inc 50

        autoBidService.setAutoBid(weak.getId(), auction.getId(), new BigDecimal("300"), new BigDecimal("10"));
        sleepEngine();
        autoBidService.setAutoBid(mid.getId(), auction.getId(), new BigDecimal("500"), new BigDecimal("20"));
        sleepEngine();
        autoBidService.setAutoBid(strong.getId(), auction.getId(), new BigDecimal("800"), new BigDecimal("50"));
        sleepEngine();

        Auction updated = auctionService.getAuctionById(auction.getId());
        assertEquals(strong.getId(), updated.getHighestBidderId());
        
        // Mid là runner-up (max 500). Strong có inc 50.
        // Giá sẽ là: 500 + max(Strong.inc, minInc) = 550.
        assertEquals(0, new BigDecimal("550").compareTo(updated.getCurrentPrice()));

        cleanUpUser(weak); cleanUpUser(mid); cleanUpUser(strong);
    }

    @Test
    @DisplayName("N=5: Chuỗi thao tác phức tạp (Autobid đụng độ -> Manual xen ngang -> Autobid trùm cuối)")
    void test_ChainReaction() throws SQLException {
        auction = createRunningAuction(seller, new BigDecimal("10"));
        User autoA = createBidder(new BigDecimal("3000")); // max 1000, inc 100
        User autoB = createBidder(new BigDecimal("3000")); // max 900, inc 50
        User autoC = createBidder(new BigDecimal("3000")); // max 800, inc 20
        User manual = createBidder(new BigDecimal("3000"));
        User autoD = createBidder(new BigDecimal("3000")); // max 2000, inc 200

        autoBidService.setAutoBid(autoA.getId(), auction.getId(), new BigDecimal("1000"), new BigDecimal("100"));
        autoBidService.setAutoBid(autoB.getId(), auction.getId(), new BigDecimal("900"), new BigDecimal("50"));
        autoBidService.setAutoBid(autoC.getId(), auction.getId(), new BigDecimal("800"), new BigDecimal("20"));
        sleepEngine();

        // Lúc này AutoA đang dẫn đầu ở giá 1000 (do đạp lên AutoB 900)
        // Manual nhảy vào đặt 1200
        bidService.placeBid(auction.getId(), manual, new BigDecimal("1200"));
        sleepEngine();

        // AutoD xuất hiện vào phút chót với max 2000, inc 200
        autoBidService.setAutoBid(autoD.getId(), auction.getId(), new BigDecimal("2000"), new BigDecimal("200"));
        sleepEngine();

        Auction updated = auctionService.getAuctionById(auction.getId());
        assertEquals(autoD.getId(), updated.getHighestBidderId());
        
        // AutoD sẽ vượt Manual(1200) bằng bước nhảy của D (200) -> 1400
        assertEquals(0, new BigDecimal("1400").compareTo(updated.getCurrentPrice()));

        cleanUpUser(autoA); cleanUpUser(autoB); cleanUpUser(autoC); cleanUpUser(manual); cleanUpUser(autoD);
    }

    @Test
    @DisplayName("N=2: 2 Autobidders có MaxBid bằng nhau -> Ai thiết lập trước thắng")
    void test_SameMaxBid_TieBreak() throws SQLException {
        auction = createRunningAuction(seller, new BigDecimal("10"));
        User first = createBidder(new BigDecimal("1000")); // Đặt trước
        User second = createBidder(new BigDecimal("1000")); // Đặt sau

        autoBidService.setAutoBid(first.getId(), auction.getId(), new BigDecimal("500"), new BigDecimal("10"));
        sleepEngine();
        autoBidService.setAutoBid(second.getId(), auction.getId(), new BigDecimal("500"), new BigDecimal("50"));
        sleepEngine();

        Auction updated = auctionService.getAuctionById(auction.getId());
        assertEquals(first.getId(), updated.getHighestBidderId());
        // Vì maxBid bằng nhau, người thắng sẽ phải all-in ở mức 500
        assertEquals(0, new BigDecimal("500").compareTo(updated.getCurrentPrice()));

        cleanUpUser(first); cleanUpUser(second);
    }

    @Test
    @DisplayName("N=5: 5 Manual Bidders đặt giá liên tiếp")
    void test_5ManualSequential() throws SQLException {
        auction = createRunningAuction(seller, new BigDecimal("10"));
        User m1 = createBidder(new BigDecimal("1000"));
        User m2 = createBidder(new BigDecimal("1000"));
        User m3 = createBidder(new BigDecimal("1000"));
        User m4 = createBidder(new BigDecimal("1000"));
        User m5 = createBidder(new BigDecimal("1000"));

        bidService.placeBid(auction.getId(), m1, new BigDecimal("20"));
        bidService.placeBid(auction.getId(), m2, new BigDecimal("40"));
        bidService.placeBid(auction.getId(), m3, new BigDecimal("60"));
        bidService.placeBid(auction.getId(), m4, new BigDecimal("100"));
        bidService.placeBid(auction.getId(), m5, new BigDecimal("500"));
        
        Auction updated = auctionService.getAuctionById(auction.getId());
        assertEquals(m5.getId(), updated.getHighestBidderId());
        assertEquals(0, new BigDecimal("500").compareTo(updated.getCurrentPrice()));

        cleanUpUser(m1); cleanUpUser(m2); cleanUpUser(m3); cleanUpUser(m4); cleanUpUser(m5);
    }

    @Test
    @DisplayName("N=3: Bị Sniper (kẻ bắn tỉa) hớt tay trên Autobid phút cuối")
    void test_SniperOutbidsAutobid() throws SQLException {
        auction = createRunningAuction(seller, new BigDecimal("10"));
        User auto = createBidder(new BigDecimal("1000")); // max 400
        User manual1 = createBidder(new BigDecimal("1000"));
        User sniper = createBidder(new BigDecimal("1000"));

        autoBidService.setAutoBid(auto.getId(), auction.getId(), new BigDecimal("400"), new BigDecimal("10"));
        sleepEngine();

        // Kéo giá lên 350
        bidService.placeBid(auction.getId(), manual1, new BigDecimal("350"));
        sleepEngine();

        // Sniper lao vào nhảy hẳn lên 450 (vượt quá max của auto)
        bidService.placeBid(auction.getId(), sniper, new BigDecimal("450"));
        sleepEngine();

        Auction updated = auctionService.getAuctionById(auction.getId());
        assertEquals(sniper.getId(), updated.getHighestBidderId());
        assertEquals(0, new BigDecimal("450").compareTo(updated.getCurrentPrice()));

        cleanUpUser(auto); cleanUpUser(manual1); cleanUpUser(sniper);
    }

    @Test
    @DisplayName("N=3: Sniper thất bại do bị Autobid phản công tức thì")
    void test_SniperFailsAgainstAutobid() throws SQLException {
        auction = createRunningAuction(seller, new BigDecimal("10"));
        User auto = createBidder(new BigDecimal("1000")); // max 500, inc 20
        User manual1 = createBidder(new BigDecimal("1000"));
        User sniper = createBidder(new BigDecimal("1000"));

        autoBidService.setAutoBid(auto.getId(), auction.getId(), new BigDecimal("500"), new BigDecimal("20"));
        sleepEngine();

        bidService.placeBid(auction.getId(), manual1, new BigDecimal("300"));
        sleepEngine(); // Auto đang dẫn đầu tại 320 (300+20)

        // Sniper nhảy vào 450 (vẫn trong tầm phủ sóng của auto)
        bidService.placeBid(auction.getId(), sniper, new BigDecimal("450"));
        sleepEngine();

        // Auto lập tức phản công: 450 + 20 = 470
        Auction updated = auctionService.getAuctionById(auction.getId());
        assertEquals(auto.getId(), updated.getHighestBidderId());
        assertEquals(0, new BigDecimal("470").compareTo(updated.getCurrentPrice()));

        cleanUpUser(auto); cleanUpUser(manual1); cleanUpUser(sniper);
    }

    @Test
    @DisplayName("Bug check: User 1 manual 400, User 2 Auto 500")
    void test_UserBugReproduction() throws SQLException {
        auction = createRunningAuction(seller, new BigDecimal("10"));
        User user1 = createBidder(new BigDecimal("10000"));
        User user2 = createBidder(new BigDecimal("10000"));

        // User 2 bids 36
        bidService.placeBid(auction.getId(), user2, new BigDecimal("36"));
        sleepEngine();

        // User 1 bids 400
        bidService.placeBid(auction.getId(), user1, new BigDecimal("400"));
        sleepEngine();

        // Now User 2 sets AutoBid to 500
        autoBidService.setAutoBid(user2.getId(), auction.getId(), new BigDecimal("500"), new BigDecimal("10"));
        sleepEngine();

        Auction updated = auctionService.getAuctionById(auction.getId());
        System.out.println("TEST BUG Current Price: " + updated.getCurrentPrice());
        System.out.println("TEST BUG Leader ID: " + updated.getHighestBidderId() + " (User 2 is " + user2.getId() + ")");
        
        assertEquals(user2.getId(), updated.getHighestBidderId(), "User 2 should win");
        assertEquals(0, new BigDecimal("410").compareTo(updated.getCurrentPrice()), "Price should be 410");

        cleanUpUser(user1); cleanUpUser(user2);
    }
}

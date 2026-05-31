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

@DisplayName("Theoretical Scenarios Test for Proxy Bidding (Rule Mapping)")
public class AutoBidTheoreticalScenariosTest {

    private static UserService userService;
    private static AuctionService auctionService;
    private static BidService bidService;
    private static AutoBidService autoBidService;

    private static String uniqueUsername() {
        return "user_theo_" + UUID.randomUUID().toString().substring(0, 6);
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
    @DisplayName("Trường hợp 2.1 - 1a: 1 Manual duy nhất -> Thắng tại m")
    void test1a_OneManual() throws SQLException {
        auction = createRunningAuction(seller, new BigDecimal("10"));
        User manual = createBidder(new BigDecimal("1000"));

        bidService.placeBid(auction.getId(), manual, new BigDecimal("20"));
        
        Auction updated = auctionService.getAuctionById(auction.getId());
        assertEquals(manual.getId(), updated.getHighestBidderId());
        assertEquals(0, new BigDecimal("20").compareTo(updated.getCurrentPrice()));

        cleanUpUser(manual);
    }

    @Test
    @DisplayName("Trường hợp 2.1 - 1b: 1 Autobid duy nhất -> Thắng ngay tại starting price")
    void test1b_OneAuto() throws SQLException {
        auction = createRunningAuction(seller, new BigDecimal("10"));
        User auto = createBidder(new BigDecimal("1000"));

        autoBidService.setAutoBid(auto.getId(), auction.getId(), new BigDecimal("50"), new BigDecimal("10"));
        sleepEngine();

        Auction updated = auctionService.getAuctionById(auction.getId());
        assertEquals(auto.getId(), updated.getHighestBidderId());
        // Khi không có ai cạnh tranh, giá chỉ nhảy lên bằng đúng startingPrice
        assertEquals(0, new BigDecimal("10").compareTo(updated.getCurrentPrice()));

        cleanUpUser(auto);
    }

    @Test
    @DisplayName("Trường hợp A: Manual vs Manual -> m_h > m_s, H thắng ở m_h")
    void testA_ManualVsManual() throws SQLException {
        auction = createRunningAuction(seller, new BigDecimal("10"));
        User s = createBidder(new BigDecimal("1000"));
        User h = createBidder(new BigDecimal("1000"));

        bidService.placeBid(auction.getId(), s, new BigDecimal("20"));
        bidService.placeBid(auction.getId(), h, new BigDecimal("30"));
        
        Auction updated = auctionService.getAuctionById(auction.getId());
        assertEquals(h.getId(), updated.getHighestBidderId());
        assertEquals(0, new BigDecimal("30").compareTo(updated.getCurrentPrice()));

        cleanUpUser(s); cleanUpUser(h);
    }

    @Test
    @DisplayName("Trường hợp B: Manual(H) vs Autobid(S) -> A_s < m_h -> H thắng ở m_h")
    void testB_ManualVsAutoWeaker() throws SQLException {
        auction = createRunningAuction(seller, new BigDecimal("10"));
        User s = createBidder(new BigDecimal("1000")); // Autobid
        User h = createBidder(new BigDecimal("1000")); // Manual

        autoBidService.setAutoBid(s.getId(), auction.getId(), new BigDecimal("40"), new BigDecimal("5"));
        sleepEngine();

        // H nhảy vào bid thủ công 50. Vì 50 > 40 (max của S), S không thể counter
        bidService.placeBid(auction.getId(), h, new BigDecimal("50"));
        sleepEngine();

        Auction updated = auctionService.getAuctionById(auction.getId());
        assertEquals(h.getId(), updated.getHighestBidderId());
        assertEquals(0, new BigDecimal("50").compareTo(updated.getCurrentPrice()));

        cleanUpUser(s); cleanUpUser(h);
    }

    @Test
    @DisplayName("Trường hợp C1: Autobid(H) vs Manual(S) -> A_h >= m_s + inc_h")
    void testC1_AutoStrongVsManual() throws SQLException {
        auction = createRunningAuction(seller, new BigDecimal("10"));
        User h = createBidder(new BigDecimal("1000")); // Autobid
        User s = createBidder(new BigDecimal("1000")); // Manual

        autoBidService.setAutoBid(h.getId(), auction.getId(), new BigDecimal("100"), new BigDecimal("10"));
        sleepEngine();

        // S nhảy vào bid 50. H counter bằng 50 + max(inc_h, platformMin)
        bidService.placeBid(auction.getId(), s, new BigDecimal("50"));
        sleepEngine();

        Auction updated = auctionService.getAuctionById(auction.getId());
        assertEquals(h.getId(), updated.getHighestBidderId());
        // 50 + 10 = 60
        assertEquals(0, new BigDecimal("60").compareTo(updated.getCurrentPrice()));

        cleanUpUser(h); cleanUpUser(s);
    }

    @Test
    @DisplayName("Trường hợp C2: Autobid(H) vs Manual(S) -> A_h đủ thắng S nhưng không đủ bước inc_h -> Hệ thống cứu bằng minIncrement")
    void testC2_AutoVsManual_AutoSavedByMinInc() throws SQLException {
        auction = createRunningAuction(seller, new BigDecimal("10"));
        User h = createBidder(new BigDecimal("1000")); // Autobid
        User s = createBidder(new BigDecimal("1000")); // Manual

        autoBidService.setAutoBid(h.getId(), auction.getId(), new BigDecimal("55"), new BigDecimal("20"));
        sleepEngine();

        // S bid 50. H muốn nhảy 50 + 20 = 70. Nhưng 70 > 55 (max).
        // Theo lý thuyết C2 chặt chẽ: H không vượt nổi S, S thắng ở 50.
        // NHƯNG Proxy Bidding của chúng ta thông minh hơn: Cứu H bằng cách dùng platform min
        // Min inc tại 50 thường là 2.5 hoặc 5. Giả sử 5. 50 + 5 = 55 <= 55. H "all-in" và thắng ở 55!
        bidService.placeBid(auction.getId(), s, new BigDecimal("50"));
        sleepEngine();

        Auction updated = auctionService.getAuctionById(auction.getId());
        assertEquals(h.getId(), updated.getHighestBidderId());
        
        // Final price phụ thuộc vào minIncrement. Tại m=50, min là 2.5, nên final = 52.5.
        BigDecimal expectedMinInc = bidService.getBidIncrement(new BigDecimal("50")); // 2.5
        BigDecimal expectedFinal = new BigDecimal("50").add(expectedMinInc);
        assertEquals(0, expectedFinal.compareTo(updated.getCurrentPrice()));

        cleanUpUser(h); cleanUpUser(s);
    }

    @Test
    @DisplayName("Trường hợp D1: Autobid(H) vs Autobid(S) -> H thắng bằng runnerUpMax + increment")
    void testD1_AutoVsAuto() throws SQLException {
        auction = createRunningAuction(seller, new BigDecimal("10"));
        User s = createBidder(new BigDecimal("1000")); // S: max=80
        User h = createBidder(new BigDecimal("1000")); // H: max=100

        autoBidService.setAutoBid(s.getId(), auction.getId(), new BigDecimal("80"), new BigDecimal("5"));
        sleepEngine();
        autoBidService.setAutoBid(h.getId(), auction.getId(), new BigDecimal("100"), new BigDecimal("10"));
        sleepEngine();

        Auction updated = auctionService.getAuctionById(auction.getId());
        assertEquals(h.getId(), updated.getHighestBidderId());
        // Theo proxy bidding: H sẽ trả runnerUpMax(80) + max(H.inc=10, min=4) = 90
        assertEquals(0, new BigDecimal("90").compareTo(updated.getCurrentPrice()));

        cleanUpUser(h); cleanUpUser(s);
    }

    @Test
    @DisplayName("Trường hợp D2: Autobid(H) vs Autobid(S) -> inc_h quá lớn nhưng H vẫn thắng nhờ fallback minIncrement")
    void testD2_AutoVsAuto_SavedByFallback() throws SQLException {
        auction = createRunningAuction(seller, new BigDecimal("10"));
        User s = createBidder(new BigDecimal("1000")); // S: max=80
        User h = createBidder(new BigDecimal("1000")); // H: max=100

        autoBidService.setAutoBid(s.getId(), auction.getId(), new BigDecimal("80"), new BigDecimal("5"));
        sleepEngine();
        // H có inc_h = 50. Khi đấu tay đôi, runnerUpMax = 80.
        // target = 80 + 50 = 130 > 100.
        // H sẽ dùng fallback: 80 + minInc(4.0) = 84.
        autoBidService.setAutoBid(h.getId(), auction.getId(), new BigDecimal("100"), new BigDecimal("50"));
        sleepEngine();

        Auction updated = auctionService.getAuctionById(auction.getId());
        assertEquals(h.getId(), updated.getHighestBidderId());
        
        BigDecimal expectedMinInc = bidService.getBidIncrement(new BigDecimal("80")); // 4.0
        BigDecimal expectedFinal = new BigDecimal("80").add(expectedMinInc);
        assertEquals(0, expectedFinal.compareTo(updated.getCurrentPrice()));

        cleanUpUser(h); cleanUpUser(s);
    }

    @Test
    @DisplayName("Trường hợp E: Nhiễu từ người thứ 3 (T) đẩy giá lên cho AutoBid")
    void testE_ThirdPartyPush() throws SQLException {
        auction = createRunningAuction(seller, new BigDecimal("10"));
        User s = createBidder(new BigDecimal("1000")); // S auto 90
        User h = createBidder(new BigDecimal("1000")); // H auto 100
        User t = createBidder(new BigDecimal("1000")); // T manual 95

        autoBidService.setAutoBid(s.getId(), auction.getId(), new BigDecimal("90"), new BigDecimal("1"));
        sleepEngine();
        autoBidService.setAutoBid(h.getId(), auction.getId(), new BigDecimal("100"), new BigDecimal("1"));
        sleepEngine();

        // Lúc này H và S đấu nhau -> H thắng ở 90 + 1 = 91 (nếu minInc <= 1, thực tế minInc ở 90 là 4.5 -> H thắng ở 94.5)
        // Để dễ, giả sử đấu nhau bình thường
        
        // T (Manual) nhảy vào đẩy giá lên 95
        bidService.placeBid(auction.getId(), t, new BigDecimal("95"));
        sleepEngine();

        // H counter T: 95 + max(H.inc(1), minInc(4.75)) = 95 + 4.75 = 99.75
        Auction updated = auctionService.getAuctionById(auction.getId());
        assertEquals(h.getId(), updated.getHighestBidderId());
        
        BigDecimal minInc = bidService.getBidIncrement(new BigDecimal("95"));
        BigDecimal finalPrice = new BigDecimal("95").add(minInc); // Vì 1 < 4.75
        assertEquals(0, finalPrice.compareTo(updated.getCurrentPrice()));

        cleanUpUser(s); cleanUpUser(h); cleanUpUser(t);
    }
}

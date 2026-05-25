package auction_system.server.service;

import auction_system.common.dto.AuctionDTO;
import auction_system.common.enums.ItemType;
import auction_system.common.enums.UserRole;
import auction_system.server.dao.DatabaseConnection;
import auction_system.server.model.Auction;
import auction_system.server.model.User;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class BidServiceTest {

    // ═══════════════════════════════════════════════════════
    // getBidIncrement() không cần DB → test độc lập hoàn toàn
    // placeBid() cần DB thật → mỗi test tạo data riêng
    // ═══════════════════════════════════════════════════════

    private static UserService userService;
    private static AuctionService auctionService;
    private static BidService bidService;

    // Helper sinh username duy nhất, tránh duplicate
    private static String uniqueUsername() {
        return "user_" + UUID.randomUUID().toString().substring(0, 8);
    }

    // Helper tạo bidder hợp lệ có đủ số dư
    private static User createBidder(BigDecimal balance) {
        String username = uniqueUsername();
        userService.registerUser("Test User", username, "password123",
                Set.of(UserRole.BIDDER));

        User user = userService.getUserByUsername(username);
        userService.deposit(user.getId(), balance);

        // Lấy lại lần nữa để balance được cập nhật
        return userService.getUserByUsername(username);
    }



    // Helper tạo auction đang RUNNING
    private Auction createRunningAuction(User seller, BigDecimal startingPrice) {
        // Thời gian: đã bắt đầu 1 giờ trước, còn 23 giờ nữa mới kết thúc
        LocalDateTime start = LocalDateTime.now().minusHours(3);
        LocalDateTime end   = LocalDateTime.now().plusHours(6);

        int serllerId = seller.getId();
        AuctionDTO auctionDTO = new AuctionDTO(
                "MacBook Air M2",
                "Laptop dùng cho học tập và lập trình",
                ItemType.ELECTRONICS,
                serllerId,
                startingPrice,
                start,
                end
        );
        Auction auction = new Auction(auctionDTO);
        auctionService.createAuction(auction);
        return auctionService.getAuctionById(auction.getId());
    }

    private Auction createOpenAuction(User seller, BigDecimal startingPrice) {
        // Thời gian: sẽ bắt đầu 3 giờ trước, còn 10 giờ nữa mới kết thúc
        LocalDateTime start = LocalDateTime.now().plusHours(3);
        LocalDateTime end   = LocalDateTime.now().plusHours(10);

        int serllerId = seller.getId();
        AuctionDTO auctionDTO = new AuctionDTO("Ghế Gaming RGB",
                "Ghế gaming công thái học",
                ItemType.ELECTRONICS,
                serllerId,
                startingPrice,
                start,
                end
        );
        Auction auction = new Auction(auctionDTO);
        auctionService.createAuction(auction);
        return auctionService.getAuctionById(auction.getId());
    }

    private Auction createFinishedAuction(User seller, BigDecimal startingPrice) {
        // Thời gian: đã bắt đầu 10 giờ trước, 5 giờ trước đã kết thúc
        LocalDateTime start = LocalDateTime.now().minusHours(10);
        LocalDateTime end   = LocalDateTime.now().minusHours(5);

        int serllerId = seller.getId();
        AuctionDTO auctionDTO = new AuctionDTO("Bàn phím cơ Keychron K8",
                "Switch brown, LED trắng",
                ItemType.ELECTRONICS,
                serllerId,
                startingPrice,
                start,
                end
        );
        Auction auction = new Auction(auctionDTO);
        auctionService.createAuction(auction);
        return auctionService.getAuctionById(auction.getId());
    }

    private static void assertBigDecimalValueEquals(String expected, BigDecimal actual) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }

    private static void cleanUpAuction(Auction auction) {
        if (auction == null || auction.getId() <= 0) {
            return;
        }

        try {
            auctionService.cancelAuction(auction.getId());
        } catch (RuntimeException ignored) {
        }

        try {
            auctionService.deleteAuction(auction.getId());
            return;
        } catch (RuntimeException ignored) {
        }

        deleteAuctionRows(auction);
    }

    private static void deleteAuctionRows(Auction auction) {
        try (Connection connection = DatabaseConnection.getConnection()) {
            deleteById(connection, "DELETE FROM bid_transactions WHERE auction_id = ?", auction.getId());
            deleteById(connection, "DELETE FROM auctions WHERE id = ?", auction.getId());

            if (auction.getItemId() > 0) {
                deleteById(connection, "DELETE FROM items WHERE id = ?", auction.getItemId());
            }
        } catch (SQLException ignored) {
        }
    }

    private static void deleteById(Connection connection, String sql, int id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    private static void cleanUpUser(User user) {
        if (user == null || user.getId() <= 0) {
            return;
        }

        try {
            userService.removeUser(user.getId());
        } catch (RuntimeException ignored) {
        }
    }



    @BeforeAll
    static void setUp() {
        userService   = UserService.getInstance();
        auctionService = AuctionService.getInstance();
        bidService    = BidService.getInstance();
    }


    // ═══════════════════════════════════════════════════════
    // NHÓM 1: getBidIncrement() — Không cần DB, test thuần túy
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("getBidIncrement() — BVA tại các biên giá")
    class GetBidIncrementTests {

        // Biên dưới của mỗi mức — giá trị NẰM TRONG mức đó
        @ParameterizedTest
        @DisplayName("BVA: Đúng các mức biên → increment đúng")
        @CsvSource({
                // price            , expectedIncrement
                "0.01,               0.05",   // < 1        → 0.05
                "1.00,               0.25",   // biên vào mức [1, 5)
                "5.00,               0.50",   // biên vào mức [5, 25)
                "25.00,              1",      // biên vào mức [25, 100)
                "100.00,             2.5",    // biên vào mức [100, 250)
                "250.00,             5",      // biên vào mức [250, 500)
                "500.00,             10",     // biên vào mức [500, 1000)
                "1000.00,            25"      // >= 1000    → 25
        })
        void exactBoundary_shouldReturnCorrectIncrement(
                String price, String expectedIncrement) {

            BigDecimal result = bidService.getBidIncrement(new BigDecimal(price));

            assertBigDecimalValueEquals(expectedIncrement, result);
        }

        // Biên trên của mức trước — 1 đơn vị NHỎ NHẤT trước khi nhảy mức
        @ParameterizedTest
        @DisplayName("BVA: Ngay dưới biên → vẫn thuộc mức cũ")
        @CsvSource({
                "0.99,   0.05",   // ngay dưới 1    → vẫn là 0.05
                "4.99,   0.25",   // ngay dưới 5    → vẫn là 0.25
                "24.99,  0.50",   // ngay dưới 25   → vẫn là 0.5
                "99.99,  1",      // ngay dưới 100  → vẫn là 1
                "249.99, 2.5",    // ngay dưới 250  → vẫn là 2.5
                "499.99, 5",      // ngay dưới 500  → vẫn là 5
                "999.99, 10"      // ngay dưới 1000 → vẫn là 10
        })
        void justBelowBoundary_shouldReturnLowerIncrement(
                String price, String expectedIncrement) {

            BigDecimal result = bidService.getBidIncrement(new BigDecimal(price));

            assertBigDecimalValueEquals(expectedIncrement, result);
        }

        @Test
        @DisplayName("EP: Giá rất lớn → increment = 25")
        void veryHighPrice_shouldReturn25() {
            BigDecimal result = bidService.getBidIncrement(new BigDecimal("999999"));

            assertBigDecimalValueEquals("25", result);
        }
    }


    // ═══════════════════════════════════════════════════════
    // NHÓM 2: placeBid() — Validation (không phụ thuộc trạng thái auction)
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("placeBid() — Validation input")
    class PlaceBidValidationTests {

        private User seller;
        private User bidder;
        private User sellerOnly;
        private User poorBidder;
        private Auction runningAuction;

        @BeforeEach
        void setUpAuction() {
            // Seller tạo auction
            seller = userService.createSeller(
                    "Seller", uniqueUsername(), "password123"
            );
            // Bidder có đủ tiền
            bidder = createBidder(new BigDecimal("99999"));

            // Auction đang RUNNING, giá khởi điểm 100
            runningAuction = createRunningAuction(seller, new BigDecimal("100"));
        }

        @AfterEach
        void tearDownAuction() {
            cleanUpAuction(runningAuction);
            cleanUpUser(poorBidder);
            cleanUpUser(sellerOnly);
            cleanUpUser(bidder);
            cleanUpUser(seller);
        }


        // ── Equivalence Partitioning ─────────────────────

        @Test
        @DisplayName("EP: Bidder null → throw NullPointerException")
        void placeBid_nullBidder_shouldThrow() {
            assertThrows(RuntimeException.class, () ->
                    bidService.placeBid(runningAuction.getId(), null, new BigDecimal("100"))
            );
        }

        @Test
        @DisplayName("EP: User không có role BIDDER → throw RuntimeException")
        void placeBid_userNotBidder_shouldThrow() {
            // Tạo user chỉ có role SELLER
            sellerOnly = userService.createSeller(
                    "Seller Only", uniqueUsername(), "password123"
            );

            assertThrows(RuntimeException.class, () ->
                    bidService.placeBid(runningAuction.getId(), sellerOnly, new BigDecimal("100"))
            );
        }

        @Test
        @DisplayName("EP: Amount = 0 → throw InvalidBidException")
        void placeBid_zeroAmount_shouldThrow() {
            assertThrows(RuntimeException.class, () ->
                    bidService.placeBid(runningAuction.getId(), bidder, BigDecimal.ZERO)
            );
        }

        @Test
        @DisplayName("EP: Amount âm → throw InvalidBidException")
        void placeBid_negativeAmount_shouldThrow() {
            assertThrows(RuntimeException.class, () ->
                    bidService.placeBid(runningAuction.getId(), bidder, new BigDecimal("-1"))
            );
        }

        @Test
        @DisplayName("EP: Số dư không đủ → throw RuntimeException")
        void placeBid_insufficientBalance_shouldThrow() {
            // Bidder chỉ có 50, bid 9999
            poorBidder = createBidder(new BigDecimal("50"));

            assertThrows(RuntimeException.class, () ->
                    bidService.placeBid(runningAuction.getId(), poorBidder, new BigDecimal("9999"))
            );
        }

        @Test
        @DisplayName("EP: auctionId không tồn tại → throw RuntimeException")
        void placeBid_invalidAuctionId_shouldThrow() {
            assertThrows(RuntimeException.class, () ->
                    bidService.placeBid(99999, bidder, new BigDecimal("100"))
            );
        }
    }


    // ═══════════════════════════════════════════════════════
    // NHÓM 3: placeBid() — Trạng thái Auction
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("placeBid() — Trạng thái Auction")
    class PlaceBidAuctionStatusTests {

        private User seller;
        private User bidder;
        private Auction auction;

        @BeforeEach
        void setUpUsers() {
            seller = userService.createSeller("Seller", uniqueUsername(), "password123");
            seller = userService.getUserByUsername(seller.getUsername());
            bidder = createBidder(new BigDecimal("99999"));
        }

        @AfterEach
        void tearDownUsers() {
            cleanUpAuction(auction);
            cleanUpUser(bidder);
            cleanUpUser(seller);
        }

        @Test
        @DisplayName("EP: Auction chưa bắt đầu (OPEN) → throw RuntimeException")
        void placeBid_auctionNotStarted_shouldThrow() {
            // Auction bắt đầu 1 giờ nữa
            auction = createOpenAuction(seller, new BigDecimal("100"));

            assertThrows(RuntimeException.class, () ->
                    bidService.placeBid(auction.getId(), bidder, new BigDecimal("1000"))
            );
        }

        @Test
        @DisplayName("EP: Auction đã kết thúc (FINISHED) → throw RuntimeException")
        void placeBid_auctionFinished_shouldThrow() {
            // Auction đã kết thúc từ 1 giờ trước
            auction = createFinishedAuction(seller, new BigDecimal("100"));

            assertThrows(RuntimeException.class, () ->
                    bidService.placeBid(auction.getId(), bidder, new BigDecimal("100"))
            );
        }

        @Test
        @DisplayName("EP: Auction đang RUNNING → bid hợp lệ → thành công")
        void placeBid_auctionRunning_validBid_shouldSucceed() {
            auction = createRunningAuction(seller, new BigDecimal("100"));

            // Bid đúng bằng startingPrice (lần bid đầu tiên)
            assertDoesNotThrow(() ->
                    bidService.placeBid(auction.getId(), bidder, new BigDecimal("100"))
            );
        }
    }


    // ═══════════════════════════════════════════════════════
    // NHÓM 4: placeBid() — Logic giá thầu
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("placeBid() — Logic giá thầu (BVA + EP)")
    class PlaceBidPriceLogicTests {

        private User seller;
        private User bidder1;
        private User bidder2;
        private User exactBidder;
        private User nearBidder;
        private Auction auction; // startingPrice = 100

        @BeforeEach
        void setUpAuction() {
            seller  = userService.createSeller("Seller", uniqueUsername(), "password123");
            seller  = userService.getUserByUsername(seller.getUsername());
            bidder1 = createBidder(new BigDecimal("99999"));
            bidder2 = createBidder(new BigDecimal("99999"));
            auction = createRunningAuction(seller, new BigDecimal("100"));
        }

        @AfterEach
        void tearDownAuction() {
            cleanUpAuction(auction);
            cleanUpUser(nearBidder);
            cleanUpUser(exactBidder);
            cleanUpUser(bidder2);
            cleanUpUser(bidder1);
            cleanUpUser(seller);
        }

        // ── Lần bid đầu tiên (currentPrice == null hoặc == 0) ────

        @Test
        @DisplayName("EP: Bid đầu tiên đúng bằng startingPrice → thành công")
        void firstBid_equalStartingPrice_shouldSucceed() {
            assertDoesNotThrow(() ->
                    bidService.placeBid(auction.getId(), bidder1, new BigDecimal("100"))
            );
        }

        @Test
        @DisplayName("BVA: Bid đầu tiên thấp hơn startingPrice 1 đơn vị → throw")
        void firstBid_oneBelowStartingPrice_shouldThrow() {
            // startingPrice = 100, bid 99 → không hợp lệ
            assertThrows(RuntimeException.class, () ->
                    bidService.placeBid(auction.getId(), bidder1, new BigDecimal("99"))
            );
        }

        @Test
        @DisplayName("EP: Bid đầu tiên cao hơn startingPrice → thành công")
        void firstBid_aboveStartingPrice_shouldSucceed() {
            assertDoesNotThrow(() ->
                    bidService.placeBid(auction.getId(), bidder1, new BigDecimal("200"))
            );
        }

        // ── Lần bid tiếp theo (currentPrice đã có) ───────────────
        // currentPrice = 100 → increment = 1 → minNextBid = 101

        @Test
        @DisplayName("BVA: Bid tiếp theo đúng bằng currentPrice + increment → thành công")
        void nextBid_exactMinimum_shouldSucceed() throws SQLException {
            bidService.placeBid(auction.getId(), bidder1, new BigDecimal("100"));

            // currentPrice = 100, increment = 2.5, minNext = 102.5
            assertDoesNotThrow(() ->
                    bidService.placeBid(auction.getId(), bidder2, new BigDecimal("102.5"))
            );
        }

        @Test
        @DisplayName("BVA: Bid tiếp theo thấp hơn minimum 1 đơn vị → throw")
        void nextBid_oneBelowMinimum_shouldThrow() throws SQLException {
            bidService.placeBid(auction.getId(), bidder1, new BigDecimal("100"));

            // currentPrice = 100, increment = 2.5, minNext = 102.5 → bid 100 không hợp lệ
            assertThrows(RuntimeException.class, () ->
                    bidService.placeBid(auction.getId(), bidder2, new BigDecimal("100"))
            );
        }

        @Test
        @DisplayName("BVA: Bid tiếp theo bằng đúng currentPrice (không có increment) → throw")
        void nextBid_equalCurrentPrice_shouldThrow() throws SQLException {
            bidService.placeBid(auction.getId(), bidder1, new BigDecimal("100"));

            assertThrows(RuntimeException.class, () ->
                    bidService.placeBid(auction.getId(), bidder2, new BigDecimal("100"))
            );
        }

        @Test
        @DisplayName("BVA: Số dư đúng bằng amount (biên trên hợp lệ) → thành công")
        void placeBid_balanceExactlyEqualAmount_shouldSucceed() {
            // Tạo bidder có balance = đúng 100 (bằng startingPrice)
            exactBidder = createBidder(new BigDecimal("100"));

            assertDoesNotThrow(() ->
                    bidService.placeBid(auction.getId(), exactBidder, new BigDecimal("100"))
            );
        }

        @Test
        @DisplayName("BVA: Số dư ít hơn amount 1 đơn vị → throw")
        void placeBid_balanceOneBelowAmount_shouldThrow() {
            // Bidder chỉ có 99, bid 100
            nearBidder = createBidder(new BigDecimal("99"));

            assertThrows(RuntimeException.class, () ->
                    bidService.placeBid(auction.getId(), nearBidder, new BigDecimal("100"))
            );
        }
    }


    // ═══════════════════════════════════════════════════════
    // NHÓM 5: placeBid() — Combinatorial Testing
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("placeBid() — Combinatorial: tổ hợp điều kiện")
    class PlaceBidCombinatorialTests {

        private User seller;
        private User bidder;
        private Auction auction;

        @BeforeEach
        void setUpSeller() {
            seller = userService.createSeller("Seller", uniqueUsername(), "password123");
            seller = userService.getUserByUsername(seller.getUsername());
        }

        @AfterEach
        void tearDownSeller() {
            cleanUpAuction(auction);
            cleanUpUser(bidder);
            cleanUpUser(seller);
        }

        @ParameterizedTest
        @DisplayName("Combinatorial: tổ hợp (balance, amount, startingPrice) → kết quả mong đợi")
        @CsvSource({
                // balance, amount, startingPrice, shouldSucceed
                "99999, 100,   100,  true",   // đủ tiền, đúng giá → thành công
                "99999, 99,    100,  false",  // đủ tiền, dưới giá → thất bại
                "50,    100,   100,  false",  // thiếu tiền, đúng giá → thất bại
                "50,    99,    100,  false",  // thiếu tiền, dưới giá → thất bại
                "99999, 500,   100,  true",   // đủ tiền, cao hơn giá → thành công
        })
        void placeBid_combinations(
                String balance, String amount,
                String startingPrice, boolean shouldSucceed) {

            bidder = createBidder(new BigDecimal(balance));
            auction = createRunningAuction(seller, new BigDecimal(startingPrice));

            if (shouldSucceed) {
                assertDoesNotThrow(() ->
                        bidService.placeBid(auction.getId(), bidder, new BigDecimal(amount))
                );
            } else {
                assertThrows(RuntimeException.class, () ->
                        bidService.placeBid(auction.getId(), bidder, new BigDecimal(amount))
                );
            }
        }
    }


    // ═══════════════════════════════════════════════════════
    // NHÓM 6: getHistoryBid / getLatestBid / getHighestBidder
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("getHistoryBid(), getLatestBid(), getHighestBidder()")
    class QueryTests {

        private User seller;
        private User bidder;
        private Auction auction;

        @BeforeEach
        void setUpAuction() {
            seller  = userService.createSeller("Seller", uniqueUsername(), "password123");
            seller  = userService.getUserByUsername(seller.getUsername());
            bidder  = createBidder(new BigDecimal("99999"));
            auction = createRunningAuction(seller, new BigDecimal("100"));
        }

        @AfterEach
        void tearDownAuction() {
            cleanUpAuction(auction);
            cleanUpUser(bidder);
            cleanUpUser(seller);
        }

        @Test
        @DisplayName("EP: Auction chưa có bid → getLatestBid throw RuntimeException")
        void getLatestBid_noBids_shouldThrow() {
            assertThrows(RuntimeException.class, () ->
                    bidService.getLatestBid(auction.getId())
            );
        }

        @Test
        @DisplayName("EP: Auction chưa có bid → getHighestBidder throw RuntimeException")
        void getHighestBidder_noBids_shouldThrow() {
            assertThrows(RuntimeException.class, () ->
                    bidService.getHighestBidder(auction.getId())
            );
        }

        @Test
        @DisplayName("EP: Auction chưa có bid → getHistoryBid trả về list rỗng")
        void getHistoryBid_noBids_shouldReturnEmptyList() {
            var history = bidService.getHistoryBid(auction.getId());

            assertNotNull(history);
            assertTrue(history.isEmpty());
        }

        @Test
        @DisplayName("EP: Sau khi bid → getHighestBidder trả về đúng bidder")
        void getHighestBidder_afterBid_shouldReturnCorrectBidder() throws SQLException {
            bidService.placeBid(auction.getId(), bidder, new BigDecimal("100"));

            User highest = bidService.getHighestBidder(auction.getId());
            assertEquals(bidder.getId(), highest.getId());
        }

        @Test
        @DisplayName("EP: Sau khi bid → getLatestBid trả về đúng amount")
        void getLatestBid_afterBid_shouldReturnCorrectAmount() throws SQLException {
            bidService.placeBid(auction.getId(), bidder, new BigDecimal("100"));

            var latest = bidService.getLatestBid(auction.getId());
            assertBigDecimalValueEquals("100", latest.getAmount());
        }

        @Test
        @DisplayName("EP: auctionId không hợp lệ (≤ 0) → throw RuntimeException")
        void queryMethods_invalidAuctionId_shouldThrow() {
            assertAll(
                    () -> assertThrows(RuntimeException.class,
                            () -> bidService.getHistoryBid(0)),
                    () -> assertThrows(RuntimeException.class,
                            () -> bidService.getLatestBid(-1)),
                    () -> assertThrows(RuntimeException.class,
                            () -> bidService.getHighestBidder(0))
            );
        }
    }
}

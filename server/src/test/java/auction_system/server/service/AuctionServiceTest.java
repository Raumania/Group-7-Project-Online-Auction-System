package auction_system.server.service;

import auction_system.common.dto.AuctionDTO;
import auction_system.common.enums.AuctionStatus;
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
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AuctionServiceTest {

    // ═══════════════════════════════════════════════════════
    // createAuction()  → validate input + trạng thái status tự động
    // deleteAuction()  → chỉ xóa được khi OPEN & chưa có bid
    // editAuction()    → chỉ sửa được khi OPEN & chưa có bid
    // closeAuction()   → chỉ đóng được khi OPEN hoặc RUNNING
    // cancelAuction()  → chỉ huỷ được khi OPEN hoặc RUNNING
    // getAuctionById() → tồn tại vs không tồn tại
    // getMyAuctions()  → seller thường vs ADMIN
    // ═══════════════════════════════════════════════════════

    private static UserService userService;
    private static AuctionService auctionService;
    private static BidService bidService;

    // ── Helpers ──────────────────────────────────────────

    private static String uniqueUsername() {
        return "user_" + UUID.randomUUID().toString().substring(0, 8);
    }

    /** Tạo seller hợp lệ, trả về object đã có ID từ DB. */
    private static User createSeller() {
        String username = uniqueUsername();
        User seller = userService.createSeller("Test Seller", username, "password123");
        return userService.getUserByUsername(seller.getUsername());
    }

    /** Tạo admin hợp lệ */
    private static User createAdmin() {
        String username = uniqueUsername();
        userService.registerUser("Admin User", username, "password123",
                Set.of(UserRole.ADMIN, UserRole.SELLER));
        return userService.getUserByUsername(username);
    }

    /** Tạo Auction với thời gian tuỳ chỉnh — dùng chung cho mọi helper. */
    private Auction buildAndCreate(User seller, BigDecimal startingPrice,
                                   LocalDateTime start, LocalDateTime end,
                                   String name) {
        AuctionDTO dto = new AuctionDTO(
                name,
                "Mô tả mẫu",
                ItemType.ELECTRONICS,
                seller.getId(),
                startingPrice,
                start,
                end
        );
        Auction auction = new Auction(dto);
        auctionService.createAuction(auction);
        return auctionService.getAuctionById(auction.getId());
    }

    /** Auction OPEN: bắt đầu 3 giờ tới */
    private Auction createOpenAuction(User seller, BigDecimal price) {
        return buildAndCreate(seller, price,
                LocalDateTime.now().plusHours(3),
                LocalDateTime.now().plusHours(10),
                "Auction OPEN - " + UUID.randomUUID());
    }

    /** Auction RUNNING: đang diễn ra */
    private Auction createRunningAuction(User seller, BigDecimal price) {
        return buildAndCreate(seller, price,
                LocalDateTime.now().minusHours(3),
                LocalDateTime.now().plusHours(6),
                "Auction RUNNING - " + UUID.randomUUID());
    }

    /** Auction FINISHED: đã kết thúc */
    private Auction createFinishedAuction(User seller, BigDecimal price) {
        return buildAndCreate(seller, price,
                LocalDateTime.now().minusHours(10),
                LocalDateTime.now().minusHours(1),
                "Auction FINISHED - " + UUID.randomUUID());
    }

    // ── Cleanup helpers (giống pattern của BidServiceTest) ──

    private static void cleanUpAuction(Auction auction) {
        if (auction == null || auction.getId() <= 0) return;

        // Thử cancel trước (nếu đang RUNNING)
        try { auctionService.cancelAuction(auction.getId()); } catch (RuntimeException ignored) {}

        // Thử xóa qua service
        try {
            auctionService.deleteAuction(auction.getId());
            return;
        } catch (RuntimeException ignored) {}

        // Fallback: xóa thẳng DB
        deleteAuctionRows(auction);
    }

    private static void deleteAuctionRows(Auction auction) {
        try (Connection conn = DatabaseConnection.getConnection()) {
            deleteById(conn, "DELETE FROM bid_transactions WHERE auction_id = ?", auction.getId());
            deleteById(conn, "DELETE FROM auctions WHERE id = ?", auction.getId());
            if (auction.getItemId() > 0)
                deleteById(conn, "DELETE FROM items WHERE id = ?", auction.getItemId());
        } catch (SQLException ignored) {}
    }

    private static void deleteById(Connection conn, String sql, int id) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    private static void cleanUpUser(User user) {
        if (user == null || user.getId() <= 0) return;
        try { userService.removeUser(user.getId()); } catch (RuntimeException ignored) {}
    }

    // ── Setup toàn test class ────────────────────────────

    @BeforeAll
    static void setUp() {
        userService    = UserService.getInstance();
        auctionService = AuctionService.getInstance();
        bidService     = BidService.getInstance();
    }


    // ═══════════════════════════════════════════════════════
    // NHÓM 1: createAuction() — Validation dữ liệu đầu vào
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("createAuction() — Validation input (EP + BVA)")
    class CreateAuctionValidationTests {

        private User   seller;
        private Auction createdAuction;

        @BeforeEach
        void setUpSeller() {
            seller = createSeller();
        }

        @AfterEach
        void tearDown() {
            cleanUpAuction(createdAuction);
            cleanUpUser(seller);
        }

        // ── EP: field null / rỗng ─────────────────────────

        @Test
        @DisplayName("EP: Tên item null → throw RuntimeException")
        void create_nullName_shouldThrow() {
            AuctionDTO dto = new AuctionDTO(
                    null, "desc", ItemType.ELECTRONICS,
                    seller.getId(), new BigDecimal("100"),
                    LocalDateTime.now().plusHours(1),
                    LocalDateTime.now().plusHours(5)
            );
            assertThrows(RuntimeException.class,
                    () -> auctionService.createAuction(new Auction(dto)));
        }

        @Test
        @DisplayName("EP: Tên item rỗng → throw RuntimeException")
        void create_emptyName_shouldThrow() {
            AuctionDTO dto = new AuctionDTO(
                    "   ", "desc", ItemType.ELECTRONICS,
                    seller.getId(), new BigDecimal("100"),
                    LocalDateTime.now().plusHours(1),
                    LocalDateTime.now().plusHours(5)
            );
            assertThrows(RuntimeException.class,
                    () -> auctionService.createAuction(new Auction(dto)));
        }

        @Test
        @DisplayName("EP: startTime null → throw RuntimeException")
        void create_nullStartTime_shouldThrow() {
            AuctionDTO dto = new AuctionDTO(
                    "Laptop", "desc", ItemType.ELECTRONICS,
                    seller.getId(), new BigDecimal("100"),
                    null,
                    LocalDateTime.now().plusHours(5)
            );
            assertThrows(RuntimeException.class,
                    () -> auctionService.createAuction(new Auction(dto)));
        }

        @Test
        @DisplayName("EP: endTime null → throw RuntimeException")
        void create_nullEndTime_shouldThrow() {
            AuctionDTO dto = new AuctionDTO(
                    "Laptop", "desc", ItemType.ELECTRONICS,
                    seller.getId(), new BigDecimal("100"),
                    LocalDateTime.now().plusHours(1),
                    null
            );
            assertThrows(RuntimeException.class,
                    () -> auctionService.createAuction(new Auction(dto)));
        }

        @Test
        @DisplayName("EP: sellerId không tồn tại trong DB → throw RuntimeException")
        void create_invalidSellerId_shouldThrow() {
            AuctionDTO dto = new AuctionDTO(
                    "Laptop", "desc", ItemType.ELECTRONICS,
                    999999,                          // ID không tồn tại
                    new BigDecimal("100"),
                    LocalDateTime.now().plusHours(1),
                    LocalDateTime.now().plusHours(5)
            );
            assertThrows(RuntimeException.class,
                    () -> auctionService.createAuction(new Auction(dto)));
        }

        @Test
        @DisplayName("EP: ItemType null → throw RuntimeException")
        void create_nullItemType_shouldThrow() {
            AuctionDTO dto = new AuctionDTO(
                    "Laptop", "desc", null,
                    seller.getId(), new BigDecimal("100"),
                    LocalDateTime.now().plusHours(1),
                    LocalDateTime.now().plusHours(5)
            );
            assertThrows(RuntimeException.class,
                    () -> auctionService.createAuction(new Auction(dto)));
        }

        // ── BVA: giá khởi điểm ───────────────────────────

        @ParameterizedTest
        @DisplayName("BVA: startingPrice ≤ 0 → throw RuntimeException")
        @CsvSource({"0", "-1", "-999"})
        void create_nonPositivePrice_shouldThrow(String price) {
            AuctionDTO dto = new AuctionDTO(
                    "Laptop", "desc", ItemType.ELECTRONICS,
                    seller.getId(), new BigDecimal(price),
                    LocalDateTime.now().plusHours(1),
                    LocalDateTime.now().plusHours(5)
            );
            assertThrows(RuntimeException.class,
                    () -> auctionService.createAuction(new Auction(dto)));
        }

        @Test
        @DisplayName("BVA: startingPrice = 0.01 (biên dưới hợp lệ) → thành công")
        void create_minPositivePrice_shouldSucceed() {
            AuctionDTO dto = new AuctionDTO(
                    "Laptop", "desc", ItemType.ELECTRONICS,
                    seller.getId(), new BigDecimal("0.01"),
                    LocalDateTime.now().plusHours(1),
                    LocalDateTime.now().plusHours(5)
            );
            Auction auction = new Auction(dto);
            assertDoesNotThrow(() -> auctionService.createAuction(auction));
            createdAuction = auctionService.getAuctionById(auction.getId());
        }

        // ── BVA: thời gian ───────────────────────────────

        @Test
        @DisplayName("BVA: endTime == startTime → throw RuntimeException")
        void create_endTimeEqualsStartTime_shouldThrow() {
            LocalDateTime t = LocalDateTime.now().plusHours(2);
            AuctionDTO dto = new AuctionDTO(
                    "Laptop", "desc", ItemType.ELECTRONICS,
                    seller.getId(), new BigDecimal("100"), t, t
            );
            assertThrows(RuntimeException.class,
                    () -> auctionService.createAuction(new Auction(dto)));
        }

        @Test
        @DisplayName("BVA: endTime trước startTime → throw RuntimeException")
        void create_endTimeBeforeStartTime_shouldThrow() {
            AuctionDTO dto = new AuctionDTO(
                    "Laptop", "desc", ItemType.ELECTRONICS,
                    seller.getId(), new BigDecimal("100"),
                    LocalDateTime.now().plusHours(5),
                    LocalDateTime.now().plusHours(1)  // end < start
            );
            assertThrows(RuntimeException.class,
                    () -> auctionService.createAuction(new Auction(dto)));
        }

        // ── EP: dữ liệu hoàn toàn hợp lệ ────────────────

        @Test
        @DisplayName("EP: Tất cả field hợp lệ → tạo thành công, có ID trong DB")
        void create_validData_shouldPersistWithId() {
            AuctionDTO dto = new AuctionDTO(
                    "MacBook Pro", "Laptop mới 100%", ItemType.ELECTRONICS,
                    seller.getId(), new BigDecimal("500"),
                    LocalDateTime.now().plusHours(1),
                    LocalDateTime.now().plusHours(8)
            );
            Auction auction = new Auction(dto);
            assertDoesNotThrow(() -> auctionService.createAuction(auction));

            createdAuction = auctionService.getAuctionById(auction.getId());
            assertNotNull(createdAuction);
            assertTrue(createdAuction.getId() > 0);
        }
    }


    // ═══════════════════════════════════════════════════════
    // NHÓM 2: createAuction() — Trạng thái tự động (Status)
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("createAuction() — Gán AuctionStatus tự động")
    class CreateAuctionStatusTests {

        private User    seller;
        private Auction createdAuction;

        @BeforeEach
        void setUpSeller() {
            seller = createSeller();
        }

        @AfterEach
        void tearDown() {
            cleanUpAuction(createdAuction);
            cleanUpUser(seller);
        }

        @Test
        @DisplayName("EP: Thời gian chưa bắt đầu → status = OPEN")
        void create_futureStartTime_statusShouldBeOpen() {
            createdAuction = createOpenAuction(seller, new BigDecimal("100"));
            assertEquals(AuctionStatus.OPEN, createdAuction.getStatus());
        }

        @Test
        @DisplayName("EP: Đang trong khoảng start–end → status = RUNNING")
        void create_currentTimeInRange_statusShouldBeRunning() {
            createdAuction = createRunningAuction(seller, new BigDecimal("100"));
            assertEquals(AuctionStatus.RUNNING, createdAuction.getStatus());
        }

        @Test
        @DisplayName("EP: endTime đã qua → status = FINISHED")
        void create_pastEndTime_statusShouldBeFinished() {
            createdAuction = createFinishedAuction(seller, new BigDecimal("100"));
            assertEquals(AuctionStatus.FINISHED, createdAuction.getStatus());
        }
    }


    // ═══════════════════════════════════════════════════════
    // NHÓM 3: deleteAuction() — EP + BVA
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("deleteAuction() — EP theo Status")
    class DeleteAuctionTests {

        private User    seller;
        private User    bidder;
        private Auction auction;

        @BeforeEach
        void setUpUsers() {
            seller = createSeller();
            // Bidder có đủ tiền để tạo bid trong test
            String username = uniqueUsername();
            userService.registerUser("Bidder", username, "password123",
                    Set.of(UserRole.BIDDER));
            User u = userService.getUserByUsername(username);
            userService.deposit(u.getId(), new BigDecimal("99999"));
            bidder = userService.getUserByUsername(username);
        }

        @AfterEach
        void tearDown() {
            cleanUpAuction(auction);
            cleanUpUser(bidder);
            cleanUpUser(seller);
        }

        @Test
        @DisplayName("EP: Auction không tồn tại → throw RuntimeException")
        void delete_notFound_shouldThrow() {
            assertThrows(RuntimeException.class,
                    () -> auctionService.deleteAuction(999999));
        }

        @Test
        @DisplayName("EP: Auction đang RUNNING → throw RuntimeException")
        void delete_runningAuction_shouldThrow() {
            auction = createRunningAuction(seller, new BigDecimal("100"));
            assertThrows(RuntimeException.class,
                    () -> auctionService.deleteAuction(auction.getId()));
        }

        @Test
        @DisplayName("EP: Auction đã FINISHED → throw RuntimeException")
        void delete_finishedAuction_shouldThrow() {
            auction = createFinishedAuction(seller, new BigDecimal("100"));
            assertThrows(RuntimeException.class,
                    () -> auctionService.deleteAuction(auction.getId()));
        }

        @Test
        @DisplayName("EP: Auction OPEN, chưa có bid → xóa thành công")
        void delete_openAuctionNoBid_shouldSucceed() {
            auction = createOpenAuction(seller, new BigDecimal("100"));
            int id = auction.getId();

            assertDoesNotThrow(() -> auctionService.deleteAuction(id));

            // Sau khi xóa, getAuctionById phải trả về null hoặc throw
            auction = null; // tránh cleanUpAuction xóa lần 2
            assertNull(auctionService.getAuctionById(id));
        }
    }


    // ═══════════════════════════════════════════════════════
    // NHÓM 4: editAuction() — EP theo Status + bid
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("editAuction() — EP + BVA")
    class EditAuctionTests {

        private User    seller;
        private User    bidder;
        private Auction auction;

        @BeforeEach
        void setUpUsers() {
            seller = createSeller();
            String username = uniqueUsername();
            userService.registerUser("Bidder", username, "password123",
                    Set.of(UserRole.BIDDER));
            User u = userService.getUserByUsername(username);
            userService.deposit(u.getId(), new BigDecimal("99999"));
            bidder = userService.getUserByUsername(username);
        }

        @AfterEach
        void tearDown() {
            cleanUpAuction(auction);
            cleanUpUser(bidder);
            cleanUpUser(seller);
        }

        @Test
        @DisplayName("EP: Auction không tồn tại → throw RuntimeException")
        void edit_notFound_shouldThrow() {
            AuctionDTO dto = new AuctionDTO(
                    "New Name", "desc", ItemType.ELECTRONICS,
                    seller.getId(), new BigDecimal("200"),
                    LocalDateTime.now().plusHours(1),
                    LocalDateTime.now().plusHours(5)
            );
            Auction fake = new Auction(dto);
            fake.setId(999999); // ID không tồn tại
            assertThrows(RuntimeException.class,
                    () -> auctionService.editAuction(fake));
        }

        @Test
        @DisplayName("EP: Auction đang RUNNING → throw RuntimeException")
        void edit_runningAuction_shouldThrow() {
            auction = createRunningAuction(seller, new BigDecimal("100"));
            auction.setName("Tên mới");

            assertThrows(RuntimeException.class,
                    () -> auctionService.editAuction(auction));
        }

        @Test
        @DisplayName("EP: Auction đã FINISHED → throw RuntimeException")
        void edit_finishedAuction_shouldThrow() {
            auction = createFinishedAuction(seller, new BigDecimal("100"));
            auction.setName("Tên mới");

            assertThrows(RuntimeException.class,
                    () -> auctionService.editAuction(auction));
        }

        @Test
        @DisplayName("EP: Auction OPEN, đã có bid → throw RuntimeException")
        void edit_openAuctionWithBid_shouldThrow() throws SQLException {
            // Dùng RUNNING auction để đặt bid, sau đó thay status thủ công
            auction = createRunningAuction(seller, new BigDecimal("100"));
            bidService.placeBid(auction.getId(), bidder, new BigDecimal("100"));

            // Lấy lại với currentPrice > 0
            Auction auctionWithBid = auctionService.getAuctionById(auction.getId());
            auctionWithBid.setStatus(AuctionStatus.OPEN); // giả lập OPEN nhưng đã có bid

            assertThrows(RuntimeException.class,
                    () -> auctionService.editAuction(auctionWithBid));
        }

        @Test
        @DisplayName("EP: Auction OPEN, chưa có bid → sửa thành công")
        void edit_openAuctionNoBid_shouldSucceed() {
            auction = createOpenAuction(seller, new BigDecimal("100"));
            auction.setName("Tên đã cập nhật");

            assertDoesNotThrow(() -> auctionService.editAuction(auction));

            Auction updated = auctionService.getAuctionById(auction.getId());
            assertEquals("Tên đã cập nhật", updated.getName());
        }
    }


    // ═══════════════════════════════════════════════════════
    // NHÓM 5: closeAuction() và cancelAuction()
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("closeAuction() và cancelAuction() — EP theo Status")
    class CloseAndCancelAuctionTests {

        private User    seller;
        private Auction auction;

        @BeforeEach
        void setUpSeller() {
            seller = createSeller();
        }

        @AfterEach
        void tearDown() {
            cleanUpAuction(auction);
            cleanUpUser(seller);
        }

        // ── closeAuction ──────────────────────────────────

        @Test
        @DisplayName("EP: closeAuction — không tồn tại → throw")
        void close_notFound_shouldThrow() {
            assertThrows(RuntimeException.class,
                    () -> auctionService.closeAuction(999999));
        }

        @Test
        @DisplayName("EP: closeAuction — đang RUNNING → thành công, status = FINISHED")
        void close_runningAuction_shouldBecomeFinished() {
            auction = createRunningAuction(seller, new BigDecimal("100"));

            assertDoesNotThrow(() -> auctionService.closeAuction(auction.getId()));

            Auction closed = auctionService.getAuctionById(auction.getId());
            assertEquals(AuctionStatus.FINISHED, closed.getStatus());
        }

        @Test
        @DisplayName("EP: closeAuction — đang OPEN → thành công, status = FINISHED")
        void close_openAuction_shouldBecomeFinished() {
            auction = createOpenAuction(seller, new BigDecimal("100"));

            assertDoesNotThrow(() -> auctionService.closeAuction(auction.getId()));

            Auction closed = auctionService.getAuctionById(auction.getId());
            assertEquals(AuctionStatus.FINISHED, closed.getStatus());
        }

        @Test
        @DisplayName("EP: closeAuction — đã FINISHED → throw")
        void close_finishedAuction_shouldThrow() {
            auction = createFinishedAuction(seller, new BigDecimal("100"));
            assertThrows(RuntimeException.class,
                    () -> auctionService.closeAuction(auction.getId()));
        }

        // ── cancelAuction ─────────────────────────────────

        @Test
        @DisplayName("EP: cancelAuction — không tồn tại → throw")
        void cancel_notFound_shouldThrow() {
            assertThrows(RuntimeException.class,
                    () -> auctionService.cancelAuction(999999));
        }

        @Test
        @DisplayName("EP: cancelAuction — đang OPEN → thành công, status = CANCELLED")
        void cancel_openAuction_shouldBecancelled() {
            auction = createOpenAuction(seller, new BigDecimal("100"));

            assertDoesNotThrow(() -> auctionService.cancelAuction(auction.getId()));

            Auction cancelled = auctionService.getAuctionById(auction.getId());
            assertEquals(AuctionStatus.CANCELLED, cancelled.getStatus());
        }

        @Test
        @DisplayName("EP: cancelAuction — đang RUNNING → thành công, status = CANCELLED")
        void cancel_runningAuction_shouldBecancelled() {
            auction = createRunningAuction(seller, new BigDecimal("100"));

            assertDoesNotThrow(() -> auctionService.cancelAuction(auction.getId()));

            Auction cancelled = auctionService.getAuctionById(auction.getId());
            assertEquals(AuctionStatus.CANCELLED, cancelled.getStatus());
        }

        @Test
        @DisplayName("EP: cancelAuction — đã FINISHED → throw")
        void cancel_finishedAuction_shouldThrow() {
            auction = createFinishedAuction(seller, new BigDecimal("100"));
            assertThrows(RuntimeException.class,
                    () -> auctionService.cancelAuction(auction.getId()));
        }
    }


    // ═══════════════════════════════════════════════════════
    // NHÓM 6: getAuctionById() và getAllAuctions()
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("getAuctionById() và getAllAuctions()")
    class QueryAuctionTests {

        private User    seller;
        private Auction auction;

        @BeforeEach
        void setUpSeller() {
            seller = createSeller();
        }

        @AfterEach
        void tearDown() {
            cleanUpAuction(auction);
            cleanUpUser(seller);
        }

        @Test
        @DisplayName("EP: ID hợp lệ → trả về đúng auction")
        void getById_validId_shouldReturnAuction() {
            auction = createOpenAuction(seller, new BigDecimal("100"));

            Auction found = auctionService.getAuctionById(auction.getId());
            assertNotNull(found);
            assertEquals(auction.getId(), found.getId());
        }

        @Test
        @DisplayName("EP: ID không tồn tại → trả về null")
        void getById_invalidId_shouldReturnNull() {
            Auction found = auctionService.getAuctionById(999999);
            assertNull(found);
        }

        @Test
        @DisplayName("EP: getAllAuctions() → trả về list không null")
        void getAll_shouldReturnNonNullList() {
            List<Auction> all = auctionService.getAllAuctions();
            assertNotNull(all);
        }

        @Test
        @DisplayName("EP: Sau khi tạo auction → getAllAuctions() chứa auction đó")
        void getAll_afterCreate_shouldContainAuction() {
            auction = createOpenAuction(seller, new BigDecimal("100"));
            int id = auction.getId();

            List<Auction> all = auctionService.getAllAuctions();
            assertTrue(all.stream().anyMatch(a -> a.getId() == id));
        }
    }


    // ═══════════════════════════════════════════════════════
    // NHÓM 7: getMyAuctions() — Seller thường vs Admin
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("getMyAuctions() — Phân quyền Seller vs Admin")
    class GetMyAuctionsTests {

        private User    seller1;
        private User    seller2;
        private User    adminUser;
        private Auction auction1;
        private Auction auction2;

        @BeforeEach
        void setUpUsersAndAuctions() {
            seller1   = createSeller();
            seller2   = createSeller();
            adminUser = createAdmin();

            auction1 = createOpenAuction(seller1, new BigDecimal("100"));
            auction2 = createOpenAuction(seller2, new BigDecimal("200"));
        }

        @AfterEach
        void tearDown() {
            cleanUpAuction(auction1);
            cleanUpAuction(auction2);
            cleanUpUser(seller1);
            cleanUpUser(seller2);
            cleanUpUser(adminUser);
        }

        @Test
        @DisplayName("EP: Seller thường → chỉ thấy auction của chính mình")
        void getMyAuctions_seller_shouldOnlySeeOwn() {
            List<Auction> myAuctions = auctionService.getMyAuctions(seller1.getId());

            assertTrue(myAuctions.stream()
                            .allMatch(a -> a.getSellerId() == seller1.getId()),
                    "Seller không được thấy auction của người khác");
        }

        @Test
        @DisplayName("EP: Admin → thấy tất cả auction của mọi seller")
        void getMyAuctions_admin_shouldSeeAll() {
            List<Auction> all = auctionService.getMyAuctions(adminUser.getId());

            int id1 = auction1.getId();
            int id2 = auction2.getId();

            assertTrue(all.stream().anyMatch(a -> a.getId() == id1),
                    "Admin phải thấy auction của seller1");
            assertTrue(all.stream().anyMatch(a -> a.getId() == id2),
                    "Admin phải thấy auction của seller2");
        }
    }
}
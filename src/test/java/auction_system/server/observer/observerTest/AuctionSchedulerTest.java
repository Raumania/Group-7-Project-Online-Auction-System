package auction_system.server.observer.observerTest;

import auction_system.common.dto.AuctionDTO;
import auction_system.common.enums.AuctionStatus;
import auction_system.server.dao.AuctionDAO;
import auction_system.server.model.Auction;
import auction_system.server.observer.AuctionScheduler;
import auction_system.server.service.AuctionService;
import auction_system.server.service.BidService;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class AuctionSchedulerTest {
    static AuctionService auctionService = AuctionService.getInstance();
    private static List<AuctionDTO> auctionList = AuctionSample.createSampleAuctions();
    static Logger logger = LoggerFactory.getLogger(AuctionSchedulerTest.class);
    private AuctionScheduler scheduler;
    private static AuctionScheduler instance;
    static BidService bidService =BidService.getInstance();
    AuctionDAO auctionRepository = AuctionDAO.getInstance();

    private final Map<Integer, Auction> auctions = new ConcurrentHashMap<>();

    @Test
    void createAu () {
        assertDoesNotThrow(() -> {
        Auction auction = new Auction(auctionList.getFirst());
        auctionService.createAuction(auction);
        });
    }

//    @BeforeAll
//    static void beforeAll() {
//        List<AuctionDTO> sample = AuctionSample.createSampleAuctions();
//        for (AuctionDTO auction : sample) {
//            auctionService.createAuction(new Auction(auction));
//        }
//    }

//    @BeforeEach
//    void setUp() {
//        scheduler = AuctionScheduler.getInstance();
//    }
//
//    @AfterEach
//    void tearDown() {
//        scheduler.shutdown();
//    }
//
//    // ─────────────────────────────────────────
//    // Test 1: Scheduler khởi động không crash
//    // ─────────────────────────────────────────
//    @Test
//    void start_shouldNotThrowException() {
//        assertDoesNotThrow(() -> {
//            scheduler.start();
//            TimeUnit.SECONDS.sleep(1);
//        });
//    }
//
//    // ─────────────────────────────────────────
//    // Test 2: Singleton chỉ tạo 1 instance
//    // ─────────────────────────────────────────
//    @Test
//    void getInstance_shouldReturnSameInstance() {
//        AuctionScheduler a = AuctionScheduler.getInstance();
//        AuctionScheduler b = AuctionScheduler.getInstance();
//        assertSame(a, b);
//    }
//
//    // ─────────────────────────────────────────
//    // Test 3: Auction SCHEDULED → RUNNING đúng thời gian
//    // ─────────────────────────────────────────
//    @Test
//    void updateAuctions_shouldTransitionScheduledToRunning()
//            throws InterruptedException {
//
//        // Tạo auction bắt đầu ngay bây giờ
//        Auction macAir = new Auction(auctionList.get(1));
//        auctionService.createAuction(macAir);
//
//        assertEquals(AuctionStatus.OPEN, macAir.getStatus());
//
//        scheduler.start();
//        TimeUnit.SECONDS.sleep(2); // chờ scheduler chạy
//
//        assertEquals(AuctionStatus.RUNNING, macAir.getStatus());
//    }
//
//    // ─────────────────────────────────────────
//    // Test 4: Auction RUNNING → ENDED đúng thời gian
//    // ─────────────────────────────────────────
//    @Test
//    void updateAuctions_shouldTransitionRunningToEnded()
//            throws InterruptedException {
//
//        // Tạo auction đã hết giờ
//        Auction auction = new Auction(auctionList.get(3));
//        auctionService.createAuction(auction);
//        TimeUnit.SECONDS.sleep(2);
//        System.err.println(auction.getName());
//
//        scheduler.start();
//        TimeUnit.SECONDS.sleep(2);
//
//        assertEquals(AuctionStatus.FINISHED, auction.getStatus());
//    }
//
//    // ─────────────────────────────────────────
//    // Test 5: canRemove đúng logic
//    // ─────────────────────────────────────────
//    @Test
//    void canRemove_shouldOnlyKeepOpenAndRunning() {
//        // Tạo auction với các trạng thái khác nhau
//        Auction open     = makeAuctionWithStatus(AuctionStatus.OPEN);
//        Auction running  = makeAuctionWithStatus(AuctionStatus.RUNNING);
//        Auction finished    = makeAuctionWithStatus(AuctionStatus.FINISHED);
//        Auction canceled = makeAuctionWithStatus(AuctionStatus.CANCELLED);
//
//        // OPEN và RUNNING phải được giữ lại
//        assertFalse(scheduler.canRemove(open));
//        assertFalse(scheduler.canRemove(running));
//
//        // ENDED và CANCELED phải bị xóa
//        assertTrue(scheduler.canRemove(finished));
//        assertTrue(scheduler.canRemove(canceled));
//    }
//
//    // ─────────────────────────────────────────
//    // Helper
//    // ─────────────────────────────────────────
//    private Auction makeAuctionWithStatus(AuctionStatus status) {
//        Auction auction = new Auction(auctionList.getFirst());
//        auction.setStatus(status);
//        return auction;
//    }
}
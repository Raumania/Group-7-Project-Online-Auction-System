package auction_system.server.engine;

import auction_system.common.enums.AuctionStatus;
import auction_system.common.enums.UserRole;
import auction_system.server.dao.*;
import auction_system.server.model.Auction;
import auction_system.server.model.AutoBid;
import auction_system.server.model.BidTransaction;
import auction_system.server.model.User;
import auction_system.server.observer.AuctionObserver;
import auction_system.server.observer.BidEvent;
import auction_system.server.observer.EventBus;
import auction_system.server.service.BidService;
import auction_system.server.service.UserService;
import auction_system.server.store.AuctionStore;
import auction_system.server.store.AutoBidStore;
import auction_system.server.store.BidTransactionStore;
import auction_system.server.store.UserStore;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class BidEngine implements AuctionObserver {
    private static BidEngine instance;

    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final AuctionDAO auctionDAO;
    private final AutoBidDAO autoBidDAO;
    private final BidTransactionDAO bidTransactionDAO;
    private final UserService userService;

    private BidEngine() {
        this.auctionDAO = AuctionDAO.getInstance();
        this.autoBidDAO = AutoBidDAO.getInstance();
        this.bidTransactionDAO = BidTransactionDAO.getInstance();
        this.userService = UserService.getInstance();

        // Register itself as a permanent observer after all dependencies are initialized
        EventBus.registerObserver(this);
    }

    public static synchronized BidEngine getInstance() {
        if (instance == null) {
            instance = new BidEngine();
        }
        return instance;
    }

    @Override
    public void onBidPlaced(BidEvent event) {
        System.out.println("[BidEngine] Received new bid event on auction: " + event.auctionId() +
                ", bidder: " + event.newBidderId() + ", amount: " + event.newPrice());
        triggerAutoBids(event.auctionId(), event.newBidderId());
    }

    @Override
    public void update(Auction auction, String message) {
    }

    @Override
    public void onAuctionCreated(Auction auction) {
    }

    @Override
    public void onAuctionEdited(Auction auction) {
    }

    @Override
    public void onAuctionDeleted(int auctionId) {
    }

    public void triggerAutoBids(int auctionId, int currentHighestBidderId) {
        System.out.println("[BidEngine] triggerAutoBids called for auction: " + auctionId + ", leader: " + currentHighestBidderId);
        executorService.submit(() -> {
            try {
                System.out.println("[BidEngine] Thread started for auction: " + auctionId);
                processAutoBids(auctionId, currentHighestBidderId);
                System.out.println("[BidEngine] Thread finished for auction: " + auctionId);
            } catch (Exception e) {
                System.err.println("[BidEngine] Critical error during auto bid processing for auction: " + auctionId);
                e.printStackTrace();
            }
        });
    }

    private void processAutoBids(int auctionId, int currentHighestBidderId) throws SQLException {
        Connection connection = null;
        try {
            connection = DatabaseConnection.getConnection();
            connection.setAutoCommit(false);

            // 1. Lấy thông tin phiên đấu giá, khóa dòng (Pessimistic Locking) để tránh race condition
            Auction auction = auctionDAO.findByIdForUpdate(connection, auctionId);
            if (auction == null || auction.getStatus() != AuctionStatus.RUNNING) {
                connection.commit();
                return;
            }

            // Lấy leader THỰC TẾ từ DB (SELECT FOR UPDATE đảm bảo đây là trạng thái mới nhất).
            // currentHighestBidderId (tham số) có thể stale do async — thread này có thể được trigger
            // bởi một event cũ khi C là leader, nhưng lúc chạy B đã thắng rồi.
            int actualLeaderId = (auction.getHighestBidderId() != null) ? auction.getHighestBidderId() : 0;

            // 2. Lấy danh sách AutoBid đang kích hoạt
            List<AutoBid> activeBids = AutoBidStore.getInstance().getActiveAutoBidsByAuctionId(auctionId);
            if (activeBids.isEmpty()) {
                connection.commit();
                return;
            }

            // Giá hiện tại. Nếu chưa có ai bid thì dùng startingPrice để tính bước kế tiếp
            BigDecimal currentPrice = (auction.getCurrentPrice() != null)
                    ? auction.getCurrentPrice()
                    : auction.getStartingPrice();

            // Bước giá tối thiểu của sàn tính từ giá hiện tại
            BigDecimal minIncrement = BidService.getInstance().getBidIncrement(currentPrice);

            // Mức giá kế tiếp tối thiểu cần đạt để trở thành highest bidder
            // Nếu chưa có ai bid: nextPrice = startingPrice (không cộng thêm increment)
            // Nếu đã có ai bid:   nextPrice = currentPrice + minIncrement
            BigDecimal nextMinPrice = (auction.getHighestBidderId() == null)
                    ? currentPrice
                    : currentPrice.add(minIncrement);

            // 3. Lọc AutoBids hợp lệ: người dùng hợp lệ VÀ maxBid >= nextMinPrice
            //    (đủ điều kiện để đặt ít nhất giá kế tiếp tối thiểu)
            List<AutoBid> validBids = new ArrayList<>();
            for (AutoBid ab : activeBids) {
                User bidder = userService.getUserById(ab.getUserId());
                boolean userInvalid = (bidder == null || !bidder.hasRole(UserRole.BIDDER));
                boolean cannotAffordNextBid = (ab.getMaxBid().compareTo(nextMinPrice) < 0);

                if (userInvalid || cannotAffordNextBid) {
                    // AutoBid không đủ điều kiện → hủy kích hoạt
                    deactivateAutoBid(connection, ab, actualLeaderId, currentPrice);
                } else {
                    validBids.add(ab);
                }
            }

            if (validBids.isEmpty()) {
                connection.commit();
                return;
            }

            // 4. Sắp xếp: MaxBid cao nhất thắng. Tie-break: đặt trước (createdAt) → ID nhỏ hơn
            validBids.sort((a, b) -> {
                int cmp = b.getMaxBid().compareTo(a.getMaxBid());
                if (cmp != 0) return cmp;
                LocalDateTime tA = a.getCreatedAt(), tB = b.getCreatedAt();
                if (tA != null && tB != null) {
                    int timeCmp = tA.compareTo(tB);
                    if (timeCmp != 0) return timeCmp;
                } else if (tA != null) return -1;
                else if (tB != null) return 1;
                return Integer.compare(a.getId(), b.getId());
            });

            AutoBid winnerAb = validBids.getFirst();
            BigDecimal winnerMax = winnerAb.getMaxBid();

            // 5. Dùng actualLeaderId (từ DB tươi) thay vì currentHighestBidderId (stale từ event)
            //    để tránh race condition khi nhiều thread chạy đồng thời.
            //    Nếu winner đã là leader thực tế VÀ không có cạnh tranh → không cần làm gì.
            //    Nếu có cạnh tranh (size > 1): vẫn phải cleanup dù winner không đổi.
            if (winnerAb.getUserId() == actualLeaderId && validBids.size() == 1) {
                connection.commit();
                return;
            }

            // 6. Tính giá đặt cuối cùng (finalPrice) theo quy tắc Proxy Bidding
            BigDecimal finalPrice;
            boolean winnerDeactivated = false;

            if (validBids.size() == 1) {
                // --- Tình huống A: Chỉ 1 AutoBid cạnh tranh ---
                // Điều kiện đã được lọc ở bước 3: winnerMax >= nextMinPrice
                // → Đặt giá đúng nextMinPrice (giá thấp nhất có thể để thắng)

                BigDecimal userIncrement = winnerAb.getBidIncrement();
                BigDecimal effectiveIncrement = (userIncrement != null && userIncrement.compareTo(minIncrement) > 0)
                        ? userIncrement : minIncrement;
                BigDecimal targetPrice = (auction.getHighestBidderId() == null)
                        ? currentPrice
                        : currentPrice.add(effectiveIncrement);

                if (targetPrice.compareTo(winnerMax) <= 0) {
                    // Đủ tiền theo bước giá ưa thích của user
                    finalPrice = targetPrice;
                } else {
                    // Bước nhảy ưa thích (inc) vượt maxBid → dùng platform min một lần cuối
                    // User coi như "hết đạn" (chiến lược inc của họ không dùng được nữa) → deactivate
                    finalPrice = nextMinPrice;
                    winnerDeactivated = true;
                }

            } else {
                // --- Tình huống B: Nhiều AutoBids cạnh tranh ---
                // Hủy tất cả AutoBids từ người về nhì trở đi (họ thua người dẫn đầu)
                for (int i = 1; i < validBids.size(); i++) {
                    deactivateAutoBid(connection, validBids.get(i), actualLeaderId, currentPrice);
                }

                AutoBid runnerUpAb = validBids.get(1);
                BigDecimal runnerUpMax = runnerUpAb.getMaxBid();

                // Bước giá tính từ mức giá của người về nhì
                BigDecimal runnerUpIncrement = BidService.getInstance().getBidIncrement(runnerUpMax);
                BigDecimal userIncrement = winnerAb.getBidIncrement();
                BigDecimal effectiveIncrement = (userIncrement != null && userIncrement.compareTo(runnerUpIncrement) > 0)
                        ? userIncrement : runnerUpIncrement;

                // Quy tắc Proxy: finalPrice = min(runnerUpMax + increment, winnerMax)
                //   → Người thắng chỉ trả vừa đủ để vượt qua người về nhì, không phải toàn bộ MaxBid
                BigDecimal targetWithEffective = runnerUpMax.add(effectiveIncrement);
                BigDecimal targetWithMin = runnerUpMax.add(runnerUpIncrement);

                if (targetWithEffective.compareTo(winnerMax) <= 0) {
                    finalPrice = targetWithEffective;
                } else if (targetWithMin.compareTo(winnerMax) <= 0) {
                    finalPrice = targetWithMin;
                    winnerDeactivated = true; // Đã dùng hết ngân sách
                } else {
                    // runnerUpMax >= winnerMax → người thắng all-in bằng MaxBid của mình
                    finalPrice = winnerMax;
                    winnerDeactivated = true;
                }
            }

            // 7. Thực thi bid: ghi DB, cập nhật Store, giải phóng balance người bid cũ
            executeBid(connection, auction, winnerAb.getUserId(), finalPrice);

            if (winnerDeactivated) {
                deactivateAutoBid(connection, winnerAb, winnerAb.getUserId(), finalPrice);
            }

            connection.commit();

            // 8. Publish BidEvent để thông báo tới toàn bộ Client qua Socket
            EventBus.getInstance().publish(new BidEvent(
                    auctionId, winnerAb.getUserId(), actualLeaderId,
                    finalPrice, currentPrice, LocalDateTime.now()
            ));

        } catch (Exception e) {
            if (connection != null) {
                try { connection.rollback(); } catch (SQLException sqle) { sqle.printStackTrace(); }
            }
            e.printStackTrace();
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(true);
                    connection.close();
                } catch (SQLException sqle) { sqle.printStackTrace(); }
            }
        }
    }

    private void deactivateAutoBid(Connection conn, AutoBid ab, int currentLeaderId, BigDecimal currentPrice) throws SQLException {
        autoBidDAO.deactivate(conn, ab.getId());
        AutoBidStore.getInstance().deactivateAutoBid(ab.getId());

        User user = userService.getUserById(ab.getUserId());
        if (user != null) {
            BigDecimal amountToUnfreeze;
            if (currentLeaderId == ab.getUserId()) {
                amountToUnfreeze = ab.getMaxBid().subtract(currentPrice);
            } else {
                amountToUnfreeze = ab.getMaxBid();
            }

            if (amountToUnfreeze.compareTo(BigDecimal.ZERO) > 0) {
                user.unfreezeBalance(amountToUnfreeze);
                UserDAO.getInstance().updateBalance(conn, user.getId(), user.getAvailableBalance(), user.getFrozenBalance());
            }
        }
    }

    private void executeBid(Connection conn, Auction auction, int newBidderId, BigDecimal finalPrice) throws SQLException {
        int previousBidderId = auction.getHighestBidderId() != null ? auction.getHighestBidderId() : 0;
        BigDecimal previousPrice = auction.getCurrentPrice() != null ? auction.getCurrentPrice() : BigDecimal.ZERO;
        
        LocalDateTime endTime = auction.getEndTime();
        LocalDateTime now = LocalDateTime.now();
        if (endTime != null && !now.isAfter(endTime)) {
            long remainingSeconds = Duration.between(now, endTime).getSeconds();
            if (remainingSeconds <= 30 && remainingSeconds >= 0) {
                auction.setEndTime(endTime.plusMinutes(1));
            }
        }

        User newBidder = userService.getUserById(newBidderId);
        
        if (previousBidderId != 0 && previousBidderId != newBidderId) {
            boolean prevHasActiveAutoBid = AutoBidStore.getInstance().getActiveAutoBidsByAuctionId(auction.getId())
                .stream().anyMatch(ab -> ab.getUserId() == previousBidderId);
                
            if (!prevHasActiveAutoBid) {
                User prevBidder = UserStore.getInstance().getUserById(previousBidderId);
                if (prevBidder != null) {
                    prevBidder.unfreezeBalance(previousPrice);
                    UserDAO.getInstance().updateBalance(conn, prevBidder.getId(), prevBidder.getAvailableBalance(), prevBidder.getFrozenBalance());
                }
            }
        }

        BidTransaction transaction = new BidTransaction(newBidder, finalPrice);
        bidTransactionDAO.save(conn, auction.getId(), transaction);

        auction.setCurrentPrice(finalPrice);
        auction.setHighestBidderId(newBidder.getId());
        auction.setHighestBidderUsername(newBidder.getUsername());
        auctionDAO.update(conn, auction);

        AuctionStore.getInstance().updateAuction(auction);
        BidTransactionStore.getInstance().addBid(auction.getId(), transaction);
    }
}

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
import java.sql.SQLException;
import java.time.LocalDateTime;
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

            System.out.println("[BidEngine] Checking if auction " + auctionId + " exists...");
            try (PreparedStatement ps = connection.prepareStatement("SELECT id, item_id, status FROM auctions WHERE id = ?")) {
                ps.setInt(1, auctionId);
                try (java.sql.ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        System.out.println("[BidEngine] Auction exists directly in DB! item_id: " + rs.getInt("item_id") + ", status: " + rs.getString("status"));
                    } else {
                        System.out.println("[BidEngine] Auction DOES NOT EXIST in DB directly!");
                    }
                }
            }

            Auction auction = auctionDAO.findByIdForUpdate(connection, auctionId);
            if (auction == null || auction.getStatus() != AuctionStatus.RUNNING) {
                System.out.println("[BidEngine] processAutoBids aborted. auction=" + (auction != null ? auction.getId() : "null") + ", status=" + (auction != null ? auction.getStatus() : "null"));
                connection.commit();
                return;
            }

            List<AutoBid> activeBids = AutoBidStore.getInstance().getActiveAutoBidsByAuctionId(auctionId);
            if (activeBids.isEmpty()) {
                System.out.println("[BidEngine] processAutoBids aborted. No active bids.");
                connection.commit();
                return;
            }

            int leaderId = auction.getHighestBidderId() != null ? auction.getHighestBidderId() : 0;
            BigDecimal currentPrice = auction.getCurrentPrice() != null ? auction.getCurrentPrice() : BigDecimal.ZERO;
            System.out.println("[BidEngine] leaderId=" + leaderId + ", currentPrice=" + currentPrice);

            List<AutoBid> validBids = new java.util.ArrayList<>();
            for (AutoBid ab : activeBids) {
                User bidder = userService.getUserById(ab.getUserId());
                if (bidder == null || !bidder.hasRole(UserRole.BIDDER)) {
                    System.out.println("[BidEngine] Deactivating auto bid for user: " + ab.getUserId() + " due to missing or invalid bidder.");
                    deactivateAutoBid(connection, ab, leaderId, currentPrice);
                    continue;
                }
                
                if (ab.getUserId() != leaderId && ab.getMaxBid().compareTo(currentPrice) < 0) {
                    System.out.println("[BidEngine] Deactivating auto bid for user: " + ab.getUserId() + " because max bid " + ab.getMaxBid() + " < currentPrice " + currentPrice);
                    deactivateAutoBid(connection, ab, leaderId, currentPrice);
                    continue;
                }
                
                if (ab.getUserId() == leaderId && ab.getMaxBid().compareTo(currentPrice) < 0) {
                    System.out.println("[BidEngine] Deactivating auto bid for leader: " + ab.getUserId() + " because max bid " + ab.getMaxBid() + " < currentPrice " + currentPrice);
                    deactivateAutoBid(connection, ab, leaderId, currentPrice);
                    continue;
                }
                
                System.out.println("[BidEngine] Valid auto bid found for user: " + ab.getUserId() + " with max bid " + ab.getMaxBid());
                validBids.add(ab);
            }

            if (validBids.isEmpty()) {
                System.out.println("[BidEngine] No valid auto bids remain.");
                connection.commit();
                return;
            }

            validBids.sort((a, b) -> {
                int cmp = b.getMaxBid().compareTo(a.getMaxBid());
                if (cmp != 0) return cmp;
                return a.getCreatedAt().compareTo(b.getCreatedAt());
            });

            AutoBid winnerAb = validBids.get(0);

            if (validBids.size() == 1) {
                if (winnerAb.getUserId() == leaderId) {
                    connection.commit();
                    return;
                } else {
                    if (winnerAb.getMaxBid().compareTo(currentPrice) == 0) {
                        executeBid(connection, auction, winnerAb.getUserId(), currentPrice);
                        deactivateAutoBid(connection, winnerAb, winnerAb.getUserId(), currentPrice);
                        connection.commit();
                        EventBus.getInstance().publish(new BidEvent(
                            auctionId, winnerAb.getUserId(), leaderId,
                            currentPrice, currentPrice, LocalDateTime.now()
                        ));
                        return;
                    }

                    BigDecimal userIncrement = winnerAb.getBidIncrement();
                    BigDecimal auctionMinIncrement = BidService.getInstance().getBidIncrement(currentPrice);
                    BigDecimal effectiveIncrement = (userIncrement != null && userIncrement.compareTo(auctionMinIncrement) > 0)
                        ? userIncrement : auctionMinIncrement;

                    BigDecimal bidWithUserIncrement;
                    BigDecimal bidWithMinIncrement;

                    if (currentPrice.compareTo(BigDecimal.ZERO) == 0) {
                        bidWithUserIncrement = auction.getStartingPrice();
                        bidWithMinIncrement = auction.getStartingPrice();
                    } else {
                        bidWithUserIncrement = currentPrice.add(effectiveIncrement);
                        bidWithMinIncrement = currentPrice.add(auctionMinIncrement);
                    }

                    BigDecimal finalPrice;
                    boolean winnerDeactivated = false;
                    
                    if (bidWithUserIncrement.compareTo(winnerAb.getMaxBid()) <= 0) {
                        finalPrice = bidWithUserIncrement;
                    } else if (bidWithMinIncrement.compareTo(winnerAb.getMaxBid()) <= 0) {
                        finalPrice = bidWithMinIncrement;
                        winnerDeactivated = true;
                    } else {
                        // Max bid is not enough to beat the competitor with minimum increment
                        deactivateAutoBid(connection, winnerAb, leaderId, currentPrice);
                        connection.commit();
                        return;
                    }

                    executeBid(connection, auction, winnerAb.getUserId(), finalPrice);
                    
                    if (winnerDeactivated) {
                        deactivateAutoBid(connection, winnerAb, winnerAb.getUserId(), finalPrice);
                    }

                    connection.commit();
                    EventBus.getInstance().publish(new BidEvent(
                        auctionId, winnerAb.getUserId(), leaderId,
                        finalPrice, currentPrice, LocalDateTime.now()
                    ));
                    return;
                }
            }

            AutoBid runnerUpAb = validBids.get(1);

            for (int i = 1; i < validBids.size(); i++) {
                AutoBid loser = validBids.get(i);
                deactivateAutoBid(connection, loser, leaderId, currentPrice);
            }

            BigDecimal runnerUpMax = runnerUpAb.getMaxBid();
            BigDecimal winnerMax = winnerAb.getMaxBid();

            BigDecimal userIncrement = winnerAb.getBidIncrement();
            BigDecimal auctionMinIncrement = BidService.getInstance().getBidIncrement(runnerUpMax);
            BigDecimal effectiveIncrement = (userIncrement != null && userIncrement.compareTo(auctionMinIncrement) > 0)
                ? userIncrement : auctionMinIncrement;

            BigDecimal targetPriceWithUserIncrement = runnerUpMax.add(effectiveIncrement);
            BigDecimal targetPriceWithMinIncrement = runnerUpMax.add(auctionMinIncrement);

            BigDecimal finalPrice;
            boolean winnerDeactivated = false;

            if (targetPriceWithUserIncrement.compareTo(winnerMax) <= 0) {
                finalPrice = targetPriceWithUserIncrement;
            } else if (targetPriceWithMinIncrement.compareTo(winnerMax) <= 0) {
                finalPrice = targetPriceWithMinIncrement;
                winnerDeactivated = true;
            } else {
                // If runnerUpMax < winnerMax < targetPriceWithMinIncrement, the winner still wins but at exactly winnerMax
                finalPrice = winnerMax;
                winnerDeactivated = true;
            }

            executeBid(connection, auction, winnerAb.getUserId(), finalPrice);

            if (winnerDeactivated) {
                deactivateAutoBid(connection, winnerAb, winnerAb.getUserId(), finalPrice);
            }

            connection.commit();
            EventBus.getInstance().publish(new BidEvent(
                auctionId, winnerAb.getUserId(), leaderId,
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
            long remainingSeconds = java.time.Duration.between(now, endTime).getSeconds();
            if (remainingSeconds <= 30 && remainingSeconds >= 0) {
                auctionDAO.antisnippingtime(auction.getId());
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

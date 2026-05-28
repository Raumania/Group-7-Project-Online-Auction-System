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
        executorService.submit(() -> {
            try {
                processAutoBids(auctionId, currentHighestBidderId);
            } catch (Exception e) {
                System.err.println("[BidEngine] Critical error during auto bid processing for auction: " + auctionId);
                e.printStackTrace();
            }
        });
    }

    private void processAutoBids(int auctionId, int currentHighestBidderId) throws SQLException {
        while (true) {
            Connection connection = null;
            try {
                connection = DatabaseConnection.getConnection();
                connection.setAutoCommit(false);

                // 1. Lock the auction row to fetch the latest data (prevent concurrency race conditions)
                Auction auction = auctionDAO.findByIdForUpdate(connection, auctionId);
                if (auction == null) {
                    System.out.println("[BidEngine] Auction not found, id = " + auctionId);
                    connection.commit();
                    break;
                }

                if (auction.getStatus() != AuctionStatus.RUNNING) {
                    System.out.println("[BidEngine] Auction " + auctionId + " is not in RUNNING status. Stopping Auto Bid.");
                    connection.commit();
                    break;
                }

                int highestBidderId = auction.getHighestBidderId() != null ? auction.getHighestBidderId() : 0;
                BigDecimal currentPrice = auction.getCurrentPrice() != null ? auction.getCurrentPrice() : BigDecimal.ZERO;

                // 2. Fetch all active auto bids for this auction from RAM Store
                List<AutoBid> activeBids = AutoBidStore.getInstance().getActiveAutoBidsByAuctionId(auctionId);
                
                // Exclude the current highest bidder (do not bid against oneself)
                activeBids.removeIf(ab -> ab.getUserId() == highestBidderId);

                if (activeBids.isEmpty()) {
                    System.out.println("[BidEngine] No other active auto bid configurations found for auction " + auctionId);
                    connection.commit();
                    break;
                }

                // 3. Calculate next required bid amount
                // If no one has placed a bid, the minimum next bid is the Starting Price
                BigDecimal nextBidAmount;
                if (currentPrice.compareTo(BigDecimal.ZERO) == 0) {
                    nextBidAmount = auction.getStartingPrice();
                } else {
                    // Use system standard bid increment
                    BigDecimal increment = BidService.getInstance().getBidIncrement(currentPrice);
                    nextBidAmount = currentPrice.add(increment);
                }

                // 4. Sort to find the most optimal auto bid:
                // Priority 1: Highest max_bid
                // Priority 2: Oldest configuration (createdAt) ascending
                activeBids.sort((a, b) -> {
                    int cmp = b.getMaxBid().compareTo(a.getMaxBid());
                    if (cmp != 0) return cmp;
                    return a.getCreatedAt().compareTo(b.getCreatedAt());
                });

                AutoBid eligibleAutoBid = null;
                for (AutoBid ab : activeBids) {
                    User bidder = userService.getUserById(ab.getUserId());
                    
                    if (bidder == null || !bidder.hasRole(UserRole.BIDDER)) {
                        // Deactivate invalid configuration
                        System.out.println("[BidEngine] Deactivating Auto Bid due to invalid User ID " + ab.getUserId() + " or missing BIDDER role");
                        autoBidDAO.deactivate(connection, ab.getId());
                        AutoBidStore.getInstance().deactivateAutoBid(ab.getId());
                        continue;
                    }

                    // Use custom bid increment if declared
                    BigDecimal userIncrement = ab.getBidIncrement();
                    BigDecimal actualNextBid = nextBidAmount;
                    if (currentPrice.compareTo(BigDecimal.ZERO) > 0 && userIncrement != null && userIncrement.compareTo(BigDecimal.ZERO) > 0) {
                        actualNextBid = currentPrice.add(userIncrement);
                    }

                    // Check if next bid exceeds the configured max bid
                    if (actualNextBid.compareTo(ab.getMaxBid()) > 0) {
                        System.out.println("[BidEngine] Deactivating Auto Bid for User " + ab.getUserId() +
                                " because next bid " + actualNextBid + " exceeds max_bid " + ab.getMaxBid());
                        autoBidDAO.deactivate(connection, ab.getId());
                        AutoBidStore.getInstance().deactivateAutoBid(ab.getId());
                        continue;
                    }

                    // Check if the user has sufficient balance
                    if (bidder.getAvailableBalance().compareTo(actualNextBid) < 0) {
                        System.out.println("AutoBid failed for user " + bidder.getUsername() + 
                                " due to insufficient balance " + bidder.getAvailableBalance() + " for next bid " + actualNextBid);
                        autoBidDAO.deactivate(connection, ab.getId());
                        AutoBidStore.getInstance().deactivateAutoBid(ab.getId());
                        continue;
                    }

                    // Eligible! Set as the next bidding representative
                    eligibleAutoBid = ab;
                    nextBidAmount = actualNextBid;
                    break;
                }

                if (eligibleAutoBid == null) {
                    System.out.println("[BidEngine] No other eligible auto bidders found for auction " + auctionId);
                    connection.commit();
                    break;
                }

                // 5. Place the auto-bid on behalf of this user inside a Transaction
                User bidder = userService.getUserById(eligibleAutoBid.getUserId());

                // --- BALANCE UPDATE LOGIC ---
                BigDecimal newBidderAvailable = bidder.getAvailableBalance().subtract(nextBidAmount);
                BigDecimal newBidderFrozen = bidder.getFrozenBalance().add(nextBidAmount);
                UserDAO.getInstance().updateBalance(connection, bidder.getId(), newBidderAvailable, newBidderFrozen);

                User previousBidderFromRam = null;
                if (highestBidderId != 0 && currentPrice != null && currentPrice.compareTo(BigDecimal.ZERO) > 0) {
                    previousBidderFromRam = UserStore.getInstance().getUserById(highestBidderId);
                    if (previousBidderFromRam != null) {
                        BigDecimal newPrevAvailable = previousBidderFromRam.getAvailableBalance().add(currentPrice);
                        BigDecimal newPrevFrozen = previousBidderFromRam.getFrozenBalance().subtract(currentPrice);
                        UserDAO.getInstance().updateBalance(connection, previousBidderFromRam.getId(), newPrevAvailable, newPrevFrozen);
                    }
                }

                // --- ANTI-SNIPING LOGIC ---
                LocalDateTime endTime = auction.getEndTime();
                LocalDateTime now = LocalDateTime.now();
                if (endTime != null && !now.isAfter(endTime)) {
                    long remainingSeconds = java.time.Duration.between(now, endTime).getSeconds();
                    if (remainingSeconds <= 30 && remainingSeconds >= 0) {
                        auctionDAO.antisnippingtime(auctionId);
                        auction.setEndTime(endTime.plusMinutes(1));
                    }
                }
                
                BidTransaction transaction = new BidTransaction(bidder, nextBidAmount);
                bidTransactionDAO.save(connection, auctionId, transaction);

                auction.setCurrentPrice(nextBidAmount);
                auction.setHighestBidderId(bidder.getId());
                auction.setHighestBidderUsername(bidder.getUsername());
                auctionDAO.update(connection, auction);

                // Commit Transaction successfully!
                connection.commit();

                // Sync with Server Store RAM Cache
                AuctionStore.getInstance().updateAuction(auction);
                BidTransactionStore.getInstance().addBid(auctionId, transaction);
                
                bidder.freezeBalance(nextBidAmount);
                if (previousBidderFromRam != null) {
                    previousBidderFromRam.unfreezeBalance(currentPrice);
                }

                System.out.println("[BidEngine] Auto bid placed SUCCESSFUL for User " + bidder.getId() +
                        " on auction " + auctionId + " with amount " + nextBidAmount);

                // 6. Publish the new BidEvent to EventBus to trigger the next round of competition
                BidEvent nextEvent = new BidEvent(
                        auctionId, bidder.getId(), highestBidderId,
                        nextBidAmount, currentPrice, LocalDateTime.now()
                );
                EventBus.getInstance().publish(nextEvent);

                // Break the current loop; the next match will be triggered asynchronously via EventBus.publish() above!
                break;

            } catch (Exception e) {
                if (connection != null) {
                    try {
                        connection.rollback();
                    } catch (SQLException sqle) {
                        System.err.println("[BidEngine] Rollback failed");
                        sqle.printStackTrace();
                    }
                }
                throw e;
            } finally {
                if (connection != null) {
                    try {
                        connection.setAutoCommit(true);
                        connection.close();
                    } catch (SQLException sqle) {
                        System.err.println("[BidEngine] Connection close failed");
                        sqle.printStackTrace();
                    }
                }
            }
        }
    }
}

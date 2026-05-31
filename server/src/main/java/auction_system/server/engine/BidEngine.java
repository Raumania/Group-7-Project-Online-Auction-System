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

            // 1. Get auction info, lock row (Pessimistic Locking) to avoid race condition
            Auction auction = auctionDAO.findByIdForUpdate(connection, auctionId);
            if (auction == null || auction.getStatus() != AuctionStatus.RUNNING) {
                connection.commit();
                return;
            }

            // Get ACTUAL leader from DB (SELECT FOR UPDATE ensures this is the latest state).
            // currentHighestBidderId (parameter) could be stale due to async — this thread could be triggered
            // by an old event when C is leader, but at runtime B has already won.
            int actualLeaderId = (auction.getHighestBidderId() != null) ? auction.getHighestBidderId() : 0;

            // 2. Get the list of active AutoBids
            List<AutoBid> activeBids = AutoBidStore.getInstance().getActiveAutoBidsByAuctionId(auctionId);
            if (activeBids.isEmpty()) {
                connection.commit();
                return;
            }

            // Current price. If no one has bid, use startingPrice to calculate the next step
            BigDecimal currentPrice = (auction.getCurrentPrice() != null)
                    ? auction.getCurrentPrice()
                    : auction.getStartingPrice();

            // Minimum bid increment from the current price
            BigDecimal minIncrement = BidService.getInstance().getBidIncrement(currentPrice);

            // Next minimum price required to become the highest bidder
            // If no one has bid: nextPrice = startingPrice (no increment added)
            // If someone has bid:   nextPrice = currentPrice + minIncrement
            BigDecimal nextMinPrice = (auction.getHighestBidderId() == null)
                    ? currentPrice
                    : currentPrice.add(minIncrement);

            // 3. Filter valid AutoBids: valid user AND maxBid >= nextMinPrice
            //    (eligible to place at least the next minimum price)
            List<AutoBid> validBids = new ArrayList<>();
            for (AutoBid ab : activeBids) {
                User bidder = userService.getUserById(ab.getUserId());
                boolean userInvalid = (bidder == null || !bidder.hasRole(UserRole.BIDDER));
                boolean cannotAffordNextBid = (ab.getMaxBid().compareTo(nextMinPrice) < 0);

                if (userInvalid || cannotAffordNextBid) {
                    // AutoBid ineligible -> deactivate
                    deactivateAutoBid(connection, ab, actualLeaderId, currentPrice);
                } else {
                    validBids.add(ab);
                }
            }

            if (validBids.isEmpty()) {
                connection.commit();
                return;
            }

            // 4. Sort: Highest MaxBid wins. Tie-break: earlier (createdAt) -> smaller ID
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

            // 5. Use actualLeaderId (fresh from DB) instead of currentHighestBidderId (stale from event)
            //    to avoid race condition when multiple threads run concurrently.
            //    If winner is already the actual leader AND no competition -> do nothing.
            //    If there is competition (size > 1): still need to cleanup even if winner is unchanged.
            if (winnerAb.getUserId() == actualLeaderId && validBids.size() == 1) {
                connection.commit();
                return;
            }

            // 6. Calculate final bid price (finalPrice) according to Proxy Bidding rules
            BigDecimal finalPrice;
            boolean winnerDeactivated = false;

            if (validBids.size() == 1) {
                // --- Situation A: Only 1 competing AutoBid ---
                // Condition already filtered in step 3: winnerMax >= nextMinPrice
                // -> Set price exactly to nextMinPrice (lowest possible price to win)

                BigDecimal userIncrement = winnerAb.getBidIncrement();
                BigDecimal effectiveIncrement = (userIncrement != null && userIncrement.compareTo(minIncrement) > 0)
                        ? userIncrement : minIncrement;
                BigDecimal targetPrice = (auction.getHighestBidderId() == null)
                        ? currentPrice
                        : currentPrice.add(effectiveIncrement);

                if (targetPrice.compareTo(winnerMax) <= 0) {
                    // Sufficient funds according to user's preferred bid increment
                    finalPrice = targetPrice;
                } else {
                    // Preferred increment (inc) exceeds maxBid -> use platform min one last time
                    // User is considered "out of ammo" (their inc strategy is no longer usable) -> deactivate
                    finalPrice = nextMinPrice;
                    winnerDeactivated = true;
                }

            } else {
                // --- Situation B: Multiple competing AutoBids ---
                // Cancel all AutoBids from the runner-up onwards (they lost to the leader)
                for (int i = 1; i < validBids.size(); i++) {
                    deactivateAutoBid(connection, validBids.get(i), actualLeaderId, currentPrice);
                }

                AutoBid runnerUpAb = validBids.get(1);
                BigDecimal runnerUpMax = runnerUpAb.getMaxBid();

                // Bid increment calculated from the runner-up's price
                BigDecimal runnerUpIncrement = BidService.getInstance().getBidIncrement(runnerUpMax);
                BigDecimal userIncrement = winnerAb.getBidIncrement();
                BigDecimal effectiveIncrement = (userIncrement != null && userIncrement.compareTo(runnerUpIncrement) > 0)
                        ? userIncrement : runnerUpIncrement;

                // Proxy Rule: finalPrice = min(runnerUpMax + increment, winnerMax)
                //   -> Winner only pays just enough to beat the runner-up, not the full MaxBid
                BigDecimal targetWithEffective = runnerUpMax.add(effectiveIncrement);
                BigDecimal targetWithMin = runnerUpMax.add(runnerUpIncrement);

                if (targetWithEffective.compareTo(winnerMax) <= 0) {
                    finalPrice = targetWithEffective;
                } else if (targetWithMin.compareTo(winnerMax) <= 0) {
                    finalPrice = targetWithMin;
                    winnerDeactivated = true; // Budget exhausted
                } else {
                    // runnerUpMax >= winnerMax -> winner goes all-in with their MaxBid
                    finalPrice = winnerMax;
                    winnerDeactivated = true;
                }
            }

            // 7. Execute bid: write to DB, update Store, unfreeze balance of previous bidder
            executeBid(connection, auction, winnerAb.getUserId(), finalPrice);

            if (winnerDeactivated) {
                deactivateAutoBid(connection, winnerAb, winnerAb.getUserId(), finalPrice);
            }

            connection.commit();

            // 8. Publish BidEvent to notify all Clients via Socket
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

package auction_system.server.service;

import auction_system.common.enums.AuctionStatus;
import auction_system.common.enums.UserRole;
import auction_system.server.dao.AuctionDAO;
import auction_system.server.dao.BidTransactionDAO;
import auction_system.server.dao.DatabaseConnection;
import auction_system.server.exception.InvalidBidException;
import auction_system.server.model.Auction;
import auction_system.server.model.BidTransaction;
import auction_system.server.model.User;
import auction_system.server.model.User;
import auction_system.server.dao.UserDAO;
import auction_system.server.observer.BidEvent;
import auction_system.server.observer.EventBus;
import auction_system.server.store.AuctionStore;
import auction_system.server.store.UserStore;
import auction_system.server.store.BidTransactionStore;
import auction_system.server.store.AutoBidStore;
import auction_system.server.model.AutoBid;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class BidService {
    private static BidService instance;
    private final UserService userService;
    private final AuctionDAO auctionDAO;
    private final BidTransactionDAO bidTransactionDAO;
    private final AuctionStore auctionStore;
    private final BidTransactionStore bidTransactionStore;
    private final UserStore userStore;

    private BidService() {
        this.userService = UserService.getInstance();
        this.auctionDAO = AuctionDAO.getInstance();
        this.bidTransactionDAO = BidTransactionDAO.getInstance();
        this.auctionStore = AuctionStore.getInstance();
        this.bidTransactionStore = BidTransactionStore.getInstance();
        this.userStore = UserStore.getInstance();
    }

    public static BidService getInstance() {
        if (instance == null) {
            instance = new BidService();
        }
        return instance;
    }

    /*
        Update status based on time.
        This function only modifies auction object in RAM.
        Then the caller function must use auctionDAO.update(...) to save to DB.
    */
    private void updateStatusInternal(Auction auction) {
        if (auction.getStatus() == AuctionStatus.OPEN ||
                auction.getStatus() == AuctionStatus.RUNNING) {

            LocalDateTime now = LocalDateTime.now();

            if (now.isBefore(auction.getStartTime())) {
                auction.setStatus(AuctionStatus.OPEN);
            } else if (now.isBefore(auction.getEndTime())) {
                auction.setStatus(AuctionStatus.RUNNING);
            } else {
                auction.setStatus(AuctionStatus.FINISHED);
            }
        }
    }
    
    /*
        Place bid for an auction.

        Transaction is needed because:
        - insert bid transaction
        - update current_price
        - update highest_bidder_id

        SELECT FOR UPDATE is needed because:
        - multiple bidders can place bid at the same time
        - must lock auction row before checking price
    */
    public void placeBid(int auctionId, User bidder, BigDecimal amount) throws SQLException {
        Connection connection = null;
        BidEvent eventToPublish = null;

        try {
            connection = DatabaseConnection.getConnection();
            connection.setAutoCommit(false);

            Auction auction = auctionDAO.findByIdForUpdate(connection, auctionId);
            if (auction == null) {
                throw new RuntimeException("Auction not found");
            }

            Integer previousBidderId = auction.getHighestBidderId();
            BigDecimal previousPrice = auction.getCurrentPrice();
            User previousBidderFromRam = null;

            updateStatusInternal(auction);

            if (bidder == null) {
                throw new NullPointerException("Bidder cannot be null");
            }

            if (previousBidderId != null && previousBidderId.intValue() == bidder.getId()) {
                throw new InvalidBidException("You are already the highest bidder");
            }

            if (!bidder.hasRole(UserRole.BIDDER)) {
                throw new RuntimeException("Only bidder can place bid");
            }

            List<AutoBid> activeAutoBids = AutoBidStore.getInstance().getActiveAutoBidsByAuctionId(auctionId);
            boolean bidderHasActiveAutoBid = activeAutoBids.stream()
                    .anyMatch(ab -> ab.getUserId() == bidder.getId());
            if (bidderHasActiveAutoBid) {
                throw new InvalidBidException("You have an active Auto-Bid, cannot place bid manually");
            }

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new InvalidBidException("Bid amount must be greater than 0");
            }

            if (auction.getStatus() != AuctionStatus.RUNNING) {
                throw new RuntimeException("Auction is not running");
            }

            if (auction.getCurrentPrice() == null || auction.getCurrentPrice().compareTo(BigDecimal.ZERO) == 0) {
                if (amount.compareTo(auction.getStartingPrice()) < 0) {
                    throw new InvalidBidException("Bid amount must not be lower than minBid");
                }
                auction.setCurrentPrice(amount);

            } else {
                BigDecimal bidIncrement = getBidIncrement(auction.getCurrentPrice());
                if (amount.compareTo(auction.getCurrentPrice().add(bidIncrement)) < 0) {
                    throw new InvalidBidException("Bid amount must not be lower than minBid");
                }
            }

            BigDecimal newBidderAvailable = bidder.getAvailableBalance().subtract(amount);
            BigDecimal newBidderFrozen = bidder.getFrozenBalance().add(amount);

            if (newBidderAvailable.compareTo(BigDecimal.ZERO) < 0) {
                throw new RuntimeException("Not enough balance");
            }

            UserDAO.getInstance().updateBalance(connection, bidder.getId(), newBidderAvailable, newBidderFrozen);

            if (previousBidderId != null && previousBidderId != 0 && previousPrice != null) {
                boolean prevHasActiveAutoBid = activeAutoBids.stream()
                        .anyMatch(ab -> ab.getUserId() == previousBidderId);
                
                if (!prevHasActiveAutoBid) {
                    previousBidderFromRam = userStore.getUserById(previousBidderId);
                    if (previousBidderFromRam != null) {
                        previousBidderFromRam.unfreezeBalance(previousPrice);
                        UserDAO.getInstance().updateBalance(connection, previousBidderId, previousBidderFromRam.getAvailableBalance(), previousBidderFromRam.getFrozenBalance());
                    }
                }
            }
            
            LocalDateTime endTime = auction.getEndTime();
            LocalDateTime now = LocalDateTime.now();
            if (endTime == null) {
                throw new RuntimeException("the endtime is null");
            }
            if (now.isAfter(endTime)) {
                throw new RuntimeException("the auction already end");
            }
            
            long X = 30;
            long remainingSeconds = Duration.between(now, endTime).getSeconds();

            // If remaining time <= X seconds
            if (remainingSeconds <= X && remainingSeconds >= 0) {
                // Also update in-memory object time!
                auction.setEndTime(endTime.plusMinutes(1));
            }

            System.out.println("Bid placed successfully");

            BidTransaction latestTransaction = new BidTransaction(bidder, amount);
            bidTransactionDAO.save(connection, auctionId, latestTransaction);
            auction.setCurrentPrice(amount);
            auction.setHighestBidderId(bidder.getId());
            auction.setHighestBidderUsername(bidder.getUsername()); // Ensure broadcast has bidder name
            auctionDAO.update(connection, auction);
            connection.commit();

            // Sync with Server Store RAM Cache
            auctionStore.updateAuction(auction);
            bidTransactionStore.addBid(auctionId, latestTransaction);

            bidder.freezeBalance(amount);
            if (previousBidderFromRam != null) {
                previousBidderFromRam.unfreezeBalance(previousPrice);
            }

            eventToPublish = new BidEvent(
                    auctionId, bidder.getId(), previousBidderId,
                    amount, previousPrice,
                    LocalDateTime.now());

        } catch (InvalidBidException e) {
            rollback(connection);
            throw e;
        } catch (Exception e) {
            rollback(connection);
            throw new RuntimeException(e.getMessage() != null ? e.getMessage() : "Cannot place bid", e);

        } finally {
            closeConnection(connection);
            if (eventToPublish != null) {
                EventBus.getInstance().publish(eventToPublish);
            }
        }
    }

    /*
        Get bid history of an auction.
    */
    public List<BidTransaction> getHistoryBid(int auctionId) {
        findAuctionOrThrow(auctionId);
        return bidTransactionStore.getHistory(auctionId);
    }

    /*
        Get latest bid of an auction.
    */
    public BidTransaction getLatestBid(int auctionId) {
        findAuctionOrThrow(auctionId);

        BidTransaction transaction = bidTransactionStore.getLatestBid(auctionId);

        if (transaction == null) {
            throw new RuntimeException("This auction has no bids yet");
        }

        return transaction;
    }

    /*
        Get the highest bidder.
    */
    public User getHighestBidder(int auctionId) {
        Auction auction = findAuctionOrThrow(auctionId);

        if (auction.getHighestBidderId() == null) {
            throw new RuntimeException("This auction has no highest bidder yet");
        }

        return userStore.getUserById(auction.getHighestBidderId());
    }

    /*
        Get current price of the auction.
    */
    public BigDecimal getCurrentPrice(int auctionId) {
        Auction auction = findAuctionOrThrow(auctionId);
        return auction.getCurrentPrice();
    }

    private Auction findAuctionOrThrow(int auctionId) {
        if (auctionId <= 0) {
            throw new RuntimeException("Auction id must be greater than 0");
        }

        Auction auction = auctionStore.getAuctionById(auctionId);

        if (auction == null) {
            throw new RuntimeException("Auction not found");
        }

        return auction;
    }

    private void rollback(Connection connection) {
        try {
            if (connection != null) {
                connection.rollback();
            }
        } catch (Exception e) {
            throw new RuntimeException("Rollback failed", e);
        }
    }

    private void closeConnection(Connection connection) {
        try {
            if (connection != null) {
                connection.setAutoCommit(true);
                connection.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public BigDecimal getBidIncrement(BigDecimal price) {
        // Never return 0 - price 0 applies the lowest minimum increment
        if (price == null || price.compareTo(new BigDecimal("1")) < 0)  return new BigDecimal("0.05");
        else if (price.compareTo(new BigDecimal("5")) < 0)              return new BigDecimal("0.25");
        else if (price.compareTo(new BigDecimal("25")) < 0)             return new BigDecimal("0.5");
        else if (price.compareTo(new BigDecimal("100")) < 0)            return new BigDecimal("1");
        else if (price.compareTo(new BigDecimal("250")) < 0)            return new BigDecimal("2.5");
        else if (price.compareTo(new BigDecimal("500")) < 0)            return new BigDecimal("5");
        else if (price.compareTo(new BigDecimal("1000")) < 0)           return new BigDecimal("10");
        else                                                             return new BigDecimal("25");
    }
}
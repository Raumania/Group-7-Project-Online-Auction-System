package auction_system.server.service;

import auction_system.common.enums.AuctionStatus;
import auction_system.common.enums.UserRole;
import auction_system.server.dao.AutoBidDAO;
import auction_system.server.dao.DatabaseConnection;
import auction_system.server.engine.BidEngine;
import auction_system.server.model.Auction;
import auction_system.server.model.AutoBid;
import auction_system.server.model.User;
import auction_system.server.store.AutoBidStore;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;

public class AutoBidService {
    private static AutoBidService instance;

    private final UserService userService;
    private final AuctionService auctionService;
    private final BidService bidService;
    private final AutoBidDAO autoBidDAO;
    private final AutoBidStore autoBidStore;

    private AutoBidService() {
        this.userService = UserService.getInstance();
        this.auctionService = AuctionService.getInstance();
        this.bidService = BidService.getInstance();
        this.autoBidDAO = AutoBidDAO.getInstance();
        this.autoBidStore = AutoBidStore.getInstance();
    }

    public static AutoBidService getInstance() {
        if (instance == null) {
            instance = new AutoBidService();
        }
        return instance;
    }

    public void setAutoBid(int userId, int auctionId, BigDecimal maxBid, BigDecimal bidIncrement) throws SQLException {
        // 1. Verify User exists and has BIDDER role
        User user = userService.getUserById(userId);
        if (user == null) {
            throw new RuntimeException("User not found");
        }
        if (!user.hasRole(UserRole.BIDDER)) {
            throw new RuntimeException("Only users with role BIDDER can register auto bid");
        }

        // 2. Verify auction exists and is not ended
        Auction auction = auctionService.getAuctionById(auctionId);
        if (auction == null) {
            throw new RuntimeException("Auction not found");
        }
        if (auction.getStatus() == AuctionStatus.FINISHED || auction.getStatus() == AuctionStatus.CANCELLED) {
            throw new RuntimeException("Auction has already ended or been cancelled");
        }

        // 3. Verify max bid is valid
        if (maxBid == null || maxBid.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Max bid must be greater than 0");
        }

        // 4. Calculate default bid increment if empty
        BigDecimal refPrice = auction.getCurrentPrice() != null ? auction.getCurrentPrice() : auction.getStartingPrice();
        BigDecimal platformMinIncrement = bidService.getBidIncrement(refPrice);

        BigDecimal finalIncrement = bidIncrement;
        if (finalIncrement == null || finalIncrement.compareTo(BigDecimal.ZERO) <= 0) {
            finalIncrement = platformMinIncrement;
        } else if (finalIncrement.compareTo(platformMinIncrement) < 0) {
            throw new RuntimeException("Bid increment (" + finalIncrement + ") must be at least the platform minimum increment (" + platformMinIncrement + ")");
        }

        // 5. Verify bid validity compared to current price
        BigDecimal nextMinBid;
        if (auction.getCurrentPrice() == null || auction.getCurrentPrice().compareTo(BigDecimal.ZERO) == 0) {
            nextMinBid = auction.getStartingPrice();
        } else {
            nextMinBid = auction.getCurrentPrice().add(finalIncrement);
        }

        if (maxBid.compareTo(nextMinBid) < 0) {
            throw new RuntimeException("Your max bid (" + maxBid + ") is lower than the next minimum bid required (" + nextMinBid + ")");
        }

        // 6. Calculate amount to freeze for the Max Bid
        BigDecimal amountToFreeze;
        AutoBid existingAutoBid = getAutoBidConfig(userId, auctionId);
        if (existingAutoBid != null) {
            amountToFreeze = maxBid.subtract(existingAutoBid.getMaxBid());
        } else {
            Integer currentHighestBidder = auction.getHighestBidderId();
            if (currentHighestBidder != null && currentHighestBidder == userId) {
                BigDecimal currentPrice = auction.getCurrentPrice() != null ? auction.getCurrentPrice() : BigDecimal.ZERO;
                amountToFreeze = maxBid.subtract(currentPrice);
            } else {
                amountToFreeze = maxBid;
            }
        }

        if (amountToFreeze.compareTo(BigDecimal.ZERO) > 0 && user.getAvailableBalance().compareTo(amountToFreeze) < 0) {
            throw new RuntimeException("Insufficient available balance. Required to freeze for Max Bid: " + amountToFreeze);
        }

        // 7. Save or update configuration in the database and freeze balance
        AutoBid autoBid = new AutoBid(userId, auctionId, maxBid, finalIncrement);
        
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                if (amountToFreeze.compareTo(BigDecimal.ZERO) != 0) {
                    BigDecimal newAvail = user.getAvailableBalance().subtract(amountToFreeze);
                    BigDecimal newFrozen = user.getFrozenBalance().add(amountToFreeze);
                    auction_system.server.dao.UserDAO.getInstance().updateBalance(connection, user.getId(), newAvail, newFrozen);
                    
                    if (amountToFreeze.compareTo(BigDecimal.ZERO) > 0) {
                        user.freezeBalance(amountToFreeze);
                    } else {
                        user.unfreezeBalance(amountToFreeze.abs());
                    }
                }

                autoBidDAO.saveOrUpdate(connection, autoBid);
                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        }

        // Sync with memory cache
        autoBidStore.addOrUpdateAutoBid(autoBid);

        // 8. If auction is running and registerer is not highest bidder -> trigger auto bidding match immediately
        if (auction.getStatus() == AuctionStatus.RUNNING) {
            Integer currentHighestBidder = auction.getHighestBidderId();
            if (currentHighestBidder == null || currentHighestBidder != userId) {
                // Trigger asynchronous auto bid processing
                BidEngine.getInstance().triggerAutoBids(auctionId, currentHighestBidder != null ? currentHighestBidder : 0);
            }
        }
    }

    public void cancelAutoBid(int userId, int auctionId) throws SQLException {
        AutoBid ab = getAutoBidConfig(userId, auctionId);
        if (ab == null) return;
        
        try (Connection connection = DatabaseConnection.getConnection()) {
            connection.setAutoCommit(false);
            try {
                autoBidDAO.disableAutoBid(connection, userId, auctionId);
                
                Auction auction = auctionService.getAuctionById(auctionId);
                BigDecimal amountToUnfreeze;
                Integer currentHighestBidder = auction.getHighestBidderId();
                if (currentHighestBidder != null && currentHighestBidder == userId) {
                    BigDecimal currentPrice = auction.getCurrentPrice() != null ? auction.getCurrentPrice() : BigDecimal.ZERO;
                    amountToUnfreeze = ab.getMaxBid().subtract(currentPrice);
                } else {
                    amountToUnfreeze = ab.getMaxBid();
                }

                if (amountToUnfreeze.compareTo(BigDecimal.ZERO) > 0) {
                    User user = userService.getUserById(userId);
                    BigDecimal newAvail = user.getAvailableBalance().add(amountToUnfreeze);
                    BigDecimal newFrozen = user.getFrozenBalance().subtract(amountToUnfreeze);
                    auction_system.server.dao.UserDAO.getInstance().updateBalance(connection, user.getId(), newAvail, newFrozen);
                    user.unfreezeBalance(amountToUnfreeze);
                }

                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        }
        // Sync with memory cache
        autoBidStore.disableAutoBid(userId, auctionId);
    }

    public AutoBid getAutoBidConfig(int userId, int auctionId) {
        java.util.List<AutoBid> activeBids = autoBidStore.getActiveAutoBidsByAuctionId(auctionId);
        for (AutoBid ab : activeBids) {
            if (ab.getUserId() == userId) {
                return ab;
            }
        }
        return null;
    }
}

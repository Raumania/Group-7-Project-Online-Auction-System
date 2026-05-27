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
        BigDecimal finalIncrement = bidIncrement;
        if (finalIncrement == null || finalIncrement.compareTo(BigDecimal.ZERO) <= 0) {
            BigDecimal refPrice = auction.getCurrentPrice() != null ? auction.getCurrentPrice() : auction.getStartingPrice();
            finalIncrement = bidService.getBidIncrement(refPrice);
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

        // 6. Verify user balance is sufficient for the next minimum bid
        if (user.getBalance().compareTo(nextMinBid) < 0) {
            throw new RuntimeException("Insufficient balance to place the next required bid of " + nextMinBid);
        }

        // 7. Save or update configuration in the database
        AutoBid autoBid = new AutoBid(userId, auctionId, maxBid, finalIncrement);
        
        try (Connection connection = DatabaseConnection.getConnection()) {
            autoBidDAO.saveOrUpdate(connection, autoBid);
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
        try (Connection connection = DatabaseConnection.getConnection()) {
            autoBidDAO.disableAutoBid(connection, userId, auctionId);
        }
        // Sync with memory cache
        autoBidStore.disableAutoBid(userId, auctionId);
    }
}

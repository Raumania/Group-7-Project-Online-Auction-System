package auction_system.server.service;

import auction_system.server.dao.AuctionDAO;
import auction_system.server.dao.BidTransactionDAO;
import auction_system.server.model.Auction;
import auction_system.server.model.BidTransaction;
import auction_system.server.model.User;
import auction_system.server.model.UserRole;

import java.util.List;

public class BidService {

    private AuctionService auctionService;
    private UserService userService;
    private AuctionDAO auctionDAO;
    private BidTransactionDAO bidTransactionDAO;

    public BidService() {
        this.auctionService = AuctionService.getInstance();
        this.userService = new UserService();
        this.auctionDAO = new AuctionDAO();
        this.bidTransactionDAO = new BidTransactionDAO();
    }

    /*
        CẬP NHẬT: Đổi tham số String auctionId thành int auctionId.
    */
    public void placeBid(int auctionId, User bidder, double amount) {
        Auction auction = findAuctionOrThrow(auctionId);

        if (bidder == null) {
            throw new RuntimeException("Bidder cannot be null");
        }

        if (!bidder.hasRole(UserRole.BIDDER)) {
            throw new RuntimeException("Only bidder can place bid");
        }

        if (amount <= 0) {
            throw new RuntimeException("Bid amount must be greater than 0");
        }

        User realBidder = userService.getUserById(bidder.getId());

        if (!realBidder.hasRole(UserRole.BIDDER)) {
            throw new RuntimeException("Only bidder can place bid");
        }

        if (realBidder.getBalance() < amount) {
            throw new RuntimeException("Not enough balance");
        }

        if (auction.getHighestBidder() != null) {
            User oldHighestBidder = auction.getHighestBidder();
            userService.deposit(oldHighestBidder.getId(), auction.getCurrentPrice());
        }

        userService.withdraw(realBidder.getId(), amount);

        auction.placeBid(realBidder, amount);

        List<BidTransaction> bidHistory = auction.getBidHistory();
        BidTransaction latestTransaction = bidHistory.get(bidHistory.size() - 1);

        /*
            Lưu bid transaction với auctionId kiểu int.
            Hãy chắc chắn rằng BidTransactionDAO.save() cũng đã được cập nhật để nhận int.
        */
        bidTransactionDAO.save(auctionId, latestTransaction);

        boolean updated = auctionDAO.update(auction);

        if (!updated) {
            throw new RuntimeException("Cannot update auction after bid");
        }
    }

    public List<BidTransaction> getBidHistory(int auctionId) {
        findAuctionOrThrow(auctionId);
        return bidTransactionDAO.findByAuctionId(auctionId);
    }

    public BidTransaction getLatestBid(int auctionId) {
        findAuctionOrThrow(auctionId);

        BidTransaction transaction = bidTransactionDAO.findLatestByAuctionId(auctionId);

        if (transaction == null) {
            throw new RuntimeException("This auction has no bids yet");
        }

        return transaction;
    }

    public User getHighestBidder(int auctionId) {
        Auction auction = findAuctionOrThrow(auctionId);
        return auction.getHighestBidder();
    }

    public double getCurrentPrice(int auctionId) {
        Auction auction = findAuctionOrThrow(auctionId);
        return auction.getCurrentPrice();
    }

    public int getTotalBids(int auctionId) {
        findAuctionOrThrow(auctionId);
        return bidTransactionDAO.countByAuctionId(auctionId);
    }

    public boolean hasBids(int auctionId) {
        return getTotalBids(auctionId) > 0;
    }

    public boolean isHighestBidder(int auctionId, User bidder) {
        if (bidder == null) {
            return false;
        }

        if (!bidder.hasRole(UserRole.BIDDER)) {
            return false;
        }

        Auction auction = findAuctionOrThrow(auctionId);

        if (auction.getHighestBidder() == null) {
            return false;
        }

        return auction.getHighestBidder().getId().equals(bidder.getId());
    }

    public boolean auctionExists(int auctionId) {
        // int không thể null, nên chỉ cần check <= 0
        if (auctionId <= 0) {
            return false;
        }

        // Chuyển int sang String để gọi DAO cũ
        return auctionDAO.findById(String.valueOf(auctionId)) != null;
    }

    private Auction findAuctionOrThrow(int auctionId) {
        if (auctionId <= 0) {
            throw new RuntimeException("Auction id must be greater than 0");
        }

        // Chuyển int sang String để gọi Service cũ
        Auction auction = auctionService.getAuctionById(String.valueOf(auctionId));

        if (auction == null) {
            throw new RuntimeException("Auction not found");
        }

        return auction;
    }
}
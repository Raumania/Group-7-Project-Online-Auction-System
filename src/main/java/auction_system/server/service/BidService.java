package auction_system.server.service;

import auction_system.common.enums.AuctionStatus;
import auction_system.common.enums.UserRole;
import auction_system.server.dao.AuctionDAO;
import auction_system.server.dao.BidTransactionDAO;
import auction_system.server.exception.InvalidBidException;
import auction_system.server.model.Auction;
import auction_system.server.model.BidTransaction;
import auction_system.server.model.User;
import auction_system.server.observer.BidEvent;
import auction_system.server.observer.EventBus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class BidService {

    private final AuctionService auctionService;
    private final UserService userService;
    private final AuctionDAO auctionDAO;
    private final BidTransactionDAO bidTransactionDAO;
    private ReentrantLock reetrantlock = new ReentrantLock();

    public BidService() {
        this.auctionService = AuctionService.getInstance();
        this.userService = new UserService();
        this.auctionDAO = new AuctionDAO();
        this.bidTransactionDAO = new BidTransactionDAO();
    }

    /*
        Đặt bid cho một auction.

        auctionId là int vì auctions.id trong database là INT AUTO_INCREMENT.
        bidder là User, nhưng bắt buộc phải có role BIDDER.

        currentPrice có thể null nếu auction chưa có ai bid.
    */

    private void updateStatusInternal(int auctionId) {
        Auction auction = findAuctionOrThrow(auctionId);
        if (auction.getStatus() == AuctionStatus.OPEN || auction.getStatus() == AuctionStatus.RUNNING) {
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

    // Public method có lock (cho Scheduler gọi)
    public void updateStatus(int auctionId) {
        reetrantlock.lock();
        try {
            updateStatusInternal(auctionId);
        } finally {
            reetrantlock.unlock();
        }
    }
    public void placeBid(int auctionId, User bidder, double amount) {
        Auction auction = findAuctionOrThrow(auctionId); //ném exc

        BidEvent eventToPublish = null;
        reetrantlock.lock();

        try {
            updateStatusInternal(auctionId);

            if (bidder == null) {
                throw new NullPointerException("Bidder cannot be null");
            }

            if (!bidder.hasRole(UserRole.BIDDER)) {
                throw new RuntimeException("Only bidder can place bid");
            }

            if (amount <= 0) {
                throw new InvalidBidException("Bid amount must be greater than 0");
            }

             auction.setCurrentPrice(auction.getStartingPrice());

        /*
            Nếu currentPrice == null nghĩa là chưa có ai bid.
            Khi đó bid đầu tiên phải lớn hơn startingPrice.

            Nếu currentPrice != null nghĩa là đã có bid.
            Khi đó bid mới phải lớn hơn currentPrice.
        */

            if (amount <= auction.getCurrentPrice() && auction.getCurrentPrice() != 0) {
                throw new InvalidBidException("Bid amount must be greater than current price");
            }

            if (bidder.getBalance() < amount) {
                throw new RuntimeException("Not enough balance");
            }


//        List<BidTransaction> bidHistory = auction.getBidHistory();
//
//        if (bidHistory == null || bidHistory.isEmpty()) {
//            throw new RuntimeException("Bid history is empty after placing bid");
//        }
//
//        BidTransaction latestTransaction = bidHistory.get(bidHistory.size() - 1);

        /*
            Lưu bid transaction xuống database.
        */
            BidTransaction latestTransaction = new BidTransaction(bidder, amount);
            bidTransactionDAO.save(auctionId, latestTransaction);

        /*
            Cập nhật auction xuống database:
            - current_price
            - highest_bidder_id
            - status nếu có
        */
            auctionDAO.update(auction);
        } finally {
            reetrantlock.unlock();
            if (eventToPublish != null) {
                EventBus.publish(eventToPublish);
            }
        }
    }

    /*
        Lấy lịch sử bid của một auction.
    */
    public List<BidTransaction> getBidHistory(int auctionId) {
        findAuctionOrThrow(auctionId);
        return bidTransactionDAO.findByAuctionId(auctionId);
    }

    /*
        Lấy bid mới nhất của một auction.
    */
    public BidTransaction getLatestBid(int auctionId) {
        findAuctionOrThrow(auctionId);

        BidTransaction transaction = bidTransactionDAO.findLatestByAuctionId(auctionId);

        if (transaction == null) {
            throw new RuntimeException("This auction has no bids yet");
        }

        return transaction;
    }

    /*
        Lấy người đang giữ giá cao nhất.
    */
    public User getHighestBidder(int auctionId) {
        Auction auction = findAuctionOrThrow(auctionId);
        return userService.getUserById(auction.getHighestBidderId());
    }

    /*
        Lấy giá hiện tại của auction.

        Vì currentPrice có thể null khi chưa có ai bid,
        nên kiểu trả về phải là Double, không phải double.
    */
    public Double getCurrentPrice(int auctionId) {
        Auction auction = findAuctionOrThrow(auctionId);
        return auction.getCurrentPrice();
    }

    /*
        Kiểm tra auction có tồn tại không.
    /*
        Hàm dùng chung để lấy auction hoặc báo lỗi.
    */
    private Auction findAuctionOrThrow(int auctionId) {
        if (auctionId <= 0) {
            throw new RuntimeException("Auction id must be greater than 0");
        }

        Auction auction = auctionService.getAuctionById(auctionId);

        if (auction == null) {
            throw new RuntimeException("Auction not found");
        }

        return auction;
    }
}

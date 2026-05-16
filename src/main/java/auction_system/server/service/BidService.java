package auction_system.server.service;

import auction_system.server.dao.AuctionDAO;
import auction_system.server.dao.BidTransactionDAO;
import auction_system.server.exception.InvalidBidException;
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
        Đặt bid cho một auction.

        auctionId là int vì auctions.id trong database là INT AUTO_INCREMENT.
        bidder là User, nhưng bắt buộc phải có role BIDDER.
    */
    public void placeBid(int auctionId, User bidder, double amount) {
        Auction auction = findAuctionOrThrow(auctionId);

        if (bidder == null) {
            throw new RuntimeException("Bidder cannot be null");
        }

        if (!bidder.hasRole(UserRole.BIDDER)) {
            throw new RuntimeException("Only bidder can place bid");
        }

        /*
            Sửa lỗi logic:
            Dù auction đã có highestBidder hay chưa,
            amount vẫn luôn phải > 0.
        */
        if (amount <= 0) {
            throw new InvalidBidException("Bid amount must be greater than 0");
        }

        /*
            Lấy lại bidder thật từ database.
            Không tin hoàn toàn object bidder truyền từ client/test vào.
        */
        User realBidder = userService.getUserById(bidder.getId());

        if (realBidder == null) {
            throw new RuntimeException("Bidder not found");
        }

        if (!realBidder.hasRole(UserRole.BIDDER)) {
            throw new RuntimeException("Only bidder can place bid");
        }

        /*
            Auction.placeBid thường đã check:
            - auction còn OPEN không
            - amount phải lớn hơn currentPrice không

            Nhưng check ở đây trước cho rõ lỗi hơn.
        */
        if (amount <= auction.getCurrentPrice()) {
            throw new InvalidBidException("Bid amount must be greater than current price");
        }

        if (realBidder.getBalance() < amount) {
            throw new RuntimeException("Not enough balance");
        }

        /*
            Nếu đã có người đang giữ giá cao nhất,
            trả lại tiền cho người đó.
        */
        if (auction.getHighestBidder() != null) {
            User oldHighestBidder = auction.getHighestBidder();
            userService.deposit(oldHighestBidder.getId(), auction.getCurrentPrice());
        }

        /*
            Trừ tiền người đặt bid mới.
        */
        userService.withdraw(realBidder.getId(), amount);

        /*
            Cập nhật auction trong RAM:
            - currentPrice
            - highestBidder
            - bidHistory
            - tạo BidTransaction mới
            - BidTransaction mới có bidTime = LocalDateTime.now()
        */
        auction.placeBid(realBidder, amount);

        /*
            Lấy bid transaction mới nhất vừa được Auction tạo ra.
        */
        List<BidTransaction> bidHistory = auction.getBidHistory();

        if (bidHistory.isEmpty()) {
            throw new RuntimeException("Bid history is empty after placing bid");
        }

        BidTransaction latestTransaction = bidHistory.get(bidHistory.size() - 1);

        /*
            Lưu bid transaction xuống database.

            BidTransactionDAO mới:
            - không insert id nữa
            - id trong bid_transactions là INT AUTO_INCREMENT
            - sau khi insert xong DAO sẽ lấy generated id rồi set lại vào object
            - bidtime lấy từ latestTransaction.getBidTime()
        */
        bidTransactionDAO.save(auctionId, latestTransaction);

        /*
            Cập nhật auction xuống database:
            - current_price
            - highest_bidder_id
            - status nếu có
        */
        boolean updated = auctionDAO.update(auction);

        if (!updated) {
            throw new RuntimeException("Cannot update auction after bid");
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
        return auction.getHighestBidder();
    }

    /*
        Lấy giá hiện tại của auction.
    */
    public double getCurrentPrice(int auctionId) {
        Auction auction = findAuctionOrThrow(auctionId);
        return auction.getCurrentPrice();
    }

    /*
        Đếm tổng số bid của một auction.
    */
    public int getTotalBids(int auctionId) {
        findAuctionOrThrow(auctionId);
        return bidTransactionDAO.countByAuctionId(auctionId);
    }

    /*
        Kiểm tra auction đã có bid chưa.
    */
    public boolean hasBids(int auctionId) {
        return getTotalBids(auctionId) > 0;
    }

    /*
        Kiểm tra user này có phải highest bidder hiện tại không.
    */
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

    /*
        Kiểm tra auction có tồn tại không.
    */
    public boolean auctionExists(int auctionId) {
        if (auctionId <= 0) {
            return false;
        }

        return auctionDAO.findById(String.valueOf(auctionId)) != null;
    }

    /*
        Hàm dùng chung để lấy auction hoặc báo lỗi.
    */
    private Auction findAuctionOrThrow(int auctionId) {
        if (auctionId <= 0) {
            throw new RuntimeException("Auction id must be greater than 0");
        }

        Auction auction = auctionService.getAuctionById(String.valueOf(auctionId));

        if (auction == null) {
            throw new RuntimeException("Auction not found");
        }

        return auction;
    }
}
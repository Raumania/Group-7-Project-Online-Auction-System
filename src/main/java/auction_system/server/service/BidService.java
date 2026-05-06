package auction_system.server.service;

import auction_system.server.model.Auction;
import auction_system.server.model.BidTransaction;
import auction_system.server.model.Bidder;
import auction_system.server.model.User;
import auction_system.server.dao.AuctionDAO;
import auction_system.server.dao.BidTransactionDAO;

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
        Đặt bid.

        Luồng:
        1. Lấy auction từ database.
        2. Kiểm tra bidder.
        3. Kiểm tra số tiền bid.
        4. Kiểm tra số dư bidder.
        5. Gọi auction.placeBid().
        6. Lấy bid transaction mới nhất trong object auction.
        7. Lưu bid transaction vào database.
        8. Update auction trong database.
    */
    public void placeBid(String auctionId, Bidder bidder, double amount) {
        Auction auction = findAuctionOrThrow(auctionId);

        if (bidder == null) {
            throw new RuntimeException("Bidder cannot be null");
        }

        if (amount <= 0) {
            throw new RuntimeException("Bid amount must be greater than 0");
        }

        /*
            Lấy bidder mới nhất từ database để kiểm tra balance.
            Tránh trường hợp object bidder truyền vào bị cũ.
        */
        User user = userService.getUserById(bidder.getId());

        if (!(user instanceof Bidder)) {
            throw new RuntimeException("Only bidder can place bid");
        }

        Bidder realBidder = (Bidder) user;

        if (realBidder.getBalance() < amount) {
            throw new RuntimeException("Not enough balance");
        }

        /*
            Nếu có highestBidder cũ thì hoàn tiền cho người đó.
            Đây là cách đơn giản cho project:
            - Bidder mới bị trừ amount
            - Highest bidder cũ được hoàn currentPrice
        */
        if (auction.getHighestBidder() != null) {
            Bidder oldHighestBidder = auction.getHighestBidder();
            userService.deposit(oldHighestBidder.getId(), auction.getCurrentPrice());
        }

        /*
            Trừ tiền bidder mới.
        */
        userService.withdraw(realBidder.getId(), amount);

        /*
            Cập nhật object auction:
            - currentPrice
            - highestBidder
            - bidHistory
        */
        auction.placeBid(realBidder, amount);

        /*
            Lấy bid mới nhất trong bidHistory.
        */
        List<BidTransaction> bidHistory = auction.getBidHistory();
        BidTransaction latestTransaction = bidHistory.get(bidHistory.size() - 1);

        /*
            Lưu bid transaction.
        */
        bidTransactionDAO.save(auctionId, latestTransaction);

        /*
            Update auction:
            - current_price
            - highest_bidder_id
            - status
        */
        boolean updated = auctionDAO.update(auction);

        if (!updated) {
            throw new RuntimeException("Cannot update auction after bid");
        }
    }

    /*
        Lấy lịch sử bid từ database.
    */
    public List<BidTransaction> getBidHistory(String auctionId) {
        findAuctionOrThrow(auctionId);
        return bidTransactionDAO.findByAuctionId(auctionId);
    }

    /*
        Lấy bid gần nhất từ database.
    */
    public BidTransaction getLatestBid(String auctionId) {
        findAuctionOrThrow(auctionId);

        BidTransaction transaction = bidTransactionDAO.findLatestByAuctionId(auctionId);

        if (transaction == null) {
            throw new RuntimeException("This auction has no bids yet");
        }

        return transaction;
    }

    public Bidder getHighestBidder(String auctionId) {
        Auction auction = findAuctionOrThrow(auctionId);
        return auction.getHighestBidder();
    }

    public double getCurrentPrice(String auctionId) {
        Auction auction = findAuctionOrThrow(auctionId);
        return auction.getCurrentPrice();
    }

    public int getTotalBids(String auctionId) {
        findAuctionOrThrow(auctionId);
        return bidTransactionDAO.countByAuctionId(auctionId);
    }

    public boolean hasBids(String auctionId) {
        return getTotalBids(auctionId) > 0;
    }

    public boolean isHighestBidder(String auctionId, Bidder bidder) {
        if (bidder == null) {
            return false;
        }

        Auction auction = findAuctionOrThrow(auctionId);

        if (auction.getHighestBidder() == null) {
            return false;
        }

        return auction.getHighestBidder().getId().equals(bidder.getId());
    }

    public boolean auctionExists(String auctionId) {
        if (auctionId == null || auctionId.trim().isEmpty()) {
            return false;
        }

        return auctionDAO.findById(auctionId) != null;
    }

    //bọc Service nhưng thêm phan kiểm tra lỗi
    private Auction findAuctionOrThrow(String auctionId) {
        if (auctionId == null || auctionId.trim().isEmpty()) {
            throw new RuntimeException("Auction id cannot be null or empty");
        }

        Auction auction = auctionService.getAuctionById(auctionId);

        if (auction == null) {
            throw new RuntimeException("Auction not found");
        }

        return auction;
    }
}
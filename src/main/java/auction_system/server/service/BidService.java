package auction_system.server.service;

import auction_system.common.enums.UserRole;
import auction_system.server.dao.AuctionDAO;
import auction_system.server.dao.BidTransactionDAO;
import auction_system.server.exception.InvalidBidException;
import auction_system.server.model.Auction;
import auction_system.server.model.BidTransaction;
import auction_system.server.model.User;

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

        currentPrice có thể null nếu auction chưa có ai bid.
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
            Nếu currentPrice == null nghĩa là chưa có ai bid.
            Khi đó bid đầu tiên phải lớn hơn startingPrice.

            Nếu currentPrice != null nghĩa là đã có bid.
            Khi đó bid mới phải lớn hơn currentPrice.
        */
        Double currentPrice = auction.getCurrentPrice();

        double priceToCompare;

        if (currentPrice == null) {
            priceToCompare = auction.getStartingPrice();
        } else {
            priceToCompare = currentPrice;
        }

        if (amount <= priceToCompare) {
            throw new InvalidBidException("Bid amount must be greater than current price");
        }

        if (realBidder.getBalance() < amount) {
            throw new RuntimeException("Not enough balance");
        }

        /*
            Nếu đã có người đang giữ giá cao nhất,
            trả lại tiền cho người đó.

            Trường hợp có highestBidder mà currentPrice lại null là dữ liệu bị sai logic.
        */
        if (auction.getHighestBidder() != null) {
            User oldHighestBidder = auction.getHighestBidder();

            Double oldCurrentPrice = auction.getCurrentPrice();

            if (oldCurrentPrice == null) {
                throw new RuntimeException("Current price is null while highest bidder exists");
            }

            userService.deposit(oldHighestBidder.getId(), oldCurrentPrice);
        }

        /*
            Trừ tiền người đặt bid mới.
        */
        userService.withdraw(realBidder.getId(), amount);

        /*
            Cập nhật auction trong RAM:
            - currentPrice = amount
            - highestBidder = realBidder
            - thêm BidTransaction vào bidHistory
        */
        auction.placeBid(realBidder, amount);

        /*
            Lấy bid transaction mới nhất vừa được Auction tạo ra.
        */
        List<BidTransaction> bidHistory = auction.getBidHistory();

        if (bidHistory == null || bidHistory.isEmpty()) {
            throw new RuntimeException("Bid history is empty after placing bid");
        }

        BidTransaction latestTransaction = bidHistory.get(bidHistory.size() - 1);

        /*
            Lưu bid transaction xuống database.
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

        Vì currentPrice có thể null khi chưa có ai bid,
        nên kiểu trả về phải là Double, không phải double.
    */
    public Double getCurrentPrice(int auctionId) {
        Auction auction = findAuctionOrThrow(auctionId);
        return auction.getCurrentPrice();
    }

    /*
        Nếu muốn lấy giá dùng để so sánh bid:
        - chưa ai bid thì trả startingPrice
        - có bid rồi thì trả currentPrice
    */
    public double getPriceToCompare(int auctionId) {
        Auction auction = findAuctionOrThrow(auctionId);

        Double currentPrice = auction.getCurrentPrice();

        if (currentPrice == null) {
            return auction.getStartingPrice();
        }

        return currentPrice;
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

        return auction.getHighestBidder().getId() == bidder.getId();
    }

    /*
        Kiểm tra auction có tồn tại không.
    */
    public boolean auctionExists(int auctionId) {
        if (auctionId <= 0) {
            return false;
        }

        return auctionDAO.findById(auctionId) != null;
    }

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

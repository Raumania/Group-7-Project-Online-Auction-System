package auction_system.server.service;

import auction_system.common.enums.AuctionStatus;
import auction_system.common.enums.UserRole;
import auction_system.server.dao.AuctionDAO;
import auction_system.server.dao.BidTransactionDAO;
import auction_system.server.dao.DatabaseConnection;
import auction_system.server.exception.serviceException.Bid.AuthorizationException;
import auction_system.server.exception.serviceException.Bid.InvalidBidException;
import auction_system.server.exception.serviceException.Auctioon.StatusException;
import auction_system.server.exception.daoException.FindingException;
import auction_system.server.model.Auction;
import auction_system.server.model.BidTransaction;
import auction_system.server.model.User;
import auction_system.server.observer.BidEvent;
import auction_system.server.observer.EventBus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;


public class BidService {
    private static BidService instance;
    private final AuctionService auctionService;
    private final UserService userService;
    private final AuctionDAO auctionDAO;
    private final BidTransactionDAO bidTransactionDAO;
    Logger logger = LoggerFactory.getLogger(BidService.class);

    private BidService() {
        this.auctionService = AuctionService.getInstance();
        this.userService = UserService.getInstance();
        this.auctionDAO = AuctionDAO.getInstance();
        this.bidTransactionDAO = BidTransactionDAO.getInstance();
    }

    public static BidService getInstance() {
        if (instance == null) {
            instance = new BidService();
        }
        return instance;
    }

    /*
        Cập nhật status dựa theo thời gian.
        Hàm này chỉ sửa object auction trong RAM.
        Sau đó hàm gọi bên ngoài phải auctionDAO.update(...) để lưu xuống DB.
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
        Cho Scheduler gọi để cập nhật trạng thái auction.
        Hàm này nên dùng transaction + SELECT FOR UPDATE
        vì nó có đọc auction rồi update status.
    */
    public void updateStatus(int auctionId) {
        Connection connection = null;

        try {
            connection = DatabaseConnection.getConnection();
            connection.setAutoCommit(false);

            Auction auction = auctionDAO.findByIdForUpdate(connection, auctionId);

            if (auction == null) {
                throw new RuntimeException("Auction not found");
            }

            updateStatusInternal(auction);

            auctionDAO.update(connection, auction);

            connection.commit();

        } catch (Exception e) {
            rollback(connection);
            throw new RuntimeException("Cannot update auction status - " + e.getMessage());

        } finally {
            closeConnection(connection);
        }
    }

    /*
        Đặt bid cho một auction.

        Cần transaction vì:
        - insert bid transaction
        - update current_price
        - update highest_bidder_id

        Cần SELECT FOR UPDATE vì:
        - nhiều bidder có thể đặt giá cùng lúc
        - phải khóa dòng auction trước khi kiểm tra giá
    */
    public void placeBid(int auctionId, User bidder, double amount) throws SQLException {
        Connection connection = null;
        BidEvent eventToPublish = null;

        try {
            connection = DatabaseConnection.getConnection();
            connection.setAutoCommit(false);

            Auction auction = auctionDAO.findByIdForUpdate(connection, auctionId);
            Integer previousBidderId = auction.getHighestBidderId();
            double previousPrice = auction.getCurrentPrice();

            updateStatusInternal(auction);

            if (bidder == null) {
                throw new NullPointerException("Bidder cannot be null");
            }

            if (!bidder.hasRole(UserRole.BIDDER)) {
                throw new AuthorizationException("Only bidder can place bid");
            }

            if (amount <= 0) {
                throw new InvalidBidException("Bid amount must be greater than 0");
            }

            if (auction.getStatus() != AuctionStatus.RUNNING) {
                throw new StatusException("Auction is not running");
            }

            if (auction.getCurrentPrice() == 0) {
                if (amount < auction.getStartingPrice()) {
                    throw new InvalidBidException("Bid amount must not be lower than minBid");
                }
                auction.setCurrentPrice(amount);

            } else {
                double bidIncrement = getBidIncrement(auction.getCurrentPrice());
                if (amount < auction.getCurrentPrice() + bidIncrement) {
                    throw new InvalidBidException("Bid amount must not be lower than minBid");
                }
            }

            if (bidder.getBalance() < amount) {
                throw new RuntimeException("Not enough balance");
            }

            logger.info("đặt giá thành công");

                BidTransaction latestTransaction = new BidTransaction(bidder, amount);
                bidTransactionDAO.save(connection,auctionId, latestTransaction);
                auction.setCurrentPrice(amount);
                auction.setHighestBidderId(bidder.getId());
                auctionDAO.update(connection, auction);
                connection.commit();

            eventToPublish = new BidEvent(
                    auctionId, bidder.getId(), previousBidderId,
                    amount, previousPrice,
                    LocalDateTime.now());

        } catch (Exception e) {
            rollback(connection);
            throw new RuntimeException("Cannot place bid" + e.getMessage());

        } finally {
            closeConnection(connection);
            if (eventToPublish != null) {
                EventBus.publish(eventToPublish);
            }
        }
    }

    /*
        Lấy lịch sử bid của một auction.
        Chỉ đọc nên không cần transaction/lock.
    */
    public List<BidTransaction> getHistoryBid(int auctionId) {
        findAuctionOrThrow(auctionId);

        return bidTransactionDAO.findByAuctionId(auctionId);
    }

    /*
        Lấy bid mới nhất của một auction.
        Chỉ đọc nên không cần transaction/lock.
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
        Chỉ đọc nên không cần transaction/lock.
    */
    public User getHighestBidder(int auctionId) throws FindingException {
        Auction auction = findAuctionOrThrow(auctionId);

        return userService.getUserById(auction.getHighestBidderId());
    }

    /*
        Lấy giá hiện tại của auction.
        Chỉ đọc nên không cần transaction/lock.
    */
    public Double getCurrentPrice(int auctionId) {
        Auction auction = findAuctionOrThrow(auctionId);
        return auction.getCurrentPrice();
    }

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

    public static double getBidIncrement(double price) {
        if (price < 1) return 0.05;
        else if (price < 5) return 0.25;
        else if (price < 25) return 0.5;
        else if (price < 100) return 1;
        else if (price < 250) return 2.5;
        else if (price < 500) return 5;
        else if (price < 1000) return 10;
        else return 25;
    }
}
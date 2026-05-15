package auction_system.server.model;

import auction_system.server.exception.AuthorizationException;
import auction_system.server.exception.InvalidBidException;
import auction_system.server.exception.ItemInformationException;
import auction_system.server.exception.StatusException;
import auction_system.server.observer.AuctionObserver;
import auction_system.server.observer.BidEvent;
import auction_system.server.observer.EventBus;
import auction_system.util.IdGenerator;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class Auction extends Entity {
    private Item item;
    private User seller;
    private double currentPrice;
    private User highestBidder;
    private List<BidTransaction> bidHistory;
    private AuctionStatus status;
    private List<AuctionObserver> observers;
    private final ReentrantLock lock = new ReentrantLock();
    private final LocalDateTime startTime =  LocalDateTime.now();
    private final LocalDateTime endTime;
    private final EventBus eventBus =  new EventBus();

    public Auction(Item item, User seller, long time) {
        super();

        if (item == null) {
            throw new NullPointerException("Item cannot be null");
        }

        if (seller == null) {
            throw new NullPointerException("Seller cannot be null");
        }
        if (!seller.hasRole(UserRole.SELLER)) {
            throw new AuthorizationException("Seller must have SELLER role");
        }

        this.id = IdGenerator.generationAuctionId();
        this.item = item;
        this.seller = seller;
        this.currentPrice = item.getStartingPrice();
        this.highestBidder = null;
        this.bidHistory = new ArrayList<>();
        this.status = AuctionStatus.SCHEDULED;
        this.observers = new ArrayList<>();
        this.endTime = startTime.plusMinutes(time);
    }

    private void updateStatusInternal() {
        if (status == AuctionStatus.SCHEDULED || status == AuctionStatus.RUNNING) {
            LocalDateTime now = LocalDateTime.now();
            if (now.isBefore(startTime)) {
                status = AuctionStatus.SCHEDULED;
            } else if (now.isBefore(endTime)) {
                status = AuctionStatus.RUNNING;
            } else {
                status = AuctionStatus.FINISHED;
            }
        }
    }

    // Public method có lock (cho Scheduler gọi)
    public void updateStatus() {
        lock.lock();
        try {
            updateStatusInternal();
        } finally {
            lock.unlock();
        }
    }
    /*
        Đặt giá mới cho auction.

        synchronized để tránh trường hợp nhiều bidder đặt giá cùng lúc
        làm currentPrice bị sai.

        Trước đây:
        placeBid(Bidder bidder, double amount)

        Bây giờ:
        placeBid(User bidder, double amount)

        User này phải có role BIDDER.
    */
    public void placeBid(User bidder, double amount) {
        BidEvent eventToPublish = null;
        lock.lock();

        try {
            updateStatusInternal();

            if (bidder == null) {
                throw new NullPointerException("Bidder cannot be null");
            }
            if (seller.getId().equals(bidder.getId())) {
                throw new AuthorizationException("Seller cannot bid");
            }

            if (!bidder.hasRole(UserRole.BIDDER)) {
                throw new AuthorizationException("Bidder must have BIDDER role");
            }

            if (status != AuctionStatus.RUNNING) {
                throw new StatusException("Auction is not open for bidding");
            }

            if (amount <= currentPrice && highestBidder != null) {
                throw new InvalidBidException("Bid must be higher than current price");
            }

            String previousBidderId = this.highestBidder.getId();
            double previousPrice = this.currentPrice;

            currentPrice = amount;
            highestBidder = bidder;

            eventToPublish = new BidEvent(
                    id, bidder.getId(), previousBidderId,
                    amount, previousPrice,
                    LocalDateTime.now()
            );


            BidTransaction transaction = new BidTransaction(bidder, amount);
            bidHistory.add(transaction);

        } finally {
            lock.unlock();
            if (eventToPublish != null) {

                eventBus.publish(eventToPublish);
            }
        }

        System.out.println("New bid " + amount + " by " + bidder.getUsername());

    }
    /*
        Bắt đầu auction.

        Nếu bạn muốn auction chỉ được bid sau khi start,
        thì nên để placeBid chỉ nhận RUNNING.

        Hiện tại mình vẫn cho bid cả OPEN và RUNNING
        để không làm hỏng logic test cũ của bạn.
    */
    public synchronized void startAuction() {
        if (status != AuctionStatus.SCHEDULED) {
            throw new StatusException("Cannot start auction from status " + status);
        }

        status = AuctionStatus.RUNNING;
    }

    /*
        Kết thúc auction.
    */
    public synchronized void closeAuction() {
        if (status == AuctionStatus.FINISHED) {
            throw new StatusException("Auction already finished");
        }

        if (status == AuctionStatus.CANCELLED) {
            throw new StatusException("Auction already cancelled");
        }

        status = AuctionStatus.FINISHED;

        if (highestBidder != null) {
            System.out.println("Winner " + highestBidder.getUsername());
        } else {
            System.out.println("No bid placed");
        }
    }

    /*
        Hủy auction.

        Chỉ nên hủy khi auction chưa FINISHED.
    */
    public synchronized void cancelAuction() {
        if (status == AuctionStatus.FINISHED) {
            throw new StatusException("Cannot cancel finished auction");
        }

        if (status == AuctionStatus.CANCELLED) {
            throw new StatusException("Auction already cancelled");
        }

        status = AuctionStatus.CANCELLED;
    }

    public Item getItem() {
        return item;
    }

    public User getSeller() {
        return seller;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public User getHighestBidder() {
        return highestBidder;
    }

    public List<BidTransaction> getBidHistory() {
        return new ArrayList<>(bidHistory);
    }

    public AuctionStatus getStatus() {
        return status;
    }

    /*
        Các setter này hữu ích cho DAO sau này.

        Khi lấy auction từ database ra,
        constructor sẽ tạo currentPrice/status/id mặc định.
        DAO cần set lại dữ liệu thật trong database.
    */
    public void setCurrentPrice(double currentPrice) {
        if (currentPrice < item.getStartingPrice()) {
            throw new ItemInformationException("Current price cannot be lower than starting price");
        }

        this.currentPrice = currentPrice;
    }

    public void setHighestBidder(User highestBidder) {
        if (highestBidder != null && !highestBidder.hasRole(UserRole.BIDDER)) {
            throw new AuthorizationException("Highest bidder must have BIDDER role");
        }

        this.highestBidder = highestBidder;
    }

    public void setStatus(AuctionStatus status) {
        if (status == null) {
            throw new StatusException("Auction status cannot be null");
        }

        this.status = status;
    }

    public void addBidTransaction(BidTransaction transaction) {
        if (transaction != null) {
            bidHistory.add(transaction);
        }
    }

    @Override
    public String toString() {
        return "Auction{id='" + id +
                "', item=" + item.getName() +
                ", seller=" + seller.getUsername() +
                ", currentPrice=" + currentPrice +
                ", highestBidder=" + (highestBidder == null ? "none" : highestBidder.getUsername()) +
                ", status=" + status +
                "}";
    }
}
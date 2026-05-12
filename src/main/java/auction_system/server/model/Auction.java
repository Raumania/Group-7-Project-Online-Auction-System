package auction_system.server.model;

import auction_system.server.exception.AuthorizationException;
import auction_system.server.exception.InvalidBidException;
import auction_system.server.exception.ItemInformationException;
import auction_system.server.exception.StatusException;
import auction_system.server.observer.AuctionObserver;
import auction_system.util.IdGenerator;

import java.util.ArrayList;
import java.util.List;

public class Auction extends Entity {
    private Item item;

    /*
        Trước đây seller là Seller.

        Bây giờ:
        - User không còn chia cứng thành Seller/Bidder nữa
        - Một User có thể có nhiều role
        - Vì vậy seller là User
        - Nhưng User này bắt buộc phải có role SELLER
    */
    private User seller;

    private double currentPrice;

    /*
        Trước đây highestBidder là Bidder.

        Bây giờ highestBidder là User,
        nhưng User này phải có role BIDDER.
    */
    private User highestBidder;

    private List<BidTransaction> bidHistory;
    private AuctionStatus status;
    private List<AuctionObserver> observers;

    public Auction(Item item, User seller) {
        super();

        if (item == null) {
            throw new NullPointerException("Item cannot be null");
        }

        if (seller == null) {
            throw new NullPointerException("Seller cannot be null");
        }
        if (item.getOwner() == null) {
            throw new RuntimeException("Item owner cannot be null");
        }

        if (!item.getOwner().getId().equals(seller.getId())) {
            throw new RuntimeException("Seller does not own this item");
        }

        /*
            Không dùng instanceof Seller nữa.
            Kiểm tra bằng role.
        */
        if (!seller.hasRole(UserRole.SELLER)) {
            throw new AuthorizationException("Seller must have SELLER role");
        }

        this.id = IdGenerator.generationAuctionId();
        this.item = item;
        this.seller = seller;
        this.currentPrice = item.getStartingPrice();
        this.highestBidder = null;
        this.bidHistory = new ArrayList<>();
        this.status = AuctionStatus.OPEN;
        this.observers = new ArrayList<>();
    }

    /*
        Thêm observer để nhận thông báo khi auction có thay đổi.
        Ví dụ: có bid mới, auction kết thúc.
    */
    public void addObserver(AuctionObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }

    /*
        Gỡ observer khỏi danh sách nhận thông báo.
    */
    public void removeObserver(AuctionObserver observer) {
        observers.remove(observer);
    }

    /*
        Bạn đang dùng tên detach cũng được.
        Mình giữ lại để không lỗi code cũ nếu bạn đã gọi detach().
    */
    public void detach(AuctionObserver observer) {
        removeObserver(observer);
    }

    /*
        Gửi thông báo cho tất cả observer.
    */
    private void notifyObservers(String message) {
        for (AuctionObserver observer : observers) {
            observer.update(this, message);
        }
    }

    /*
        Kiểm tra auction có cho đặt bid không.

        Vì bạn có cả OPEN và RUNNING:
        - OPEN: vừa tạo, có thể cho bid luôn
        - RUNNING: đã start, cũng cho bid

        Nếu sau này bạn muốn chặt chẽ hơn,
        có thể chỉ cho bid khi status == RUNNING.
    */
    private boolean canPlaceBid() {
        return status == AuctionStatus.OPEN || status == AuctionStatus.RUNNING;
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
    public synchronized void placeBid(User bidder, double amount) {
        if (bidder == null) {
            throw new NullPointerException("Bidder cannot be null");
        }

        if (!bidder.hasRole(UserRole.BIDDER)) {
            throw new AuthorizationException("Bidder must have BIDDER role");
        }

        if (!canPlaceBid()) {
            throw new StatusException("Auction is not open for bidding");
        }

        if (amount <= currentPrice) {
            throw new InvalidBidException("Bid must be higher than current price");
        }

        currentPrice = amount;
        highestBidder = bidder;

        BidTransaction transaction = new BidTransaction(bidder, amount);
        bidHistory.add(transaction);

        System.out.println("New bid " + amount + " by " + bidder.getUsername());
        notifyObservers("New bid: " + amount + " by " + bidder.getUsername());
    }

    /*
        Bắt đầu auction.

        Nếu bạn muốn auction chỉ được bid sau khi start,
        thì nên để placeBid chỉ nhận RUNNING.

        Hiện tại mình vẫn cho bid cả OPEN và RUNNING
        để không làm hỏng logic test cũ của bạn.
    */
    public synchronized void startAuction() {
        if (status != AuctionStatus.OPEN) {
            throw new StatusException("Cannot start auction from status " + status);
        }

        status = AuctionStatus.RUNNING;
        notifyObservers("Auction started");
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
            notifyObservers("Auction finished. Winner: " + highestBidder.getUsername());
        } else {
            System.out.println("No bid placed");
            notifyObservers("Auction finished with no bids.");
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
        notifyObservers("Auction cancelled");
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
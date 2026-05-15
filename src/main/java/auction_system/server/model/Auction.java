package auction_system.server.model;

import auction_system.server.exception.AuthorizationException;
import auction_system.server.exception.InvalidBidException;
import auction_system.server.exception.ItemInformationException;
import auction_system.server.exception.StatusException;
import auction_system.server.observer.AuctionObserver;
import auction_system.server.util.IdGenerator;

import java.util.ArrayList;
import java.util.List;

public class Auction extends Entity {
    private Item item;
    private User seller;

    // CẬP NHẬT: Thêm startingPrice
    private double startingPrice;
    private double currentPrice;
    private User highestBidder;

    private List<BidTransaction> bidHistory;
    private AuctionStatus status;
    private List<AuctionObserver> observers;

    /*
        CẬP NHẬT: Constructor dùng để TẠO MỚI một Auction từ Service/UI.
        Yêu cầu truyền vào startingPrice.
    */
    public Auction(Item item, User seller, double startingPrice) {
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
        this.startingPrice = startingPrice;
        this.currentPrice = startingPrice; // currentPrice ban đầu bằng startingPrice
        this.highestBidder = null;
        this.bidHistory = new ArrayList<>();
        this.status = AuctionStatus.OPEN;
        this.observers = new ArrayList<>();
    }

    /*
        CẬP NHẬT: Constructor dùng cho DAO khi lấy dữ liệu từ Database.
        DAO sẽ dùng các hàm setter để set startingPrice và currentPrice sau.
    */
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

        if (!seller.hasRole(UserRole.SELLER)) {
            throw new AuthorizationException("Seller must have SELLER role");
        }

        this.id = IdGenerator.generationAuctionId();
        this.item = item;
        this.seller = seller;
        this.highestBidder = null;
        this.bidHistory = new ArrayList<>();
        this.status = AuctionStatus.OPEN;
        this.observers = new ArrayList<>();
    }

    public void addObserver(AuctionObserver observer) {
        if (observer != null && !observers.contains(observer)) {
            observers.add(observer);
        }
    }

    public void removeObserver(AuctionObserver observer) {
        observers.remove(observer);
    }

    public void detach(AuctionObserver observer) {
        removeObserver(observer);
    }

    private void notifyObservers(String message) {
        for (AuctionObserver observer : observers) {
            observer.update(this, message);
        }
    }

    private boolean canPlaceBid() {
        return status == AuctionStatus.OPEN || status == AuctionStatus.RUNNING;
    }

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

        if (amount <= currentPrice && highestBidder != null) {
            throw new InvalidBidException("Bid must be higher than current price");
        }

        currentPrice = amount;
        highestBidder = bidder;

        BidTransaction transaction = new BidTransaction(bidder, amount);
        bidHistory.add(transaction);

        System.out.println("New bid " + amount + " by " + bidder.getUsername());
        notifyObservers("New bid: " + amount + " by " + bidder.getUsername());
    }

    public synchronized void startAuction() {
        if (status != AuctionStatus.OPEN) {
            throw new StatusException("Cannot start auction from status " + status);
        }

        status = AuctionStatus.RUNNING;
        notifyObservers("Auction started");
    }

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

    // CẬP NHẬT: Thêm getter cho startingPrice
    public double getStartingPrice() {
        return startingPrice;
    }

    // CẬP NHẬT: Thêm setter cho startingPrice
    public void setStartingPrice(double startingPrice) {
        this.startingPrice = startingPrice;
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

    public void setCurrentPrice(double currentPrice) {
        // CẬP NHẬT: So sánh trực tiếp với startingPrice của class này
        if (currentPrice < this.startingPrice) {
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
                ", startingPrice=" + startingPrice +
                ", currentPrice=" + currentPrice +
                ", highestBidder=" + (highestBidder == null ? "none" : highestBidder.getUsername()) +
                ", status=" + status +
                "}";
    }
}
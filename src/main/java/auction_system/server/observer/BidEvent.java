package auction_system.server.observer;

public record BidEvent(
    int auctionId,
    int newBidderId,
    Integer previousBidderId,   // null nếu là lần đầu
    double newPrice,
    double previousPrice,
    java.time.LocalDateTime timestamp
) {}

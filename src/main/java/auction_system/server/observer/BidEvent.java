package auction_system.server.observer;

public record BidEvent(
    String auctionId,
    String newBidderId,
    String previousBidderId,   // null nếu là lần đầu
    double newPrice,
    double previousPrice,
    java.time.LocalDateTime timestamp
) {}


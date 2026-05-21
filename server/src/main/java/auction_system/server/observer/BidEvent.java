package auction_system.server.observer;

import java.math.BigDecimal;

public record BidEvent(
    int auctionId,
    int newBidderId,
    Integer previousBidderId,   // null nếu là lần đầu
    BigDecimal newPrice,
    BigDecimal previousPrice,
    java.time.LocalDateTime timestamp
) {}
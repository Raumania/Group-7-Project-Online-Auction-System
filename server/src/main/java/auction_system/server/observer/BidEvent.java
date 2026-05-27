package auction_system.server.observer;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BidEvent(
    int auctionId,
    int newBidderId,
    Integer previousBidderId,   // null nếu là lần đầu
    BigDecimal newPrice,
    BigDecimal previousPrice,
    LocalDateTime timestamp
) {}
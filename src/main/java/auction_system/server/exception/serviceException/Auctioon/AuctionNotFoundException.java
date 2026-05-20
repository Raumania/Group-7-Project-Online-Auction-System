package auction_system.server.exception.serviceException.Auctioon;

public class AuctionNotFoundException extends RuntimeException {
    public AuctionNotFoundException(int auctionId) {
        super("Auction not found: " + auctionId);
    }
}
package auction_system.server.exception.serviceException;

public class InValidAuctionData extends RuntimeException {
    public InValidAuctionData(String message) {
        super(message);
    }
}

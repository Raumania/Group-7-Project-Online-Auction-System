import auction_system.common.enums.AuctionStatus;
import auction_system.server.model.Auction;
import auction_system.server.observer.AuctionScheduler;
import auction_system.server.service.AuctionService;

void main() {
    AuctionScheduler auctionScheduler = AuctionScheduler.getInstance();
    auctionScheduler.start();
    while (true) {}
}
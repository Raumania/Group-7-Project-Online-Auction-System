
package auction_system.server.observer;

import auction_system.common.enums.AuctionStatus;
import auction_system.server.dao.AuctionDAO;
import auction_system.server.model.Auction;
import auction_system.server.service.AuctionService;
import auction_system.server.service.BidService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.*;
//
//Tự động update trạng thái tất cả auction (tested)
public class AuctionScheduler {
    private static AuctionScheduler instance = new AuctionScheduler();
    BidService bidService =BidService.getInstance();
    AuctionDAO auctionRepository = AuctionDAO.getInstance();
    private static final Logger logger =
            LoggerFactory.getLogger(AuctionScheduler.class);

    private final Map<Integer, Auction> auctions = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(3);

    private AuctionScheduler() {
    }

    public static AuctionScheduler getInstance() {
        if (instance == null) {
            instance = new AuctionScheduler();
        }
        return instance;
    }

    public void start() {
        scheduler.scheduleAtFixedRate(
                this::syncFromDatabase,
                0, 360, TimeUnit.MILLISECONDS
        );

        scheduler.scheduleAtFixedRate(
                this::updateAuctions,
                0,
                360,
                TimeUnit.MILLISECONDS
        );
    }

    private void updateAuctions() {
        auctions.values().forEach(auction -> {
            AuctionStatus old = auction.getStatus();
            bidService.updateStatus(auction.getId());
            if (old != auction.getStatus()) {
                auctionRepository.update(auction);
                logger.info("Auction {} changed from {} to {}",
                        auction.getId(), old, auction.getStatus());
                ; //in để test
            }
        });
    }

    private void syncFromDatabase() {
        auctionRepository.findAllOpenAuctions()
                .forEach(a -> {auctions.putIfAbsent(a.getId(), a);});

        // Xóa phiên đã kết thúc khỏi RAM
        auctions.values().removeIf(this::canRemove);
    }

    public boolean canRemove(Auction auction) {
        return auction.getStatus() != AuctionStatus.OPEN
                && auction.getStatus() != AuctionStatus.RUNNING;
    }

    public void shutdown() {
        scheduler.shutdown();
    }

    public Map<Integer, Auction> getAuctions() { return auctions;
    }
}


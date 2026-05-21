package auction_system.server.observer;

import auction_system.common.enums.AuctionStatus;
import auction_system.server.dao.AuctionDAO;
import auction_system.server.model.Auction;
import auction_system.server.service.AuctionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionScheduler {
    private static AuctionScheduler instance = new AuctionScheduler();
    private final AuctionService auctionService = AuctionService.getInstance();
    private final AuctionDAO auctionRepository = AuctionDAO.getInstance();
    private static final Logger logger =
            LoggerFactory.getLogger(AuctionScheduler.class);

    private final Map<Integer, Auction> auctions = new ConcurrentHashMap<>();

    // Use a single-threaded scheduler to ensure sequential execution
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor();

    private AuctionScheduler() {
    }

    public static AuctionScheduler getInstance() {
        if (instance == null) {
            instance = new AuctionScheduler();
        }
        return instance;
    }

    public void start() {
        // Run a single, combined task periodically.
        scheduler.scheduleAtFixedRate(
                this::syncAndUpdateAll,
                0,
                500, // Run every 500ms
                TimeUnit.MILLISECONDS
        );
    }

    private void syncAndUpdateAll() {
        try {
            // Step 1: Sync all open/running auctions from the database.
            syncFromDatabase();

            // Step 2: Iterate through the in-memory auctions and update their status.
            updateStatuses();
        } catch (Exception e) {
            logger.error("Error during auction sync/update cycle", e);
        }
    }

    private void syncFromDatabase() {
        try {
            java.util.List<Auction> activeAuctions = auctionRepository.findAllOpenAuctions();
            for (Auction dbAuction : activeAuctions) {
                auctions.put(dbAuction.getId(), dbAuction);
            }
            auctions.keySet().removeIf(id -> 
                activeAuctions.stream().noneMatch(a -> a.getId() == id)
            );
        } catch (Exception e) {
            logger.error("Error syncing auctions from database", e);
        }
    }

    private void updateStatuses() {
        for (Auction auction : auctions.values()) {
            AuctionStatus oldStatus = auction.getStatus();
            auctionService.updateAuctionStatus(auction); // This method updates the DB if needed

            // Log if the status of the in-memory object has changed
            if (oldStatus != auction.getStatus()) {
                logger.info("Auction {} status changed from {} to {}",
                        auction.getId(), oldStatus, auction.getStatus());
            }
        }
    }

    public boolean canRemove(Auction auction) {
        // Remove if status is not OPEN or RUNNING.
        return auction.getStatus() != AuctionStatus.OPEN
                && auction.getStatus() != AuctionStatus.RUNNING;
    }

    public void shutdown() {
        scheduler.shutdown();
    }

    public Map<Integer, Auction> getAuctions() {
        return auctions;
    }
}
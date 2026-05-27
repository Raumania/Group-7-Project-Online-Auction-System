package auction_system.server.observer;

import auction_system.common.enums.AuctionStatus;
import auction_system.server.model.Auction;
import auction_system.server.service.AuctionService;
import auction_system.server.store.AuctionStore;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AuctionScheduler {
    private static AuctionScheduler instance = new AuctionScheduler();
    private final AuctionService auctionService = AuctionService.getInstance();

    // Single-threaded scheduler to ensure sequential execution
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private AuctionScheduler() {
    }

    public static AuctionScheduler getInstance() {
        if (instance == null) {
            instance = new AuctionScheduler();
        }
        return instance;
    }

    public void start() {
        // Periodically check and update auction statuses in-memory (0 database overhead!)
        scheduler.scheduleAtFixedRate(
                this::updateStatuses,
                0,
                500, // Run every 500ms on RAM memory cleanly!
                TimeUnit.MILLISECONDS
        );
    }

    private void updateStatuses() {
        try {
            for (Auction auction : AuctionStore.getInstance().getActiveAuctions()) {
                AuctionStatus oldStatus = auction.getStatus();
                auctionService.updateAuctionStatus(auction); // Updates state & DB & notifies client

                // Log if the status of the in-memory object has changed
                if (oldStatus != auction.getStatus()) {
                    System.out.println("[AuctionScheduler] Store: Auction " + auction.getId() + " status changed from " + oldStatus + " to " + auction.getStatus());
                }
            }
        } catch (Exception e) {
            System.err.println("[AuctionScheduler] Store: Error during in-memory status check cycle");
            e.printStackTrace();
        }
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}
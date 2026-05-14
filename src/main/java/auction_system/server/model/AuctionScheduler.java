package auction_system.server.model;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

//Tự động update trạng thái tất cả auction (tested)
public class AuctionScheduler {

    private CopyOnWriteArrayList<Auction> auctions;

    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1);

    public AuctionScheduler(CopyOnWriteArrayList<Auction> auctions) {
        this.auctions = auctions;
    }

    public void start() {
        scheduler.scheduleAtFixedRate(
                this::updateAuctions,
                0,
                1,
                TimeUnit.MILLISECONDS
        );
    }

    private void updateAuctions() {

        for (Auction auction : auctions) {

            AuctionStatus oldStatus =
                    auction.getStatus();

            auction.updateStatus();

            AuctionStatus newStatus =
                    auction.getStatus();

            if (oldStatus != newStatus) {

                System.out.println(
                        "Auction "
                                + auction.getId()
                                + " changed from "
                                + oldStatus
                                + " to "
                                + newStatus
                );
            }
        }
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}
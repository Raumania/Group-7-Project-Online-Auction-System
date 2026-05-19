
package auction_system.server.observer;

import auction_system.common.enums.AuctionStatus;
import auction_system.server.dao.AuctionDAO;
import auction_system.server.model.Auction;
import auction_system.server.service.BidService;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
//
//Tự động update trạng thái tất cả auction (tested)
public class AuctionScheduler {
    private static AuctionScheduler instance;
    BidService bidService =BidService.getInstance();
    AuctionDAO auctionRepository = AuctionDAO.getInstance();

    private final Map<Integer, Auction> auctions = new ConcurrentHashMap<>();

    private ScheduledExecutorService scheduler;

    private AuctionScheduler() {
    }

    public static AuctionScheduler getInstance() {
        if (instance == null) {
            instance = new AuctionScheduler();
        }
        return instance;
    }

    public void start() {
        scheduler =
                Executors.newScheduledThreadPool(3);
        scheduler.scheduleAtFixedRate(
                this::syncFromDatabase,
                0, 30, TimeUnit.SECONDS
        );

        scheduler.scheduleAtFixedRate(
                this::updateAuctions,
                5,
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
                System.out.println(
                        "Auction "
                                + auction.getId()
                                + " changed from "
                                + old
                ); //in để test
            }
        });
    }

    private void syncFromDatabase() {
        auctionRepository.findAllOpenAuctions()
                .forEach(a -> auctions.putIfAbsent(a.getId(), a));

        // Xóa phiên đã kết thúc khỏi RAM
        auctions.values().removeIf(this::canRemove);
    }

    public boolean canRemove(Auction auction) {
        return auction.getStatus() != AuctionStatus.OPEN
                && auction.getStatus() != AuctionStatus.RUNNING;
    }

    // vấn đề: kph nh thead vào 1 hàm mà nh hàm cx update database

    public void shutdown() {
        scheduler.shutdown();
    }

    public Map<Integer, Auction> getAuctions() {return auctions;}
}


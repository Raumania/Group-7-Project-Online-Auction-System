package auction_system.server.model;

import auction_system.common.enums.AuctionStatus;
import auction_system.server.dao.AuctionDAO;
import auction_system.server.service.BidService;

import java.util.Map;
import java.util.concurrent.*;

//Tự động update trạng thái tất cả auction (tested)
public class AuctionScheduler {
    BidService bidService = new BidService();
    AuctionDAO auctionRepository = new AuctionDAO();

    private final Map<Integer, Auction> auctions = new ConcurrentHashMap<>();

    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(3);

    public AuctionScheduler(CopyOnWriteArrayList<Auction> auctions) {
    }

    public void start() {
        scheduler.scheduleAtFixedRate(
                this::syncFromDatabase,
                0, 30, TimeUnit.SECONDS
        );

        scheduler.scheduleAtFixedRate(
                this::updateAuctions,
                0,
                1,
                TimeUnit.MILLISECONDS
        );

        scheduler.scheduleAtFixedRate(
                this::syncToDatabase,
                0,
                1,
                TimeUnit.MILLISECONDS
        )
    }

    private void updateAuctions() {
        auctions.values().forEach(auction -> {
            AuctionStatus old = auction.getStatus();
            bidService.updateStatus(auction);
            if (old != auction.getStatus()) {
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
        auctionRepository.getAllOpenAuctions()
                .forEach(a -> auctions.put(a.getId(), a));

        // Xóa phiên đã kết thúc khỏi RAM
        auctions.values().removeIf(this::canRemove);
    }

    private boolean canRemove(Auction auction) {
        return auction.getStatus() != AuctionStatus.OPEN
                && auction.getStatus() != AuctionStatus.RUNNING;
    }

    private void syncToDatabase() {
        auctionRepository.update()
    }

    // vấn đề: kph nh thead vào 1 hàm mà nh hàm cx update database








    public void shutdown() {
        scheduler.shutdown();
    }
}

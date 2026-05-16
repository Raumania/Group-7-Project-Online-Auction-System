import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

void main() throws InterruptedException {
    Auction auction = new Auction("123", 10);
    Auction auction1 = new Auction("1234", 10);
    Auction auction2 = new Auction("1235", 10);
    Auction auction3 = new Auction("1236", 10);
    Auction auction4 = new Auction("1237", 10);

    CopyOnWriteArrayList<Auction> list =
            new CopyOnWriteArrayList<>();

    list.add(auction);
    list.add(auction1);
    list.add(auction2);
    list.add(auction3);
    list.add(auction4);

    // scheduler cập nhật auction
    AuctionScheduler scheduler =
            new AuctionScheduler(list);

    scheduler.start();

    // thread pool giả lập bidder
    ExecutorService executor =
            Executors.newFixedThreadPool(10);

    // đợi scheduler chạy trước 100ms
    Thread.sleep(100);

    while (auction.getStatus() == AuctionStatus.RUNNING) {

        for (int i = 1; i <= 10; i++) {

            int bidderId = i;

            executor.submit(() -> {

                double bidAmount =
                        10 + auction.getCurrentPrice();

                try {

                    auction.placeBid(
                            "User-" + bidderId,
                            bidAmount
                    );
                    System.out.println(bidAmount);

                } catch (Exception e) {

                    System.out.println(
                            bidderId + ": " + e.getMessage()
                    );
                }
            });
        }

        // tránh submit vô hạn quá nhanh
        Thread.sleep(500);
    }

    // dừng nhận task mới
    executor.shutdown();

    // chờ task chạy xong
    executor.awaitTermination(
            10,
            TimeUnit.SECONDS
    );

    // shutdown scheduler
    scheduler.shutdown();

    System.out.println(
            "Final price: "
                    + auction.getCurrentPrice()
    );
}
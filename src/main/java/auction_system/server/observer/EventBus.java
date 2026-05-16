package auction_system.server.observer;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
// Có thể là singleton vì cả hệ thống cần 1 bus
public class EventBus {

    // Queue chứa các event chờ dispatch
    private static final BlockingQueue<BidEvent> queue =
            new LinkedBlockingQueue<>();

    private static final Map<String, Set<String>> auctions =
            new ConcurrentHashMap<>();  //FAKE DATABASE

    // Thread pool riêng cho việc notify — không dùng chung với bid thread
    private final ExecutorService dispatcher =
            Executors.newFixedThreadPool(2);

    private volatile boolean running = true;

    public EventBus() {
        // Thread đọc queue và fan-out đến observers
        Thread dispatchThread = new Thread(this::dispatchLoop, "event-bus-dispatch");
        dispatchThread.setDaemon(true);
        dispatchThread.start();
    }

    // Auction gọi cái này — nhanh, không block
    public static void publish(BidEvent event) {
        if (!queue.offer(event)) {
            System.err.println("EventBus queue full! Event dropped.");
        }
    }

    // Chạy trên thread riêng — liên tục đọc queue
    private void dispatchLoop() {
        while (running) {
            try {
                BidEvent event = queue.poll(500, TimeUnit.MILLISECONDS);
                if (event != null) {
                    notifyAll(event);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    // Mỗi observer chạy trên thread riêng trong pool — không block  , kẾT NỐI DATABASE GỬI RESPONSE NMA CH XONG

    private void notifyAll(BidEvent event) {
        Set<String> subscribers =
                new CopyOnWriteArraySet<>(auctions.get(event.auctionId()));
        for (String subscriber : subscribers) {
            dispatcher.submit(() -> {
                try {
                    //print để test -thực te la response
                    System.out.println("Thông báo biến động của phiên: " + event.auctionId());
                } catch (Exception e) {
                    // Một observer lỗi không làm hỏng observer khác
                    System.err.println("Observer error: " + e.getMessage());
                }
            });
        }
    }

    public void shutdown() {
        running = false;
        dispatcher.shutdown();
    }
}
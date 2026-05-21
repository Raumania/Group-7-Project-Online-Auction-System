package auction_system.server.observer;

import auction_system.server.model.Auction;
import auction_system.server.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

// Thiết lập Singleton thực thụ
public class EventBus {
    private static final EventBus instance = new EventBus();

    public static EventBus getInstance() {
        return instance;
    }

    // Queue chứa các event chờ dispatch
    private static final BlockingQueue<BidEvent> queue =
            new LinkedBlockingQueue<>();

    private static final Map<String, Set<String>> auctions =
            new ConcurrentHashMap<>();  //FAKE DATABASE
    private static final Logger log = LoggerFactory.getLogger(EventBus.class);

    // Danh sách lưu trữ các Observers động
    private static final List<AuctionObserver> observers = new CopyOnWriteArrayList<>();

    // Thread pool riêng cho việc notify — không dùng chung với bid thread
    private final ExecutorService dispatcher =
            Executors.newFixedThreadPool(2);

    private volatile boolean running = true;

    private EventBus() {
        // Thread đọc queue và fan-out đến observers
        Thread dispatchThread = new Thread(this::dispatchLoop, "event-bus-dispatch");
        dispatchThread.setDaemon(true);
        dispatchThread.start();
    }

    // Đăng ký observer mới
    public static void registerObserver(AuctionObserver observer) {
        observers.add(observer);
    }

    // Hủy đăng ký observer
    public static void removeObserver(AuctionObserver observer) {
        observers.remove(observer);
    }

    // Auction gọi cái này — nhanh, không block
    public static void publish(BidEvent event) {
        if (!queue.offer(event)) {
            System.err.println("EventBus queue full! Event dropped.");
        }
    }

    // Phát sự kiện khi đấu giá mới được tạo
    public static void publishAuctionCreated(Auction auction) {
        getInstance().dispatchAuctionCreated(auction);
    }

    private void dispatchAuctionCreated(Auction auction) {
        for (AuctionObserver observer : observers) {
            dispatcher.submit(() -> {
                try {
                    observer.onAuctionCreated(auction);
                } catch (Exception e) {
                    System.err.println("Observer onAuctionCreated error: " + e.getMessage());
                }
            });
        }
    }

    // Phát sự kiện khi đấu giá được chỉnh sửa
    public static void publishAuctionEdited(Auction auction) {
        getInstance().dispatchAuctionEdited(auction);
    }

    private void dispatchAuctionEdited(Auction auction) {
        for (AuctionObserver observer : observers) {
            dispatcher.submit(() -> {
                try {
                    observer.onAuctionEdited(auction);
                } catch (Exception e) {
                    System.err.println("Observer onAuctionEdited error: " + e.getMessage());
                }
            });
        }
    }

    // Phát sự kiện khi đấu giá bị xóa
    public static void publishAuctionDeleted(int auctionId) {
        getInstance().dispatchAuctionDeleted(auctionId);
    }

    private void dispatchAuctionDeleted(int auctionId) {
        for (AuctionObserver observer : observers) {
            dispatcher.submit(() -> {
                try {
                    observer.onAuctionDeleted(auctionId);
                } catch (Exception e) {
                    System.err.println("Observer onAuctionDeleted error: " + e.getMessage());
                }
            });
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

    // Mỗi observer chạy trên thread riêng trong pool — không block
    private void notifyAll(BidEvent event) {
        // 1. Thông báo cho các dynamic observers
        for (AuctionObserver observer : observers) {
            dispatcher.submit(() -> {
                try {
                    observer.onBidPlaced(event);
                } catch (Exception e) {
                    System.err.println("Observer onBidPlaced error: " + e.getMessage());
                }
            });
        }

        // 2. Thông báo theo logic cũ của NotificationService
        Set<Integer> subscribers = NotificationService.auctions.get(event.auctionId());
        if (subscribers != null) {
            for (Integer subscriber : new CopyOnWriteArraySet<>(subscribers)) {
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
    }

    public void shutdown() {
        running = false;
        dispatcher.shutdown();
    }
}
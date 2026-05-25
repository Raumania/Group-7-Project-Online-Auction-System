package auction_system.server.observer;

import auction_system.server.model.Auction;
import auction_system.server.service.NotificationService;

import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

public class EventBus {
    //singleton for EventBus (launch Event bus)
    private static EventBus instance;
    private final NotificationService notificationService = NotificationService.getInstance();
    private EventBus() {
        // Thread đọc queue và fan-out đến observers
        Thread dispatchThread = new Thread(this::dispatchLoop, "event-bus-dispatch");
        dispatchThread.setDaemon(true);
        dispatchThread.start();
    }
    public static EventBus getInstance() {
        if(instance == null) {
            instance = new EventBus();
        }
        return instance;
    }

    // Chạy trên thread riêng — liên tục đọc queue
    private volatile boolean running = true;
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
    // Queue chứa các event chờ dispatch
    private static final BlockingQueue<BidEvent> queue = new LinkedBlockingQueue<>();
    // List for storing dynamic observers
    private static final List<AuctionObserver> observers = new CopyOnWriteArrayList<>();
    // Personal thread for dispatch
    private static final ExecutorService dispatcher = Executors.newFixedThreadPool(2);

    // subscribe a new observer
    public static void registerObserver(AuctionObserver observer) {
        observers.add(observer);
    }

    // subscribe an observer
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
        dispatchAuctionCreated(auction);
    }

    private static void dispatchAuctionCreated(Auction auction) {
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
        dispatchAuctionEdited(auction);
    }

    private static void dispatchAuctionEdited(Auction auction) {
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
        dispatchAuctionDeleted(auctionId);
    }

    private static void dispatchAuctionDeleted(int auctionId) {
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



    // Mỗi observer chạy trên thread riêng trong pool — không block
    private void notifyAll(BidEvent event) {
        // 1. Notify dynamic observers
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
        Set<Integer> subscribers = notificationService.getAuctions().get(event.auctionId());
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
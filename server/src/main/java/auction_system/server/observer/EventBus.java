package auction_system.server.observer;

import auction_system.server.model.Auction;
import auction_system.server.service.NotificationService;
import auction_system.server.engine.BidEngine;

import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

public class EventBus {
    // Singleton instance
    private static EventBus instance;
    private final NotificationService notificationService = NotificationService.getInstance();

    private EventBus() {
        // Thread to read queue and fan-out to observers
        Thread dispatchThread = new Thread(this::dispatchLoop, "event-bus-dispatch");
        dispatchThread.setDaemon(true);
        dispatchThread.start();
    }

    public static synchronized EventBus getInstance() {
        if (instance == null) {
            instance = new EventBus();
        }
        return instance;
    }

    // Queue containing events waiting to be dispatched
    private static final BlockingQueue<BidEvent> queue = new LinkedBlockingQueue<>();
    // List for storing dynamic observers
    private static final List<AuctionObserver> observers = new CopyOnWriteArrayList<>();
    // Dedicated thread pool for dispatching
    private static final ExecutorService dispatcher = Executors.newFixedThreadPool(2);

    // Run on a separate thread — continuously read the queue
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

    // Register a new observer
    public static void registerObserver(AuctionObserver observer) {
        observers.add(observer);
    }

    // Place a BidEvent into the queue — fast, non-blocking
    public void publish(BidEvent event) {
        if (!queue.offer(event)) {
            System.err.println("EventBus queue full! Event dropped.");
        }
    }

    // Publish event when a new auction is created
    public void publishAuctionCreated(Auction auction) {
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

    // Publish event when an auction is edited
    public void publishAuctionEdited(Auction auction) {
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

    // Publish event when an auction is deleted
    public void publishAuctionDeleted(int auctionId) {
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

    // Each observer runs on its own thread in the pool — non-blocking
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

        // 2. Notify via NotificationService (subscribers)
        Set<Integer> subscribers = notificationService.getAuctions().get(event.auctionId());
        if (subscribers != null) {
            for (Integer subscriber : new CopyOnWriteArraySet<>(subscribers)) {
                dispatcher.submit(() -> {
                    try {
                        // Print for testing - should be replaced with actual response
                        System.out.println("Auction session update notification: " + event.auctionId());
                    } catch (Exception e) {
                        // An observer error does not break other observers
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
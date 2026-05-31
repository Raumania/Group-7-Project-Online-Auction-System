package auction_system.server;

import auction_system.server.observer.AuctionScheduler;
import auction_system.server.observer.ClientNotificationObserver;
import auction_system.server.observer.EventBus;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.Set;

public class AuctionServer {
    //Observer Pattern
    // List of all active connected clients
    private static final Set<ClientHandler> activeClients = ConcurrentHashMap.newKeySet();

    public static void addActiveClient(ClientHandler client) {
        activeClients.add(client);
    }

    public static void removeActiveClient(ClientHandler client) {
        activeClients.remove(client);
    }

    public static void broadcast(String message) {
        for (ClientHandler client : activeClients) {
            client.send(message);
        }
    }

    public static void disconnectUser(int userId) {
        for (ClientHandler client : activeClients) {
            if (client.getUserId() == userId) {
                try {
                    auction_system.common.protocol.Response response = new auction_system.common.protocol.Response(
                        auction_system.common.enums.Status.ERROR,
                        auction_system.common.enums.Action.EVENT_USER_BANNED,
                        null,
                        "Your account has been banned due to violation of platform terms."
                    );
                    client.send(auction_system.server.util.GsonUtil.toJson(response));
                    System.out.println("AuctionServer: Sent EVENT_USER_BANNED to user " + userId + ". Force-closing socket.");
                } catch (Exception e) {
                    System.err.println("Failed to send ban notification to client: " + e.getMessage());
                }
            }
        }
    }
    //end of Observer Pattern

    // Socket server used to listen for connections from clients
    private ServerSocket serverSocket;

    // Thread pool to handle multiple clients at the same time
    private ExecutorService threadPool;

    // Variable to control whether the server is running
    private volatile boolean running;

    // Server listening port
    private static final int PORT = 3636;

    // Maximum number of threads handling clients concurrently
    private static final int POOL_SIZE = 20;

    public void start() throws IOException {

        // Initialize EventBus and register ClientNotificationObserver
        EventBus.getInstance();
        EventBus.registerObserver(new ClientNotificationObserver());
        auction_system.server.engine.BidEngine.getInstance();


        // Initialize Server Stores
        auction_system.server.store.UserStore.getInstance().init();
        auction_system.server.store.AuctionStore.getInstance().init();
        auction_system.server.store.AutoBidStore.getInstance().init();
        auction_system.server.store.BidTransactionStore.getInstance().init();
        auction_system.server.store.ImageCounterStore.getInstance().init();

        // Initialize AuctionScheduler
        AuctionScheduler.getInstance().start();

        // Open server socket at port
        serverSocket = new ServerSocket(PORT);

        // Create fixed thread pool (20 threads)
        threadPool = Executors.newFixedThreadPool(POOL_SIZE);

        running = true;

        System.out.println("Auction Server started on port " + PORT + " with pool size " + POOL_SIZE);

        // Create a separate thread just to accept clients
        new Thread(() -> {
            while (running) {
                try {
                    // Wait for client connection (blocking)
                    Socket clientSocket = serverSocket.accept();

                    System.out.println("New client connected: " + clientSocket.getInetAddress());

                    // Create handler to process this client
                    ClientHandler handler = new ClientHandler(clientSocket);

                    // Submit to thread pool for processing
                    threadPool.submit(handler);

                } catch (IOException e) {
                    // If server is still running and error occurs -> print error
                    if (running) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    public void stop() {

        // Stop accept loop
        running = false;

        // Stop AuctionScheduler
        auction_system.server.observer.AuctionScheduler.getInstance().shutdown();

        try {
            // Close server socket
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (threadPool != null) {

            // No longer accept new tasks
            threadPool.shutdown();

            try {
                // Wait for threads to finish (max 5s)
                if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {

                    // If not finished -> kill
                    threadPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                threadPool.shutdownNow();
            }
        }

        System.out.println("Auction Server stopped.");
    }

    public static void main(String[] args) {
        AuctionServer server = new AuctionServer();

        try {
            server.start();

//            System.out.println("Press Enter to stop server...");
//
//            // Wait for user to press Enter to stop server
//            System.in.read();
//
//            server.stop();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
package auction_system.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AuctionServer {
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private volatile boolean running;
    private static final int PORT = 1234;
    private static final int POOL_SIZE = 20;

    public void start() throws IOException {
        serverSocket = new ServerSocket(PORT);
        threadPool = Executors.newFixedThreadPool(POOL_SIZE);
        running = true;
        System.out.println("Auction Server started on port " + PORT + " with pool size " + POOL_SIZE);

        // Luồng riêng để chấp nhận kết nối
        new Thread(() -> {
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("New client connected: " + clientSocket.getInetAddress());
                    ClientHandler handler = new ClientHandler(clientSocket);
                    threadPool.submit(handler);
                } catch (IOException e) {
                    if (running) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (threadPool != null) {
            threadPool.shutdown();
            try {
                if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
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
            System.out.println("Press Enter to stop server...");
            System.in.read();
            server.stop();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
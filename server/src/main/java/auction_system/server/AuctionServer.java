package auction_system.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AuctionServer {

    // Socket server dùng để lắng nghe kết nối từ client
    private ServerSocket serverSocket;

    // Thread pool để xử lý nhiều client cùng lúc
    private ExecutorService threadPool;

    // Biến kiểm soát server có đang chạy hay không
    private volatile boolean running;

    // Port server lắng nghe
    private static final int PORT = 3636;

    // Số lượng thread tối đa xử lý client đồng thời
    private static final int POOL_SIZE = 20;

    public void start() throws IOException {

        // Khởi chạy EventBus và đăng ký ClientNotificationObserver
        auction_system.server.observer.EventBus.getInstance();
        auction_system.server.observer.EventBus.registerObserver(new auction_system.server.observer.ClientNotificationObserver());

        // Khởi chạy AuctionScheduler
        auction_system.server.observer.AuctionScheduler.getInstance().start();

        // Mở server socket tại port
        serverSocket = new ServerSocket(PORT);

        // Tạo thread pool cố định (20 thread)
        threadPool = Executors.newFixedThreadPool(POOL_SIZE);

        running = true;

        System.out.println("Auction Server started on port " + PORT + " with pool size " + POOL_SIZE);

        // Tạo 1 luồng riêng chỉ để accept client
        new Thread(() -> {
            while (running) {
                try {
                    // Chờ client kết nối (blocking)
                    Socket clientSocket = serverSocket.accept();

                    System.out.println("New client connected: " + clientSocket.getInetAddress());

                    // Tạo handler để xử lý client này
                    ClientHandler handler = new ClientHandler(clientSocket);

                    // Đưa vào thread pool xử lý
                    threadPool.submit(handler);

                } catch (IOException e) {
                    // Nếu server vẫn đang chạy mà lỗi → in lỗi
                    if (running) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    public void stop() {

        // Dừng vòng lặp accept
        running = false;

        // Dừng AuctionScheduler
        auction_system.server.observer.AuctionScheduler.getInstance().shutdown();

        try {
            // Đóng server socket
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (threadPool != null) {

            // Không nhận task mới nữa
            threadPool.shutdown();

            try {
                // Chờ thread chạy xong (tối đa 5s)
                if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {

                    // Nếu chưa xong → kill luôn
                    threadPool.shutdownNow();
                }
            } catch (InterruptedException e) {
                threadPool.shutdownNow();
            }
        }

        System.out.println("Auction Server stopped.");
    }

    static void main(String[] args) {
        AuctionServer server = new AuctionServer();

        try {
            server.start();

            System.out.println("Press Enter to stop server...");

            // Chờ người dùng bấm Enter để tắt server
            System.in.read();

            server.stop();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
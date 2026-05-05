package com.auction.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AuctionServer {
    private ServerSocket serverSocket;
    // Dùng Thread Pool để quản lý các client handler, tránh quá tải
    private ExecutorService threadPool = Executors.newCachedThreadPool();

    public void start(int port) throws IOException {
        serverSocket = new ServerSocket(port);
        System.out.println("Auction Server is listening on port " + port);

        new Thread(() -> {
            while (!serverSocket.isClosed()) {
                try {
                    Socket socket = serverSocket.accept();
                    System.out.println("New client connected: " + socket.getInetAddress());

                    // Xử lý mỗi client trong một thread riêng từ thread pool
                    ClientHandler clientHandler = new ClientHandler(socket);
                    threadPool.submit(clientHandler);
                } catch (IOException e) {
                    if (!serverSocket.isClosed()) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    public void stop() throws IOException {
        if (serverSocket != null && !serverSocket.isClosed()) {
            serverSocket.close();
            threadPool.shutdown();
            System.out.println("Auction Server stopped.");
        }
    }

    public static void main(String[] args) {
        AuctionServer server = new AuctionServer();
        try {
            server.start(8080);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
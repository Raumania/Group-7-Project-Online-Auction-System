package auction_system.client.socket;

import auction_system.client.util.GsonUtil;
import auction_system.client.store.AuctionStore;
import auction_system.client.store.BidTransactionStore;
import auction_system.client.store.SellerAuctionStore;
import auction_system.common.dto.BidTransactionDTO;
import auction_system.client.session.UserSession;
import auction_system.common.dto.AuctionDTO;
import auction_system.common.enums.Action;
import auction_system.common.protocol.Request;
import auction_system.common.protocol.Response;
import javafx.application.Platform;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class SocketClient {
    //Singleton for socket client
    private static SocketClient instance;
    private SocketClient() {}

    public static synchronized SocketClient getInstance() {
        if(instance == null) {
            instance = new SocketClient();
        }
        return instance;
    }

    //core of socket client in below
    private Socket socket;
    private DataInputStream in;
    private DataOutputStream out;
    private boolean isRunning = false;
    private java.util.concurrent.ScheduledExecutorService heartbeatScheduler;

    // Hàng đợi an toàn đa luồng chứa các phản hồi đồng bộ (Login, Bid, v.v.)
    private final BlockingQueue<Response> responseQueue = new LinkedBlockingQueue<>();

    public void connect(String URL, int PORT) {
        try {
            socket = new Socket(URL,PORT);
            socket.setKeepAlive(true);
            socket.setTcpNoDelay(true);
            System.out.println("Connected to server");
            in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            
            // Bắt đầu luồng lắng nghe ngầm khi kết nối thành công
            isRunning = true;
            startReaderThread();
            
            // Khởi động heartbeat gửi ping định kỳ
            startHeartbeat();
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }

    // Luồng chạy ngầm liên tục đọc dữ liệu từ Server
    private void startReaderThread() {
        Thread readerThread = new Thread(() -> {
            try {
                while (isRunning) {
                    try {
                        String json = readMessage(in);
                        if (json == null) continue;
                        
                        System.out.println("Received raw JSON from server: " + maskImageBase64(json));
                        Response response = GsonUtil.fromJson(json, Response.class);
                        
                        if (response != null) {
                            // Kiểm tra các sự kiện bất đồng bộ đẩy từ Server (Push Notifications)
                            if (response.getType() == Action.EVENT_NEW_AUCTION_ADDED) {
                                handleNewAuctionEvent(response);
                            } else if (response.getType() == Action.EVENT_AUCTION_EDITED) {
                                handleAuctionEditedEvent(response);
                            } else if (response.getType() == Action.EVENT_AUCTION_DELETED) {
                                handleAuctionDeletedEvent(response);
                            } else if (response.getType() == Action.EVENT_BID_PLACED) {
                                handleBidPlacedEvent(response);
                            } else if (response.getType() == Action.PING) {
                                // Phản hồi PING/PONG giữ kết nối (heartbeat), chỉ cần log và bỏ qua
                                System.out.println("Heartbeat: received PONG from server");
                            } else {
                                // Nếu là phản hồi bình thường của Request, đưa vào hàng đợi để phương thức receive() lấy ra
                                responseQueue.put(response);
                            }
                        }
                    } catch (IOException e) {
                        System.out.println("Socket read error (inside loop): " + e.getMessage());
                        throw e; // Rethrow to let outer catch block handle socket termination
                    } catch (InterruptedException e) {
                        System.out.println("Reader thread interrupted (inside loop): " + e.getMessage());
                        Thread.currentThread().interrupt(); // Restore interrupted status
                        break; // Exit the loop
                    } catch (Throwable t) {
                        System.err.println("Unexpected error processing incoming message: " + t.getMessage());
                        t.printStackTrace();
                        // Do not crash the reader thread, just continue reading
                    }
                }
            } catch (IOException e) {
                System.out.println("Socket connection closed or read error: " + e.getMessage());
            } finally {
                isRunning = false;
                System.out.println("Reader thread stopped.");
            }
        });
        readerThread.setDaemon(true); // Để luồng tự giải phóng khi ứng dụng JavaFX tắt
        readerThread.start();
    }

    private void startHeartbeat() {
        if (heartbeatScheduler != null && !heartbeatScheduler.isShutdown()) {
            heartbeatScheduler.shutdownNow();
        }
        heartbeatScheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "socket-client-heartbeat");
            t.setDaemon(true);
            return t;
        });
        
        heartbeatScheduler.scheduleAtFixedRate(() -> {
            if (isRunning && socket != null && !socket.isClosed()) {
                try {
                    Request pingReq = new Request(Action.PING, null);
                    send(pingReq);
                } catch (Exception e) {
                    System.err.println("Failed to send heartbeat ping: " + e.getMessage());
                }
            }
        }, 30, 30, java.util.concurrent.TimeUnit.SECONDS);
    }

    // Xử lý sự kiện thêm đấu giá mới theo thời gian thực
    private void handleNewAuctionEvent(Response response) {
        try {
            String jsonData = GsonUtil.toJson(response.getData());
            AuctionDTO newAuction = GsonUtil.fromJson(jsonData, AuctionDTO.class);
            if (newAuction != null) {
                System.out.println("Real-time: Adding new auction ID: " + newAuction.getId());
                // Cập nhật UI an toàn trên luồng JavaFX
                Platform.runLater(() -> {
                    AuctionStore.getInstance().addAuction(newAuction);
                    if (UserSession.getInstance().getUser() != null && newAuction.getSellerId() == UserSession.getInstance().getUser().getId()) {
                        SellerAuctionStore.getInstance().addAuction(newAuction);
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("Failed to parse real-time auction event: " + e.getMessage());
        }
    }

    // Xử lý sự kiện chỉnh sửa đấu giá theo thời gian thực
    private void handleAuctionEditedEvent(Response response) {
        try {
            String jsonData = GsonUtil.toJson(response.getData());
            AuctionDTO updatedAuction = GsonUtil.fromJson(jsonData, AuctionDTO.class);
            if (updatedAuction != null) {
                System.out.println("Real-time: Updating edited auction ID: " + updatedAuction.getId());
                // Cập nhật UI an toàn trên luồng JavaFX
                Platform.runLater(() -> {
                    AuctionStore.getInstance().updateAuction(updatedAuction);
                    SellerAuctionStore.getInstance().updateAuction(updatedAuction);
                });
            }
        } catch (Exception e) {
            System.err.println("Failed to parse real-time auction edited event: " + e.getMessage());
        }
    }

    // Handle real-time bid placed event — pushes new transaction into BidTransactionStore
    private void handleBidPlacedEvent(Response response) {
        try {
            String jsonData = GsonUtil.toJson(response.getData());
            BidTransactionDTO newBid = GsonUtil.fromJson(jsonData, BidTransactionDTO.class);
            if (newBid != null && newBid.getAuctionId() > 0) {
                System.out.println("Real-time: New bid received for auction " + newBid.getAuctionId() + ", transaction ID: " + newBid.getId());
                BidTransactionStore.getInstance().addBid(newBid.getAuctionId(), newBid);
            }
        } catch (Exception e) {
            System.err.println("Failed to parse real-time bid placed event: " + e.getMessage());
        }
    }

    // Xử lý sự kiện xóa đấu giá theo thời gian thực
    private void handleAuctionDeletedEvent(Response response) {
        try {
            int deletedAuctionId = GsonUtil.getGson().toJsonTree(response.getData()).getAsInt();
            System.out.println("Real-time: Removing deleted auction ID: " + deletedAuctionId);
            // Cập nhật UI an toàn trên luồng JavaFX
            Platform.runLater(() -> {
                AuctionStore.getInstance().removeAuction(deletedAuctionId);
                SellerAuctionStore.getInstance().removeAuction(deletedAuctionId);
            });
        } catch (Exception e) {
            System.err.println("Failed to parse real-time auction deleted event: " + e.getMessage());
        }
    }

    private String readMessage(DataInputStream in) throws IOException {
        try {
            int length = in.readInt();
            byte[] data = new byte[length];
            in.readFully(data);
            return new String(data, StandardCharsets.UTF_8);
        } catch (EOFException e) {
            isRunning = false;
            return null;
        }
    }

    private void writeMessage(DataOutputStream out, String message) throws IOException {
        byte[] data = message.getBytes(StandardCharsets.UTF_8);
        out.writeInt(data.length);
        out.write(data);
        out.flush();
    }

    public synchronized void send(Request request) {
        try {
            String json = GsonUtil.toJson(request);
            // Gửi qua output stream chính đã được flush
            writeMessage(out, json);
            System.out.println("Sent request: " + maskImageBase64(json));
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }

    // receive() giờ đây lấy gói tin từ hàng đợi BlockingQueue, không còn bị nghẽn hay lẫn lộn dữ liệu
    public Response receive() {
        try {
            return responseQueue.take(); // Chờ và lấy Response từ Reader Thread đưa vào
        }
        catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Đóng socket an toàn
    public void disconnect() {
        isRunning = false;
        if (heartbeatScheduler != null) {
            heartbeatScheduler.shutdownNow();
        }
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
            System.out.println("Disconnected from server");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Helper ẩn chuỗi Base64 dài khi in log để dễ debug
    private String maskImageBase64(String json) {
        if (json == null) return null;
        if (json.length() > 2000) {
            return json.substring(0, 2000) + "... [PAYLOAD TRUNCATED FOR LOGGING TO PREVENT MEMORY LEAK]";
        }
        return json;
    }
}
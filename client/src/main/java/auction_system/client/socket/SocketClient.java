package auction_system.client.socket;

import auction_system.client.util.GsonUtil;
import auction_system.client.store.AuctionStore;
import auction_system.client.store.BidTransactionStore;
import auction_system.client.store.SellerAuctionStore;
import auction_system.client.store.AdminUserStore;
import auction_system.client.service.BidService;
import auction_system.common.dto.AuctionDTO;
import auction_system.common.dto.BidTransactionDTO;
import auction_system.common.dto.UserDTO;
import auction_system.client.session.UserSession;
import auction_system.common.enums.Action;
import auction_system.common.enums.Status;
import auction_system.common.protocol.Request;
import auction_system.common.protocol.Response;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.Parent;
import javafx.scene.Scene;
import java.math.BigDecimal;
import auction_system.client.controller.MainAuctionController;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;
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

    // Thread-safe queue containing synchronous responses (Login, Bid, etc.)
    private final BlockingQueue<Response> responseQueue = new LinkedBlockingQueue<>();

    public void connect(String URL, int PORT) {
        try {
            socket = new Socket(URL,PORT);
            socket.setKeepAlive(true);
            socket.setTcpNoDelay(true);
            System.out.println("Connected to server");
            in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
            out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
            
            // Start background listening thread upon successful connection
            isRunning = true;
            startReaderThread();
            
            // Start heartbeat to send periodic pings
            startHeartbeat();
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }

    // Background thread continuously reading data from Server
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
                            // Check asynchronous events pushed from Server (Push Notifications)
                            if (response.getType() == Action.EVENT_NEW_AUCTION_ADDED) {
                                handleNewAuctionEvent(response);
                            } else if (response.getType() == Action.EVENT_AUCTION_EDITED) {
                                handleAuctionEditedEvent(response);
                            } else if (response.getType() == Action.EVENT_AUCTION_DELETED) {
                                handleAuctionDeletedEvent(response);
                            } else if (response.getType() == Action.EVENT_BID_PLACED) {
                                handleBidPlacedEvent(response);
                            } else if (response.getType() == Action.EVENT_USER_BANNED) {
                                handleUserBannedEvent(response);
                            } else if (response.getType() == Action.EVENT_AUCTION_CANCELLED) {
                                handleAuctionCancelledEvent(response);
                            } else if (response.getType() == Action.GET_CURRENT_USER) {
                                handleGetCurrentUserEvent(response);
                            } else if (response.getType() == Action.PING) {
                                // PING/PONG response keeping connection alive (heartbeat), just log and ignore
                                System.out.println("Heartbeat: received PONG from server");
                            } else {
                                // If it is a normal Request response, put it in the queue for the receive() method to retrieve
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
                // Prevent network drop (Network Chaos): Report error and kick back to login screen
                if (isRunning) { // Only report error if not intentionally closing app
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Connection Lost");
                        alert.setHeaderText("Lost connection to Server");
                        alert.setContentText("Network connection interrupted or Server closed. Please check your network!");
                        alert.showAndWait();
                        
                        try {
                            // Return to login screen
                            Parent root = FXMLLoader.load(getClass().getResource("/fxml/login.fxml"));
                            auction_system.client.util.ViewSingleton.getInstance().getViewport().getScene().setRoot(root);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    });
                }
            } finally {
                isRunning = false;
                System.out.println("Reader thread stopped.");
            }
        });
        readerThread.setDaemon(true); // Let thread self-release when JavaFX app closes
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

    // Handle real-time new auction event
    private void handleNewAuctionEvent(Response response) {
        try {
            String jsonData = GsonUtil.toJson(response.getData());
            AuctionDTO newAuction = GsonUtil.fromJson(jsonData, AuctionDTO.class);
            if (newAuction != null) {
                System.out.println("Real-time: Adding new auction ID: " + newAuction.getId());
                // Safely update UI on JavaFX thread
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

    // Handle real-time auction edited event
    private void handleAuctionEditedEvent(Response response) {
        try {
            String jsonData = GsonUtil.toJson(response.getData());
            AuctionDTO updatedAuction = GsonUtil.fromJson(jsonData, AuctionDTO.class);
            if (updatedAuction != null) {
                System.out.println("Real-time: Updating edited auction ID: " + updatedAuction.getId());
                final int auctionId = updatedAuction.getId();

                // Update Store and calculate balance on JavaFX thread
                Platform.runLater(() -> {
                    UserDTO currentUser = UserSession.getInstance().getUser();
                    if (currentUser != null && currentUser.getUsername() != null) {
                        AuctionDTO oldAuction = null;
                        for (AuctionDTO a : AuctionStore.getInstance().getAuctions()) {
                            if (a.getId() == updatedAuction.getId()) {
                                oldAuction = a;
                                break;
                            }
                        }

                        if (oldAuction != null) {
                            String myUsername = currentUser.getUsername();
                            String oldBidder = oldAuction.getHighestBidderUsername();
                            String newBidder = updatedAuction.getHighestBidderUsername();
                            
                            BigDecimal oldPrice = oldAuction.getCurrentPrice() != null ? oldAuction.getCurrentPrice() : BigDecimal.ZERO;
                            BigDecimal newPrice = updatedAuction.getCurrentPrice() != null ? updatedAuction.getCurrentPrice() : BigDecimal.ZERO;

                            boolean wasHighest = myUsername.equals(oldBidder);
                            boolean isHighest = myUsername.equals(newBidder);

                            // If user is related to bid price change, instead of calculating discrepancies (especially with AutoBid),
                            // we request server to resend the latest balance for 100% accuracy.
                            if (wasHighest || isHighest) {
                                Request syncReq = new Request(Action.GET_CURRENT_USER, null);
                                send(syncReq);
                            }
                        }
                    }

                    AuctionStore.getInstance().updateAuction(updatedAuction);
                    SellerAuctionStore.getInstance().updateAuction(updatedAuction);
                });

                // Re-fetch bid history trên background thread (không block JavaFX thread)
                // Ensure bids of banned users disappear from the table immediately upon update
                new Thread(() -> {
                    try {
                        List<BidTransactionDTO> freshHistory = BidService.getInstance().getBidHistory(auctionId);
                        Platform.runLater(() -> {
                            BidTransactionStore.getInstance().setHistory(auctionId, freshHistory);
                        });
                    } catch (Exception e) {
                        System.err.println("Failed to re-fetch bid history after auction edit: " + e.getMessage());
                    }
                }, "bid-history-refresh-" + auctionId).start();
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

    // Handle real-time auction deleted event
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

    private void handleUserBannedEvent(Response response) {
        try {
            int bannedUserId = GsonUtil.getGson().toJsonTree(response.getData()).getAsInt();
            System.out.println("Real-time: Account " + bannedUserId + " has been BANNED by administrator.");

            Platform.runLater(() -> {
                try {
                    UserDTO currentUser = UserSession.getInstance().getUser();
                    if (currentUser != null && currentUser.getId() == bannedUserId) {
                        System.out.println("Real-time: I have been BANNED! Force-logging out...");
                        // Clear sessions and stores
                        UserSession.getInstance().logout();
                        AdminUserStore.getInstance().logout();
                        AuctionStore.getInstance().logout();
                        BidTransactionStore.getInstance().logout();
                        SellerAuctionStore.getInstance().logout();

                        // Wake up and kill background threads (zombie threads) stuck at receive() command
                for (int i = 0; i < 5; i++) {
                    responseQueue.offer(new auction_system.common.protocol.Response(auction_system.common.enums.Status.ERROR, auction_system.common.enums.Action.PING, null, "Force release zombie"));
                }
                
                // Show warning alert dialog
                javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                alert.setTitle("Account Banned");
                alert.setHeaderText("Notification from Administrator");
                alert.setContentText(response.getMessage() != null ? response.getMessage() : "Your account has been banned due to violation of platform terms.");
                alert.showAndWait();
                
                        // Transition to login screen
                        javafx.stage.Window activeWindow = javafx.stage.Window.getWindows().stream()
                                .filter(javafx.stage.Window::isShowing)
                                .findFirst()
                                .orElse(null);
                        
                        if (activeWindow instanceof javafx.stage.Stage stage) {
                            FXMLLoader loader = new FXMLLoader();
                            loader.setLocation(getClass().getResource("/fxml/login.fxml"));
                            Parent root = loader.load();
                            Scene scene = new Scene(root);
                            stage.setScene(scene);
                        } else {
                            System.exit(0);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Failed to force-logout banned user: " + e.getMessage());
                    e.printStackTrace();
                    System.exit(0);
                }
            });
        } catch (Exception ex) {
            System.err.println("Failed to parse real-time banned user event: " + ex.getMessage());
        }
    }

    private void handleAuctionCancelledEvent(Response response) {
        try {
            int cancelledAuctionId = GsonUtil.getGson().toJsonTree(response.getData()).getAsInt();
            System.out.println("Real-time: Auction has been cancelled: " + cancelledAuctionId);
            
            Platform.runLater(() -> {
                UserDTO currentUser = UserSession.getInstance().getUser();
                // Find the auction in store and update its status to CANCELLED
                for (AuctionDTO auction : AuctionStore.getInstance().getAuctions()) {
                    if (auction.getId() == cancelledAuctionId) {
                        // If we are the highest bidder of the cancelled auction
                        if (currentUser != null && currentUser.getUsername() != null && currentUser.getUsername().equals(auction.getHighestBidderUsername())) {
                            Request syncReq = new Request(Action.GET_CURRENT_USER, null);
                            send(syncReq);
                        }

                        auction.setStatus(auction_system.common.enums.AuctionStatus.CANCELLED);
                        AuctionStore.getInstance().updateAuction(auction);
                        break;
                    }
                }
                for (AuctionDTO auction : SellerAuctionStore.getInstance().getAuctions()) {
                    if (auction.getId() == cancelledAuctionId) {
                        auction.setStatus(auction_system.common.enums.AuctionStatus.CANCELLED);
                        SellerAuctionStore.getInstance().updateAuction(auction);
                        break;
                    }
                }
            });
        } catch (Exception e) {
            System.err.println("Failed to parse real-time auction cancelled event: " + e.getMessage());
        }
    }

    private void handleGetCurrentUserEvent(Response response) {
        try {
            if (response.getStatus() == Status.SUCCESS) {
                String userJson = GsonUtil.toJson(response.getData());
                UserDTO freshUser = GsonUtil.fromJson(userJson, UserDTO.class);
                if (freshUser != null) {
                    Platform.runLater(() -> {
                        UserDTO currentUser = UserSession.getInstance().getUser();
                        if (currentUser != null) {
                            currentUser.setAvailableBalance(freshUser.getAvailableBalance());
                            currentUser.setFrozenBalance(freshUser.getFrozenBalance());
                            if (MainAuctionController.getInstance() != null) {
                                MainAuctionController.getInstance().refreshBalance();
                            }
                            System.out.println("Balance successfully synchronized with server.");
                        }
                    });
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to parse GET_CURRENT_USER event: " + e.getMessage());
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
            // Send via the main flushed output stream
            writeMessage(out, json);
            System.out.println("Sent request: " + maskImageBase64(json));
        }
        catch(IOException e) {
            e.printStackTrace();
        }
    }

    // [HOTFIX] Synchronization wrapper method to fix Race Condition
    // Ensure each thread sending Request will exclusively wait and get its correct Response
    public synchronized Response sendAndReceive(Request request) {
        try {
            send(request);
            return receive();
        } catch (Exception e) {
            System.err.println("Error during synchronized sendAndReceive: " + e.getMessage());
            return null;
        }
    }

    // receive() now gets packets from BlockingQueue, no longer blocked or mixed data
    public Response receive() {
        try {
            return responseQueue.take(); // Wait and get Response put in by Reader Thread
        }
        catch (InterruptedException e) {
            e.printStackTrace();
            return null;
        }
    }

    // Safely close socket
    public void disconnect() {
        isRunning = false;
        responseQueue.clear(); // Clear queue to avoid affecting subsequent tests
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

    // Helper to hide long Base64 strings when logging for easier debugging
    private String maskImageBase64(String json) {
        if (json == null) return null;
        if (json.length() > 2000) {
            return json.substring(0, 2000) + "... [PAYLOAD TRUNCATED FOR LOGGING TO PREVENT MEMORY LEAK]";
        }
        return json;
    }
}
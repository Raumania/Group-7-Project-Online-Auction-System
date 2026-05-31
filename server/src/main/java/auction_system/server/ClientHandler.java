package auction_system.server;

import auction_system.common.enums.Action;
import auction_system.common.enums.Status;
import auction_system.common.protocol.Request;
import auction_system.common.protocol.Response;
import auction_system.server.controller.*;
//import auction_system.server.controller.BidController;
import auction_system.server.controller.RequestHandler;
import auction_system.server.util.GsonUtil;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final Map<Action, RequestHandler> handlers = new ConcurrentHashMap<>();
    private DataOutputStream out;
    private int userId = -1;

    // Async sender fields to prevent blocking the entire broadcast
    private final java.util.concurrent.BlockingQueue<String> outgoingQueue = new java.util.concurrent.LinkedBlockingQueue<>();
    private volatile boolean isRunning = true;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        try {
            this.socket.setKeepAlive(true);
            this.socket.setTcpNoDelay(true);
        } catch (java.net.SocketException e) {
            System.err.println("Failed to set socket options on accepted client: " + e.getMessage());
        }
        initHandlers();
    }
    private String readMessage(DataInputStream in) throws IOException {
        int length = in.readInt();

        byte[] data = new byte[length];
        in.readFully(data);

        return new String(data, StandardCharsets.UTF_8);
    }
    private void writeMessage(DataOutputStream out, String message) throws IOException {
        byte[] data = message.getBytes(StandardCharsets.UTF_8);

        out.writeInt(data.length);
        out.write(data);
        out.flush();
    }
    private void initHandlers() {
        handlers.put(Action.LOGIN, new LoginController());
        AuctionController auctionController = new AuctionController();
        handlers.put(Action.REGISTER, new RegisterController());
        handlers.put(Action.GET_ALL_AUCTIONS, auctionController);
        handlers.put(Action.GET_AUCTION_DETAIL, auctionController);
        handlers.put(Action.CREATE_AUCTION, auctionController);
        handlers.put(Action.CLOSE_AUCTION, auctionController);
        handlers.put(Action.GET_SELLER_ITEMS, auctionController);
        handlers.put(Action.DELETE_AUCTION, auctionController);
        handlers.put(Action.EDIT_AUCTION, auctionController);
        handlers.put(Action.CANCEL_AUCTION, auctionController);
        handlers.put(Action.PLACE_BID, new BidController());
        handlers.put(Action.GET_BID_HISTORY,new BidHistoryController());
        handlers.put(Action.GET_OPEN_AUCTIONS,new AuctionController());
        handlers.put(Action.CHAT_AI, new AIController());

        AutoBidController autoBidController = new AutoBidController();
        handlers.put(Action.SET_AUTO_BID, autoBidController);
        handlers.put(Action.CANCEL_AUTO_BID, autoBidController);
        handlers.put(Action.GET_AUTO_BID_CONFIG, autoBidController);

        UserController userController = new UserController();
        handlers.put(Action.GET_ALL_USERS, userController);
        handlers.put(Action.CREATE_USER, userController);
        handlers.put(Action.UPDATE_USER, userController);
        handlers.put(Action.DELETE_USER, userController);
        handlers.put(Action.BAN_USER, userController);

    }

    @Override
    public void run() {
        AuctionServer.addActiveClient(this);

        // Start dedicated sender thread to process outgoing messages asynchronously
        Thread senderThread = new Thread(() -> {
            try {
                while (isRunning && !socket.isClosed()) {
                    String msg = outgoingQueue.take();
                    synchronized (this) {
                        if (out != null && isRunning) {
                            writeMessage(out, msg);
                        }
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (IOException e) {
                System.err.println("ClientHandler sender thread encountered write error: " + e.getMessage());
                closeConnection();
            }
        }, "client-sender-" + socket.getRemoteSocketAddress());
        senderThread.setDaemon(true);
        senderThread.start();

        // FIX: Synchronize using DataInputStream and DataOutputStream like Client
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
             DataOutputStream output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()))) {

            synchronized (this) {
                this.out = output;
            }

            // Use infinite loop, readMessage will throw EOFException when Client disconnects
            while (isRunning) {
                String line;

                try {
                    line = readMessage(in);
                } catch (EOFException e) {
                    // Client disconnected
                    System.out.println("Client disconnected.");
                    break;
                }

                if (line == null) continue;

                System.out.println("Request: " + maskImageBase64(line));
                Request req = GsonUtil.fromJson(line, Request.class);

                if (req != null && req.getAction() == Action.PING) {
                    Response pongRes = new Response(Status.SUCCESS, Action.PING, null, "PONG");
                    send(GsonUtil.toJson(pongRes));
                    continue;
                }

                if (req != null && req.getAction() == Action.GET_CURRENT_USER) {
                    if (this.userId > 0) {
                        auction_system.server.model.User userFromRam = auction_system.server.store.UserStore.getInstance().getUserById(this.userId);
                        if (userFromRam != null) {
                            Response userRes = new Response(Status.SUCCESS, Action.GET_CURRENT_USER, userFromRam.toDTO(), "User profile fetched");
                            send(GsonUtil.toJson(userRes));
                        }
                    }
                    continue;
                }

                RequestHandler handler = handlers.get(req != null ? req.getAction() : null);

                try {
                    Response res;
                    if (handler != null) {
                        res = handler.handle(req);
                        if (req.getAction() == Action.LOGIN && res.getStatus() == Status.SUCCESS) {
                            try {
                                String userJson = GsonUtil.toJson(res.getData());
                                auction_system.common.dto.UserDTO loggedInUser = GsonUtil.fromJson(userJson, auction_system.common.dto.UserDTO.class);
                                if (loggedInUser != null) {
                                    this.userId = loggedInUser.getId();
                                    System.out.println("ClientHandler: User ID " + this.userId + " successfully authenticated on this socket.");
                                }
                            } catch (Exception e) {
                                System.err.println("Failed to extract userId from login response: " + e.getMessage());
                            }
                        }
                    } else {
                        res = new Response(Status.ERROR, "Unknown action: " + (req != null ? req.getAction() : "null"), null);
                    }
                    String jsonResponse = GsonUtil.toJson(res);
                    System.out.println("Respond: " + maskImageBase64(jsonResponse));
                    send(jsonResponse);
                } catch (Throwable t) {
                    System.err.println("Error handling or serializing request: " + (req != null ? req.getAction() : "null"));
                    t.printStackTrace();
                    try {
                        Response errRes = new Response(Status.ERROR, req != null ? req.getAction() : null, null, "Internal server error: " + t.getMessage());
                        String errJson = new com.google.gson.Gson().toJson(errRes);
                        System.out.println("Respond (error): " + errJson);
                        send(errJson);
                    } catch (Exception ioe) {
                        System.err.println("Failed to send error response: " + ioe.getMessage());
                    }
                }
            }

        } catch (IOException e) {
            System.err.println("ClientHandler IO error: " + e.getMessage());
        } finally {
            closeConnection();
        }
    }

    public void send(String message) {
        if (isRunning) {
            outgoingQueue.offer(message);
        }
    }

    private synchronized void closeConnection() {
        if (isRunning) {
            isRunning = false;
            AuctionServer.removeActiveClient(this);
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                System.err.println("Error closing socket: " + e.getMessage());
            }
        }
    }

    public int getUserId() {
        return userId;
    }

    // Helper to mask long Base64 strings when logging for easier debugging on Server side
    public static String maskImageBase64(String json) {
        if (json == null) return null;
        if (json.length() > 2000) {
            return json.substring(0, 2000) + "... [PAYLOAD TRUNCATED FOR LOGGING TO PREVENT MEMORY LEAK]";
        }
        return json;
    }
}
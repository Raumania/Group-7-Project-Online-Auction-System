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

        UserController userController = new UserController();
        handlers.put(Action.GET_ALL_USERS, userController);
        handlers.put(Action.CREATE_USER, userController);
        handlers.put(Action.UPDATE_USER, userController);
        handlers.put(Action.DELETE_USER, userController);

    }

    @Override
    public void run() {
        AuctionServer.addActiveClient(this);
        // SỬA: Đồng bộ dùng DataInputStream và DataOutputStream như Client
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
             DataOutputStream output = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()))) {

            this.out = output;
            // Dùng vòng lặp vô hạn, readUTF() sẽ quăng lỗi EOFException khi Client đóng kết nối
            while (true) {
                String line;

                try {
                    // SỬA: Đọc bằng readUTF() để khớp với out.writeUTF() của Client
                    //line = in.readUTF();
                    line=readMessage(in);
                } catch (EOFException e) {
                    // Client ngắt kết nối
                    System.out.println("Client disconnected.");
                    break;
                }

                System.out.println("Request: " + maskImageBase64(line));
                Request req = GsonUtil.fromJson(line, Request.class);

                if (req != null && req.getAction() == Action.PING) {
                    Response pongRes = new Response(Status.SUCCESS, Action.PING, null, "PONG");
                    send(GsonUtil.toJson(pongRes));
                    continue;
                }

                RequestHandler handler = handlers.get(req != null ? req.getAction() : null);

                try {
                    Response res;
                    if (handler != null) {
                        res = handler.handle(req);
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
            AuctionServer.removeActiveClient(this);
            try {
                if (socket != null && !socket.isClosed())
                    socket.close();
            } catch (IOException e) {
                System.err.println("Error closing socket: " + e.getMessage());
            }
        }
    }

    public synchronized void send(String message) {
        if (out != null) {
            try {
                writeMessage(out, message);
            } catch (IOException e) {
                System.err.println("Failed to send message to client: " + e.getMessage());
                // Loại bỏ client đã ngắt mạng khỏi danh sách hoạt động và đóng socket
                AuctionServer.removeActiveClient(this);
                try {
                    socket.close();
                } catch (IOException ex) {
                    // ignore
                }
            }
        }
    }

    // Helper ẩn chuỗi Base64 dài khi in log để dễ debug ở phía Server
    public static String maskImageBase64(String json) {
        if (json == null) return null;
        if (json.length() > 2000) {
            return json.substring(0, 2000) + "... [PAYLOAD TRUNCATED FOR LOGGING TO PREVENT MEMORY LEAK]";
        }
        return json;
    }
}
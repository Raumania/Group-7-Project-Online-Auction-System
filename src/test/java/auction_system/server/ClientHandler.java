package auction_system.server;

import auction_system.common.enums.Action;
import auction_system.common.enums.Status;
import auction_system.common.protocol.Request;
import auction_system.common.protocol.Response;
import auction_system.server.controller.*;
import auction_system.server.util.GsonUtil;

import java.io.*;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final Map<Action, RequestHandler> handlers = new ConcurrentHashMap<>();

    public ClientHandler(Socket socket) {
        this.socket = socket;
        initHandlers();
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
        handlers.put(Action.PLACE_BID, new BidController());
        handlers.put(Action.GET_BID_HISTORY,new BidHistoryController());
        //handlers.put(Action.FILTER_CATEGORY)

    }

    @Override
    public void run() {

        // SỬA: Đồng bộ dùng DataInputStream và DataOutputStream như Client
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
             DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()))) {

            // Dùng vòng lặp vô hạn, readUTF() sẽ quăng lỗi EOFException khi Client đóng kết nối
            while (true) {
                String line;

                try {
                    // SỬA: Đọc bằng readUTF() để khớp với out.writeUTF() của Client
                    line = in.readUTF();
                } catch (EOFException e) {
                    // Client ngắt kết nối
                    System.out.println("Client disconnected.");
                    break;
                }

                System.out.println("Request: " + line);
                Request req = GsonUtil.fromJson(line, Request.class);

                RequestHandler handler = handlers.get(req.getAction());

                Response res;

                if (handler != null) {
                    res = handler.handle(req);
                } else {
                    res = new Response(Status.ERROR, "Unknown action: " + req.getAction(), null);
                }
                String jsonResponse = GsonUtil.toJson(res);
                System.out.println("Respond: " + jsonResponse);
                // SỬA: Gửi đi bằng writeUTF() để khớp với in.readUTF() của Client
                out.writeUTF(jsonResponse);
                out.flush();
            }

        } catch (IOException e) {
            System.err.println("ClientHandler IO error: " + e.getMessage());
        } finally {
            try {
                if (socket != null && !socket.isClosed())
                    socket.close();
            } catch (IOException e) {
                System.err.println("Error closing socket: " + e.getMessage());
            }
        }
    }
}
package auction_system.server;

import auction_system.server.common.protocol.*;
import auction_system.server.controller.RequestHandler;
import auction_system.server.controller.LoginController;
import auction_system.server.controller.RegisterController;
import auction_system.server.controller.AuctionController;
import auction_system.server.controller.BidController;
import com.google.gson.Gson;

import java.io.*;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final Gson gson = new Gson();
    private final Map<String, RequestHandler> handlers = new ConcurrentHashMap<>();

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
        handlers.put(Action.PLACE_BID, new BidController());
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

                System.out.println("Received: " + line);
                Request req = gson.fromJson(line, Request.class);

                RequestHandler handler = handlers.get(req.getAction());
                Response res;

                if (handler != null) {
                    res = handler.handle(req);
                } else {
                    res = new Response("ERROR", "type", null, "Unknown action: " + req.getAction());
                }

                String jsonResponse = gson.toJson(res);

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
package auction_system.server;

import com.google.gson.Gson;
import auction_system.server.protocol.*;
import auction_system.server.service.AuctionService;
import auction_system.server.model.*;

import java.io.*;
import java.net.Socket;
import java.util.List;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final Gson gson = new Gson();
    private final AuctionService service;
    private DataInputStream in;

    public ClientHandler(Socket socket) {
        try {
            in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.socket = socket;
        // Dùng Singleton để tránh tạo nhiều instance không cần thiết
        this.service = AuctionService.getInstance();
    }
    @Override
    public void run() {
        // Dùng try-with-resources tự động đóng BufferedReader và PrintWriter
//        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
//             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {
        try {
            System.out.println(in.readUTF());
        } catch (IOException e) {
            e.printStackTrace();
        }


//
//            String line;
//            while ((line = in.readLine()) != null) {
//                Request req = gson.fromJson(line, Request.class);
//                Response res = handle(req);
//                out.println(gson.toJson(res));
//            }
//
//        } catch (IOException e) {
//            System.err.println("ClientHandler IO error: " + e.getMessage());
//        } finally {
//            // Đảm bảo socket luôn được đóng
//            try {
//                if (socket != null && !socket.isClosed()) {
//                    socket.close();
//                }
//            } catch (IOException e) {
//                System.err.println("Error closing socket: " + e.getMessage());
//            }
//        }
//    }
//
//    private Response handle(Request req) {
//        try {
//            switch (req.getAction()) {
//                case Action.CREATE_AUCTION:
//                    CreateAuctionData data = gson.fromJson(req.getData(), CreateAuctionData.class);
//                    Auction auction = service.createAuction(data.getItem(), data.getSeller());
//                    return new Response("SUCCESS", gson.toJson(auction), "Auction created");
//
//                case Action.GET_ALL:
//                    List<Auction> list = service.getAllAuctions();
//                    return new Response("SUCCESS", gson.toJson(list), "List returned");
//
//                default:
//                    return new Response("ERROR", null, "Unknown action");
//            }
//        } catch (Exception e) {
//            return new Response("ERROR", null, e.getMessage());
//        }
    }
}
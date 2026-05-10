package auction_system.client;

import auction_system.server.common.protocol.*;
import com.google.gson.Gson;
import java.io.*;
import java.net.Socket;

public class AuctionClient {
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private final Gson gson = new Gson();

    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        System.out.println("[AuctionClient] Connected to " + host + ":" + port);
    }

    public Response sendRequest(Request req) throws IOException {
        String jsonReq = gson.toJson(req);
        out.writeUTF(jsonReq);
        out.flush();
        String jsonRes = in.readUTF();
        return gson.fromJson(jsonRes, Response.class);
    }

    // ==================== Các phương thức nghiệp vụ ====================

    public Response login(String username, String password) throws IOException {
        LoginData data = new LoginData(username, password);
        String jsonData = gson.toJson(data);
        Request req = new Request(Action.LOGIN, jsonData);
        return sendRequest(req);
    }

    public Response getAllAuctions() throws IOException {
        Request req = new Request(Action.GET_ALL_AUCTIONS, null);
        return sendRequest(req);
    }

    public Response getAuctionDetail(String auctionId) throws IOException {
        String jsonData = gson.toJson(auctionId);
        Request req = new Request(Action.GET_AUCTION_DETAIL, jsonData);
        return sendRequest(req);
    }

    public Response placeBid(String auctionId, double amount, String bidderId) throws IOException {
        BidData data = new BidData(auctionId, amount, bidderId);
        String jsonData = gson.toJson(data);
        Request req = new Request(Action.PLACE_BID, jsonData);
        return sendRequest(req);
    }

    // Phương thức createAuction mới dùng CreateAuctionRequest (không chứa Item)
    public Response createAuction(CreateAuctionRequest request) throws IOException {
        String jsonData = gson.toJson(request);
        Request req = new Request(Action.CREATE_AUCTION, jsonData);
        return sendRequest(req);
    }

    public Response closeAuction(String auctionId) throws IOException {
        String jsonData = gson.toJson(auctionId);
        Request req = new Request(Action.CLOSE_AUCTION, jsonData);
        return sendRequest(req);
    }

    public Response getOpenAuctions() throws IOException {
        Request req = new Request(Action.GET_OPEN_AUCTIONS, null);
        return sendRequest(req);
    }

    public Response getMyItems(String sellerId) throws IOException {
        String jsonData = gson.toJson(sellerId);
        Request req = new Request(Action.GET_SELLER_ITEMS, jsonData);
        return sendRequest(req);
    }

    public void disconnect() throws IOException {
        if (socket != null && !socket.isClosed()) {
            socket.close();
            System.out.println("[AuctionClient] Disconnected");
        }
    }
}
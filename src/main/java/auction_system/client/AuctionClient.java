package auction_system.client;

import auction_system.server.common.protocol.*;
import com.google.gson.Gson;
import java.io.*;
import java.net.Socket;

/**
 * Client kết nối đến Auction Server qua socket, dùng JSON để trao đổi dữ liệu.
 * Các phương thức đều throw IOException để controller xử lý (có thể bọc trong Task).
 */
public class AuctionClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private final Gson gson = new Gson();

    /**
     * Kết nối đến server
     * @param host địa chỉ server (ví dụ "localhost")
     * @param port cổng server (ví dụ 1234)
     */
    public void connect(String host, int port) throws IOException {
        socket = new Socket(host, port);
        out = new PrintWriter(socket.getOutputStream(), true);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        System.out.println("[AuctionClient] Connected to " + host + ":" + port);
    }

    /**
     * Gửi một request và nhận response
     * @param req đối tượng Request cần gửi
     * @return Response từ server
     */
    public Response sendRequest(Request req) throws IOException {
        String jsonReq = gson.toJson(req);
        out.println(jsonReq);
        String jsonRes = in.readLine();
        return gson.fromJson(jsonRes, Response.class);
    }

    // ==================== Các phương thức nghiệp vụ ====================

    /**
     * Đăng nhập
     * @param username tên đăng nhập
     * @param password mật khẩu
     * @return Response chứa kết quả (success/fail, message, user-data nếu thành công)
     */
    public Response login(String username, String password) throws IOException {
        LoginData data = new LoginData(username, password);
        String jsonData = gson.toJson(data);
        Request req = new Request(Action.LOGIN, jsonData);
        return sendRequest(req);
    }

    /**
     * Lấy danh sách tất cả phiên đấu giá
     */
    public Response getAllAuctions() throws IOException {
        Request req = new Request(Action.GET_ALL_AUCTIONS, null);
        return sendRequest(req);
    }

    /**
     * Lấy chi tiết một phiên đấu giá theo ID
     * @param auctionId mã phiên
     */
    public Response getAuctionDetail(String auctionId) throws IOException {
        String jsonData = gson.toJson(auctionId);
        Request req = new Request(Action.GET_AUCTION_DETAIL, jsonData);
        return sendRequest(req);
    }

    /**
     * Đặt giá
     * @param auctionId mã phiên
     * @param amount số tiền
     * @param bidderId mã người đặt
     */
    public Response placeBid(String auctionId, double amount, String bidderId) throws IOException {
        BidData data = new BidData(auctionId, amount, bidderId);
        String jsonData = gson.toJson(data);
        Request req = new Request(Action.PLACE_BID, jsonData);
        return sendRequest(req);
    }

    /**
     * Tạo phiên đấu giá mới (dành cho Seller)
     * @param item sản phẩm đã được tạo (có owner là seller)
     * @param seller người bán
     */
    public Response createAuction(auction_system.server.model.Item item, auction_system.server.model.Seller seller) throws IOException {
        CreateAuctionData data = new CreateAuctionData(item, seller);
        String jsonData = gson.toJson(data);
        Request req = new Request(Action.CREATE_AUCTION, jsonData);
        return sendRequest(req);
    }

    /**
     * Đóng phiên đấu giá (Seller hoặc Admin)
     * @param auctionId mã phiên
     */
    public Response closeAuction(String auctionId) throws IOException {
        String jsonData = gson.toJson(auctionId);
        Request req = new Request(Action.CLOSE_AUCTION, jsonData);
        return sendRequest(req);
    }

    /**
     * Lấy danh sách các phiên đang mở (OPEN hoặc RUNNING)
     */
    public Response getOpenAuctions() throws IOException {
        Request req = new Request(Action.GET_OPEN_AUCTIONS, null);
        return sendRequest(req);
    }

    /**
     * Lấy danh sách sản phẩm của Seller (theo sellerId)
     */
    public Response getMyItems(String sellerId) throws IOException {
        String jsonData = gson.toJson(sellerId);
        Request req = new Request(Action.GET_SELLER_ITEMS, jsonData);
        return sendRequest(req);
    }

    /**
     * Ngắt kết nối (đóng socket)
     */
    public void disconnect() throws IOException {
        if (socket != null && !socket.isClosed()) {
            socket.close();
            System.out.println("[AuctionClient] Disconnected");
        }
    }
}
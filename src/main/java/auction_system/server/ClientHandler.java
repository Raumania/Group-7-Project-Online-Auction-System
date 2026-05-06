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
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ClientHandler implements Runnable {

    // Socket kết nối với 1 client cụ thể
    private final Socket socket;

    // Dùng để parse JSON <-> Object
    private final Gson gson = new Gson();

    // Map action → handler tương ứng
    // ConcurrentHashMap để thread-safe
    private final Map<String, RequestHandler> handlers = new ConcurrentHashMap<>();

    public ClientHandler(Socket socket) {
        this.socket = socket;

        // Khởi tạo các controller xử lý request
        initHandlers();
    }

    private void initHandlers() {

        // LOGIN → LoginController
        handlers.put(Action.LOGIN, new LoginController());

        // Dùng chung 1 instance AuctionController cho nhiều action
        AuctionController auctionController = new AuctionController();

        // REGISTER → RegisterController
        handlers.put(Action.REGISTER, new RegisterController());

        // Các action liên quan auction
        handlers.put(Action.GET_ALL_AUCTIONS, auctionController);
        handlers.put(Action.GET_AUCTION_DETAIL, auctionController);
        handlers.put(Action.CREATE_AUCTION, auctionController);
        handlers.put(Action.CLOSE_AUCTION, auctionController);

        // Đặt giá
        handlers.put(Action.PLACE_BID, new BidController());
    }

    @Override
    public void run() {

        // Mở luồng đọc/ghi với client
        // Thay thế PrintWriter bằng DataOutputStream
        try (BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             DataOutputStream out = new DataOutputStream(socket.getOutputStream())) {

            String line;

            // Đọc dữ liệu từ client từng dòng (JSON string)
            while ((line = in.readLine()) != null) {
                // Convert JSON → Request object
                System.out.println(line);
                Request req = gson.fromJson(line, Request.class);

                // Lấy handler tương ứng với action
                RequestHandler handler = handlers.get(req.getAction());

                Response res;

                if (handler != null) {

                    // Gọi controller xử lý
                    res = handler.handle(req);

                } else {

                    // Nếu không có handler
                    res = new Response(
                            "ERROR",
                            "type",
                            null,
                            "Unknown action: " + req.getAction()
                    );
                }

                // Convert Response → JSON
                String jsonResponse = gson.toJson(res);

                // Ghi ra luồng DataOutputStream kèm ký tự xuống dòng "\n"
                // để phía client vẫn có thể đọc bằng readLine()
                out.write((jsonResponse + "\n").getBytes(StandardCharsets.UTF_8));

                // Bắt buộc phải flush() để đẩy dữ liệu qua mạng ngay lập tức
                out.flush();
            }

        } catch (IOException e) {

            // Lỗi IO (client disconnect, mạng lỗi...)
            System.err.println("ClientHandler IO error: " + e.getMessage());

        } finally {

            // Đóng socket khi client ngắt
            try {
                if (socket != null && !socket.isClosed())
                    socket.close();
            } catch (IOException e) {
                System.err.println("Error closing socket: " + e.getMessage());
            }
        }
    }
}
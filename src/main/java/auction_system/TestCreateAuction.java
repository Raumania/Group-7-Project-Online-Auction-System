/*
package auction_system;

import auction_system.client.AuctionClient;
import auction_system.server.common.protocol.CreateAuctionRequest;
import auction_system.server.common.protocol.Request;
import auction_system.server.common.protocol.Response;
import com.google.gson.Gson;

public class TestCreateAuction {
    public static void main(String[] args) {
        AuctionClient client = new AuctionClient();
        Gson gson = new Gson();

        try {
            // 1. Kết nối (đổi cổng cho đúng với server của bạn)
            System.out.println("Đang kết nối tới server...");
            client.connect("localhost", 3636);  // nếu server chạy cổng 1234
            System.out.println("Kết nối thành công!");

            // 2. Tạo dữ liệu request
            CreateAuctionRequest req = new CreateAuctionRequest();
            req.setSellerId("SELLER _ 255aabf9-e5a8-4075-bd20-eff8bf59dcd9");   // ⚠️ thay bằng ID seller thật từ DB
            req.setItemType("ELECTRONICS");
            req.setName("Laptop Dell XPS");
            req.setDescription("Laptop cao cấp");
            req.setStartingPrice(1000.0);
            req.setBrand("Dell");
            req.setModel("XPS 15");

            String jsonData = gson.toJson(req);
            Request request = new Request("CREATE_AUCTION", jsonData);

            // 3. Gửi request và nhận response
            System.out.println("Đang gửi request...");
            Response response = client.sendRequest(request);
            System.out.println("=== KẾT QUẢ ===");
            System.out.println("Status : " + response.getStatus());
            System.out.println("Message: " + response.getMessage());
            System.out.println("Data   : " + response.getData());

        } catch (java.net.SocketTimeoutException e) {
            System.err.println("LỖI: Timeout – server không phản hồi. Kiểm tra server đã chạy và cổng đúng.");
        } catch (Exception e) {
            System.err.println("LỖI: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // 4. Ngắt kết nối
            try {
                client.disconnect();
                System.out.println("Đã ngắt kết nối.");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}*/

package auction_system.server;//package auction_system.server;
//
//import auction_system.common.enums.Action;
//import auction_system.common.enums.ItemType;
//import auction_system.common.protocol.Request;
//import auction_system.server.util.GsonUtil;
//import com.google.gson.JsonElement;
//
//import java.io.*;
//import java.net.Socket;
//import java.time.LocalDateTime;
//
//public class TestCreateAuction {
//
//    public static void main(String[] args) {
//        String serverAddress = "127.0.0.1";
//        int port = 3636;
//
//        System.out.println("========== TEST CLIENT: TẠO AUCTION (PHIÊN BẢN TỐI GIẢN) ==========");
//
//        try (Socket socket = new Socket(serverAddress, port);
//             DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
//             DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()))) {
//
//            System.out.println("[CLIENT] Đã kết nối tới Server " + serverAddress + ":" + port);
//
//            // 1. Chuẩn bị dữ liệu payload
//            CreateAuctionRequest payload = new CreateAuctionRequest();
//
//            payload.setSellerId("1"); // ID của User có role SELLER
//            payload.setItemType(ItemType.VEHICLE);
//            payload.setName("Xe máy SYM Galaxy 50cc");
//            payload.setDescription("Xe còn chạy tốt, phù hợp đi phượt nhẹ nhàng quanh Hà Nội.");
//            payload.setStartingPrice(8000.0);
//
//            // CẬP NHẬT: Thêm thời gian bắt đầu và kết thúc
//            payload.setStartTime(LocalDateTime.now());
//            payload.setEndTime(LocalDateTime.now().plusDays(3)); // Đấu giá trong 3 ngày
//
//            /*
//               LƯU Ý: Đã loại bỏ setBrand, setYear... vì model và DTO đã tối giản.
//            */
//
//            // 2. Ép payload thành JsonElement
//            JsonElement dataElement = GsonUtil.getGson().toJsonTree(payload);
//
//            // 3. Bọc vào Request
//            Request request = new Request(Action.CREATE_AUCTION, dataElement);
//
//            // 4. Chuyển Request thành chuỗi JSON
//            String jsonRequest = GsonUtil.toJson(request);
//
//            System.out.println("[CLIENT] Đang gửi JSON tới Server: ");
//            System.out.println(jsonRequest);
//
//            // 5. Gửi qua socket
//            out.writeUTF(jsonRequest);
//            out.flush();
//
//            // 6. Đọc phản hồi
//            System.out.println("\n[CLIENT] Đang chờ Server phản hồi...");
//            String jsonResponse = in.readUTF();
//
//            System.out.println("[CLIENT] Kết quả Server trả về: ");
//            System.out.println(jsonResponse);
//
//        } catch (Exception e) {
//            System.err.println("[CLIENT] Lỗi: " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//}
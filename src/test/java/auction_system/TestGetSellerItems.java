package auction_system;//package auction_system.test;
//
//import auction_system.common.enums.Action;
//import auction_system.common.enums.Status;
//import auction_system.common.protocol.Request;
//import auction_system.common.protocol.Response;
//import auction_system.common.dto.AuctionDTO;
//
//import com.google.gson.Gson;
//import com.google.gson.JsonPrimitive;
//import com.google.gson.reflect.TypeToken;
//
//import java.io.DataInputStream;
//import java.io.DataOutputStream;
//import java.net.Socket;
//import java.lang.reflect.Type;
//import java.util.List;
//
//public class TestGetSellerItems {
//
//    private static final String SERVER_IP = "localhost";
//    private static final int SERVER_PORT = 3636;
//
//    public static void main(String[] args) {
//        System.out.println("=== BẮT ĐẦU TEST KẾT NỐI SERVER (DataOutputStream) ===");
//
//        Gson gson = new Gson();
//
//        // Thay đổi sang DataOutputStream và DataInputStream
//        try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
//             DataOutputStream out = new DataOutputStream(socket.getOutputStream());
//             DataInputStream in = new DataInputStream(socket.getInputStream())) {
//
//            System.out.println("✅ Đã kết nối tới Server thành công!");
//
//            // ==========================================
//            // 1. TẠO REQUEST ĐỂ GỬI ĐI
//            // ==========================================
//            int testSellerId = 26;
//            JsonPrimitive payloadData = new JsonPrimitive(testSellerId);
//            Request request = new Request(Action.GET_SELLER_ITEMS, payloadData);
//
//            String jsonRequest = gson.toJson(request);
//
//            System.out.println("📤 Đang gửi Request: " + jsonRequest);
//
//            // Gửi chuỗi JSON bằng phương thức writeUTF
//            out.writeUTF(jsonRequest);
//            out.flush(); // Đẩy dữ liệu đi ngay lập tức
//
//            // ==========================================
//            // 2. NHẬN VÀ XỬ LÝ RESPONSE TỪ SERVER
//            // ==========================================
//            System.out.println("⏳ Đang chờ phản hồi từ Server...");
//
//            // Đọc chuỗi JSON trả về bằng phương thức readUTF
//            String jsonResponse = in.readUTF();
//
//            if (jsonResponse != null && !jsonResponse.isEmpty()) {
//                System.out.println("📥 Đã nhận Response: " + jsonResponse);
//
//                Response response = gson.fromJson(jsonResponse, Response.class);
//
//                if (response.getStatus() == Status.SUCCESS) {
//                    System.out.println("✅ Server xử lý thành công!");
//
//                    String dataJsonString = gson.toJson(response.getData());
//                    Type listType = new TypeToken<List<AuctionDTO>>(){}.getType();
//                    List<AuctionDTO> auctions = gson.fromJson(dataJsonString, listType);
//
//                    System.out.println("📦 Lấy được " + auctions.size() + " sản phẩm từ Seller ID " + testSellerId + ":");
//                    for (AuctionDTO dto : auctions) {
//                        System.out.println("  ▶ [ID: " + dto.getId() + "] " + dto.getName() + " - Giá: $" + dto.getStartingPrice());
//                    }
//                } else {
//                    System.err.println("❌ Server báo lỗi: " + response.getMessage());
//                }
//            } else {
//                System.err.println("❌ Không nhận được dữ liệu từ Server.");
//            }
//
//        } catch (Exception e) {
//            System.err.println("❌ Lỗi kết nối mạng! Kiểm tra xem Server đã chạy và khớp Port chưa.");
//            e.printStackTrace();
//        }
//
//        System.out.println("=== KẾT THÚC TEST ===");
//    }
//}
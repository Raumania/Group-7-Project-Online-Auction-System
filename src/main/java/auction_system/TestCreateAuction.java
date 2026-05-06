package auction_system;

import auction_system.server.common.protocol.*;
import auction_system.server.common.protocol.CreateAuctionData;
import auction_system.server.model.*;
import com.google.gson.Gson;
import java.io.*;
import java.net.Socket;

public class TestCreateAuction {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 1234;
        Gson gson = new Gson();

        // 1. Tạo đối tượng Item (Electronics) trước
        // Giả sử bạn đã có Seller (cần có id, username, ...)
        // Nếu chưa có seller, bạn cần tạo seller qua UserService hoặc tạo sẵn trong DB.
        // Ở đây ta tạo seller trực tiếp (chỉ để test, nhưng lý tưởng nên lấy từ DB)
        Seller seller = new Seller("seller1", "123", "seller@ex.com"); // giả sử constructor

        // Tạo Electronics
        Electronics laptop = new Electronics(
                "Laptop Dell XPS",
                "Laptop cao cấp",
                1000.0,
                seller,
                "Dell",
                "XPS 15"
        );

        // 2. Tạo CreateAuctionData
        CreateAuctionData auctionData = new CreateAuctionData(laptop, seller);
        String jsonData = gson.toJson(auctionData);

        // 3. Tạo Request với action CREATE_AUCTION
        Request request = new Request(Action.CREATE_AUCTION, jsonData);
        String jsonRequest = gson.toJson(request);

        try (Socket socket = new Socket(host, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            System.out.println("Sending request: " + jsonRequest);
            out.println(jsonRequest);
            String responseLine = in.readLine();
            System.out.println("Response: " + responseLine);

            Response response = gson.fromJson(responseLine, Response.class);
            if ("SUCCESS".equals(response.getStatus())) {
                System.out.println("Auction created successfully. Data: " + response.getData());
            } else {
                System.out.println("Create auction failed: " + response.getMessage());
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
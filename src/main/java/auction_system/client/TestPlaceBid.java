package auction_system.client;

import auction_system.server.common.protocol.Action;
import auction_system.server.common.protocol.BidData;
import auction_system.server.common.protocol.Request;
import auction_system.server.model.User;
import auction_system.server.service.UserService;
import com.google.gson.Gson;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

public class TestPlaceBid {
    public static void main(String[] args) throws Exception {
        String host = "localhost";
        int port = 3636;

        Gson gson = new Gson();

        /*
            Điều kiện trước khi chạy test này:

            1. Server AuctionServer đang chạy.
            2. Database đã có user bidder.
               Ví dụ bidderId = "1" hoặc "2".
            3. User đó phải có role BIDDER.
               Với hệ thống mới của bạn, user mặc định có BIDDER,SELLER nên ok.
            4. Database đã có auction.
               Ví dụ auctionId lấy từ bảng auctions.
            5. User bidder phải có tiền trong balance.
        */
        UserService userService=new UserService();
        User user= userService.getUserById("1");
        userService.deposit("1",1000000);
        BidData data = new BidData(
                "AUCTION _ 570a2731-c80a-4bc0-9285-a8443e001ec2",  // auctionId
                1500,    // amount
                "1"      // bidderId
        );

        Request req = new Request(
                Action.PLACE_BID,
                gson.toJsonTree(data)
        );

        String jsonRequest = gson.toJson(req);

        System.out.println("Connecting to server...");

        try (Socket socket = new Socket(host, port);
             DataOutputStream out = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
             DataInputStream in = new DataInputStream(new BufferedInputStream(socket.getInputStream()))) {

            System.out.println("Connected");

            System.out.println("Sending:");
            System.out.println(jsonRequest);

            out.writeUTF(jsonRequest);
            out.flush();

            System.out.println("Waiting response...");

            String responseJson = in.readUTF();

            System.out.println("Response:");
            System.out.println(responseJson);
        }
    }
}
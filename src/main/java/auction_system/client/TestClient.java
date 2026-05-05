package auction_system.client;

import com.google.gson.Gson;
import auction_system.server.protocol.*;
import auction_system.server.model.*;

import java.io.*;
import java.net.Socket;

public class TestClient {
    public static void main(String[] args) throws Exception {

        Socket socket = new Socket("localhost", 1234);

        BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(
                socket.getOutputStream(), true);

        Gson gson = new Gson();

        // fake data
        Seller seller = new Seller("s1","1234","hi");
        ItemType type = ItemType.ELECTRONICS;
        Electronics item = new Electronics("e1", "hi", 3.4, seller, "dffd","dsfd");

        CreateAuctionData data = new CreateAuctionData();
        data.setItem(item);
        data.setSeller(seller);

        Request req = new Request(
                Action.CREATE_AUCTION,
                gson.toJson(data)
        );

        out.println(gson.toJson(req));

        String resStr = in.readLine();
        System.out.println(resStr);
    }
}
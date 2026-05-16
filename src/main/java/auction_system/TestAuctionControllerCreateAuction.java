package auction_system;

import auction_system.server.common.protocol.Action;
import auction_system.server.common.protocol.CreateAuctionRequest;
import auction_system.server.common.protocol.Request;
import auction_system.server.common.protocol.Response;
import auction_system.server.controller.AuctionController;
import auction_system.server.model.ItemType;
import auction_system.server.util.GsonUtil;
import com.google.gson.JsonElement;


import java.time.LocalDateTime;

public class TestAuctionControllerCreateAuction {
    public static void main(String[] args) {
        AuctionController auctionController = new AuctionController();

        /*
            Điều kiện trước khi chạy:
            1. Database đã có user id = 26.
            2. User đó phải có role SELLER.
            3. AuctionController đang dùng UserService, ItemService, AuctionService thật.
        */

        CreateAuctionRequest createAuctionRequest = new CreateAuctionRequest();

        createAuctionRequest.setSellerId("26");
        createAuctionRequest.setName("t");
        createAuctionRequest.setDescription("1");
        createAuctionRequest.setItemType(ItemType.ELECTRONICS);
        createAuctionRequest.setStartingPrice(1.0);
        createAuctionRequest.setStartTime(LocalDateTime.of(2026, 5, 17, 12, 22));
        createAuctionRequest.setEndTime(LocalDateTime.of(2026, 5, 27, 12, 22));

        JsonElement dataElement = GsonUtil.getGson().toJsonTree(createAuctionRequest);
        Request request = new Request(
                Action.CREATE_AUCTION,
                dataElement
        );
        Response response = auctionController.handle(request);

        System.out.println("Respond:");
        System.out.println(GsonUtil.toJson(response));
    }
}
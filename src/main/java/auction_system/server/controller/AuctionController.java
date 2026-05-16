package auction_system.server.controller;

import auction_system.server.common.protocol.Action;
import auction_system.server.common.protocol.CreateAuctionRequest;
import auction_system.server.common.protocol.Request;
import auction_system.server.common.protocol.Response;
import auction_system.server.model.Auction;
import auction_system.server.model.Item;
import auction_system.server.model.ItemType;
import auction_system.server.model.User;
import auction_system.server.service.AuctionService;
import auction_system.server.service.ItemService;
import auction_system.server.service.UserService;
import auction_system.server.util.GsonUtil;
import com.google.gson.JsonElement;

import java.time.LocalDateTime;
import java.util.List;

public class AuctionController implements RequestHandler {

    private final AuctionService auctionService = AuctionService.getInstance();
    private final UserService userService = new UserService();
    private final ItemService itemService = new ItemService();

    @Override
    public Response handle(Request request) {
        String action = request.getAction();

        try {
            switch (action) {
                case Action.GET_ALL_AUCTIONS:
                    return getAllAuctions();
                case Action.GET_AUCTION_DETAIL:
                    return getAuctionDetail(request.getData());
                case Action.CREATE_AUCTION:
                    return createAuction(request.getData());
                case Action.CLOSE_AUCTION:
                    return closeAuction(request.getData());
                default:
                    return new Response("ERROR", "AUCTION", null, "Unknown action: " + action);
            }
        } catch (Exception e) {
            return new Response("ERROR", "AUCTION", null, e.getMessage());
        }
    }

    private Response getAllAuctions() {
        List<Auction> auctions = auctionService.getAllAuctions();
        return new Response("SUCCESS", "AUCTION", auctions, "List returned");
    }

    private Response getAuctionDetail(JsonElement data) {
        String auctionId = GsonUtil.fromJson(data, String.class);
        Auction auction = auctionService.getAuctionById(auctionId);
        return new Response("SUCCESS", "AUCTION", auction, "Auction detail");
    }

    private Response createAuction(JsonElement data) {
        try {
            CreateAuctionRequest req = GsonUtil.fromJson(data, CreateAuctionRequest.class);
            User seller = userService.getSellerById(req.getSellerId());

            if (seller == null) {
                return new Response("ERROR", "AUCTION", null, "Seller not found");
            }
            System.out.println(req.getItemType());
            Item item = null;
            ItemType type = req.getItemType();
            LocalDateTime startTime = req.getStartTime();
            LocalDateTime endTime = req.getEndTime();
            double startingPrice = req.getStartingPrice();

            if (type == null) {
                return new Response("ERROR", "AUCTION", null, "Missing item type");
            }

            switch (type) {
                case ELECTRONICS:
                    item = itemService.createElectronics(
                            req.getName(),
                            req.getDescription(),
                            seller,
                            req.getStartTime(), // Thêm từ Request
                            req.getEndTime()    // Thêm từ Request
                    );
                    break;

                case ART:
                    item = itemService.createArt(
                            req.getName(),
                            req.getDescription(),
                            req.getStartTime(),
                            req.getEndTime()
                    );
                    break;

                case VEHICLE:
                    item = itemService.createVehicle(
                            req.getName(),
                            req.getDescription(),
                            req.getStartTime(),
                            req.getEndTime()
                    );
                    break;

                default:
                    return new Response("ERROR", "AUCTION", null, "Unsupported item type: " + type);
            }

            // Tạo phiên đấu giá với startingPrice
            Auction auction = auctionService.createAuction(item, seller,startingPrice,startTime,endTime);

            return new Response("SUCCESS", "AUCTION", auction, "Auction created");

        } catch (Exception e) {
            e.printStackTrace();
            return new Response("ERROR", "AUCTION", null, "Create auction failed: " + e.getMessage());
        }
    }

    private Response closeAuction(JsonElement data) {
        String auctionId = GsonUtil.fromJson(data, String.class);
        auctionService.closeAuction(auctionId);
        return new Response("SUCCESS", "AUCTION", null, "Auction closed");
    }

}
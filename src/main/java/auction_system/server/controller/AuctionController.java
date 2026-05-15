package auction_system.server.controller;

import auction_system.server.common.protocol.Action;
import auction_system.server.common.protocol.CreateAuctionRequest;
import auction_system.server.common.protocol.Request;
import auction_system.server.common.protocol.Response;
import auction_system.server.model.Auction;
import auction_system.server.model.Item;
import auction_system.server.model.User;
import auction_system.server.service.AuctionService;
import auction_system.server.service.ItemService;
import auction_system.server.service.UserService;
import auction_system.server.util.GsonUtil;
import com.google.gson.JsonElement;

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

    private Response getAuctionDetail(Object dataObj) {
        String auctionId = parseDataAsString(dataObj);
        Auction auction = auctionService.getAuctionById(auctionId);
        return new Response("SUCCESS", "AUCTION", auction, "Auction detail");
    }

    private Response createAuction(Object dataObj) {
        try {
            CreateAuctionRequest req = parseData(dataObj, CreateAuctionRequest.class);
            User seller = userService.getSellerById(req.getSellerId());

            if (seller == null) {
                return new Response("ERROR", "AUCTION", null, "Seller not found");
            }

            Item item = null;
            String type = req.getItemType();

            if (type == null) {
                return new Response("ERROR", "AUCTION", null, "Missing item type");
            }

            /*
                CẬP NHẬT: Tất cả các loại Item giờ chỉ dùng chung bộ tham số:
                name, description, seller, startingTime, endingTime.
            */
            switch (type) {
                case "ELECTRONICS":
                    item = itemService.createElectronics(
                            req.getName(),
                            req.getDescription(),
                            seller,
                            req.getStartingTime(), // Thêm từ Request
                            req.getEndingTime()    // Thêm từ Request
                    );
                    break;

                case "ART":
                    item = itemService.createArt(
                            req.getName(),
                            req.getDescription(),
                            seller,
                            req.getStartingTime(),
                            req.getEndingTime()
                    );
                    break;

                case "VEHICLE":
                    item = itemService.createVehicle(
                            req.getName(),
                            req.getDescription(),
                            seller,
                            req.getStartingTime(),
                            req.getEndingTime()
                    );
                    break;

                default:
                    return new Response("ERROR", "AUCTION", null, "Unsupported item type: " + type);
            }

            // Tạo phiên đấu giá với startingPrice
            Auction auction = auctionService.createAuction(item, seller, req.getStartingPrice(),req.getStartingTime(),req.getEndingTime());

            return new Response("SUCCESS", "AUCTION", auction, "Auction created");

        } catch (Exception e) {
            e.printStackTrace();
            return new Response("ERROR", "AUCTION", null, "Create auction failed: " + e.getMessage());
        }
    }

    private Response closeAuction(Object dataObj) {
        String auctionId = parseDataAsString(dataObj);
        auctionService.closeAuction(auctionId);
        return new Response("SUCCESS", "AUCTION", null, "Auction closed");
    }

    private String parseDataAsString(Object dataObj) {
        if (dataObj instanceof String) {
            return (String) dataObj;
        } else if (dataObj instanceof JsonElement) {
            return GsonUtil.fromJson((JsonElement) dataObj, String.class);
        } else {
            String json = GsonUtil.toJson(dataObj);
            return GsonUtil.fromJson(json, String.class);
        }
    }

    private <T> T parseData(Object dataObj, Class<T> clazz) {
        if (dataObj instanceof JsonElement) {
            return GsonUtil.fromJson((JsonElement) dataObj, clazz);
        } else {
            String json = GsonUtil.toJson(dataObj);
            return GsonUtil.fromJson(json, clazz);
        }
    }
}
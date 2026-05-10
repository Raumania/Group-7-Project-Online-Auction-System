package auction_system.server.controller;

import auction_system.server.common.protocol.*;
import auction_system.server.model.Auction;
import auction_system.server.model.Item;
import auction_system.server.model.Seller;
import auction_system.server.service.AuctionService;
import auction_system.server.service.ItemService;
import auction_system.server.service.UserService;
import com.google.gson.Gson;

import java.util.List;

public class AuctionController implements RequestHandler {

    private final Gson gson = new Gson();
    private final AuctionService auctionService = AuctionService.getInstance();
    private final UserService userService = new UserService();
    private final ItemService itemService = new ItemService();

    @Override
    public Response handle(Request request) {
        String action = request.getAction();
        String jsonData = request.getData();

        try {
            switch (action) {
                case Action.GET_ALL_AUCTIONS:
                    return getAllAuctions();
                case Action.GET_AUCTION_DETAIL:
                    return getAuctionDetail(jsonData);
                case Action.CREATE_AUCTION:
                    return createAuction(jsonData);
                case Action.CLOSE_AUCTION:
                    return closeAuction(jsonData);
                default:
                    return new Response("ERROR", "AUCTION", null, "Unknown action: " + action);
            }
        } catch (Exception e) {
            return new Response("ERROR", "AUCTION", null, e.getMessage());
        }
    }

    private Response getAllAuctions() {
        List<Auction> auctions = auctionService.getAllAuctions();
        return new Response("SUCCESS", "AUCTION", gson.toJson(auctions), "List returned");
    }

    private Response getAuctionDetail(String jsonData) {
        String auctionId = gson.fromJson(jsonData, String.class);
        Auction auction = auctionService.getAuctionById(auctionId);
        return new Response("SUCCESS", "AUCTION", gson.toJson(auction), "Auction detail");
    }

    private Response createAuction(String jsonData) {
        try {
            // 1. Parse request
            CreateAuctionRequest req = gson.fromJson(jsonData, CreateAuctionRequest.class);

            // 2. Lấy seller
            Seller seller = userService.getSellerById(req.getSellerId());
            if (seller == null) {
                return new Response("ERROR", "AUCTION", null, "Seller not found");
            }

            // 3. Tạo Item dựa trên itemType
            Item item = null;
            String type = req.getItemType();
            if (type == null) {
                return new Response("ERROR", "AUCTION", null, "Missing item type");
            }

            switch (type) {
                case "ELECTRONICS":
                    item = itemService.createElectronics(
                            req.getName(), req.getDescription(), req.getStartingPrice(),
                            seller, req.getBrand(), req.getModel()
                    );
                    break;
                case "ART":
                    item = itemService.createArt(
                            req.getName(), req.getDescription(), req.getStartingPrice(),
                            seller, req.getArtist(), req.getYear()
                    );
                    break;
                case "VEHICLE":
                    item = itemService.createVehicle(
                            req.getName(), req.getDescription(), req.getStartingPrice(),
                            seller, req.getBrand(), req.getYear()
                    );
                    break;
                default:
                    return new Response("ERROR", "AUCTION", null, "Unsupported item type: " + type);
            }

            // 4. Tạo auction
            Auction auction = auctionService.createAuction(item, seller);
            return new Response("SUCCESS", "AUCTION", gson.toJson(auction), "Auction created");
        } catch (Exception e) {
            e.printStackTrace();
            return new Response("ERROR", "AUCTION", null, "Create auction failed: " + e.getMessage());
        }
    }

    private Response closeAuction(String jsonData) {
        String auctionId = gson.fromJson(jsonData, String.class);
        auctionService.closeAuction(auctionId);
        return new Response("SUCCESS", "AUCTION", null, "Auction closed");
    }
}
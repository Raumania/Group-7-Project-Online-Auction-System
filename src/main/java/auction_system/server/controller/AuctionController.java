package auction_system.server.controller;

import auction_system.server.common.protocol.*;
import auction_system.server.model.Auction;
import auction_system.server.service.AuctionService;
import com.google.gson.Gson;
import java.util.List;

/**
 * Xử lý các action:
 * - GET_ALL_AUCTIONS
 * - GET_AUCTION_DETAIL
 * - CREATE_AUCTION
 * - CLOSE_AUCTION
 */
public class AuctionController implements RequestHandler {

    private final Gson gson = new Gson();
    private final AuctionService auctionService = AuctionService.getInstance();

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
                    return new Response("ERROR", null, "Unknown auction action: " + action);
            }
        } catch (Exception e) {
            return new Response("ERROR", null, e.getMessage());
        }
    }

    private Response getAllAuctions() {
        List<Auction> auctions = auctionService.getAllAuctions();
        return new Response("SUCCESS", gson.toJson(auctions), "List returned");
    }

    private Response getAuctionDetail(String jsonData) {
        String auctionId = gson.fromJson(jsonData, String.class);
        Auction auction = auctionService.getAuctionById(auctionId);
        return new Response("SUCCESS", gson.toJson(auction), "Auction detail");
    }

    private Response createAuction(String jsonData) {
        CreateAuctionData data = gson.fromJson(jsonData, CreateAuctionData.class);
        Auction auction = auctionService.createAuction(data.getItem(), data.getSeller());
        return new Response("SUCCESS", gson.toJson(auction), "Auction created");
    }

    private Response closeAuction(String jsonData) {
        String auctionId = gson.fromJson(jsonData, String.class);
        auctionService.closeAuction(auctionId);
        return new Response("SUCCESS", null, "Auction closed");
    }
}
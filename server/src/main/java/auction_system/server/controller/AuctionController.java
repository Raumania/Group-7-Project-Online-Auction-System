package auction_system.server.controller;

import auction_system.common.dto.AuctionDTO;
import auction_system.common.enums.Action;
import auction_system.common.enums.Status;
import auction_system.common.protocol.Request;
import auction_system.common.protocol.Response;
import auction_system.server.model.Auction;
import auction_system.server.service.AuctionService;
import auction_system.server.util.GsonUtil;
import com.google.gson.JsonElement;

import java.util.List;

public class AuctionController implements RequestHandler {

    private final AuctionService auctionService = AuctionService.getInstance();

    @Override
    public Response handle(Request request) {
        Action action = request.getAction();

        try {
            switch (action) {
                case GET_ALL_AUCTIONS:
                    return getAllAuctions(action);
                case GET_AUCTION_DETAIL:
                    return getAuctionDetail(action, request.getData());
                case GET_SELLER_ITEMS:
                    return getSellerItems(action, request.getData());
                case CREATE_AUCTION:
                    return createAuction(action, request.getData());
                case EDIT_AUCTION:
                    return editAuction(action, request.getData());
                case DELETE_AUCTION:
                    return deleteAuction(action, request.getData());
                case CLOSE_AUCTION:
                    return closeAuction(action, request.getData());
                default:
                    return new Response(Status.ERROR, action, null, "Unknown action: " + action);
            }
        } catch (Exception e) {
            return new Response(Status.ERROR, action, null, e.getMessage());
        }
    }

    private Response getAllAuctions(Action action) {
        List<Auction> auctions = auctionService.getAllAuctions();
        return new Response(Status.SUCCESS, action, auctions, "List returned");
    }

    private Response getSellerItems(Action action, JsonElement data) {
        try {
            int sellerId = GsonUtil.fromJson(data, Integer.class);
            List<Auction> auctions = auctionService.getMyAuctions(sellerId);
            return new Response(Status.SUCCESS, action, auctions, "Seller Items List returned");
        } catch (Exception e){
            System.err.println(e.getMessage());
            return new Response(Status.ERROR, action, null, e.getMessage());
        }
    }

    private Response getAuctionDetail(Action action, JsonElement data) {
        int auctionId = GsonUtil.fromJson(data, Integer.class);
        Auction auction = auctionService.getAuctionById(auctionId);
        return new Response(Status.SUCCESS, action, auction, "Auction detail");
    }

    private Response createAuction(Action action, JsonElement data) {
        try {
            AuctionDTO auctionDTO = GsonUtil.fromJson(data, AuctionDTO.class);

            Auction auction = new Auction(auctionDTO);
            auctionService.createAuction(auction);
            return new Response(Status.SUCCESS, action, auction, "Auction created");
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(Status.ERROR, action, null, "Create auction failed: " + e.getMessage());
        }
    }

    private Response editAuction(Action action, JsonElement data) {
        try {
            AuctionDTO auctionDTO = GsonUtil.fromJson(data, AuctionDTO.class);
            Auction auction = new Auction(auctionDTO);
            auctionService.editAuction(auction);
            return new Response(Status.SUCCESS, action, auction, "Auction edited");
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(Status.ERROR, action, null, "Edit auction failed: " + e.getMessage());
        }
    }

    private Response deleteAuction(Action action, JsonElement data) {
        try {
            int auctionId = GsonUtil.fromJson(data, Integer.class);
            auctionService.deleteAuction(auctionId);
            return new Response(Status.SUCCESS, action, null, "Auction deleted");
        } catch (Exception e) {
            e.printStackTrace();
            return new Response(Status.ERROR, action, null, "Delete auction failed: " + e.getMessage());
        }
    }

    private Response closeAuction(Action action, JsonElement data) {
        int auctionId = GsonUtil.fromJson(data, Integer.class);
        auctionService.closeAuction(auctionId);
        return new Response(Status.SUCCESS, action, null, "Auction closed");
    }
}
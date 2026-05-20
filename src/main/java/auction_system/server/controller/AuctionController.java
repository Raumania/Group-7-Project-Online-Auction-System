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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class AuctionController implements RequestHandler {
    Logger logger = LoggerFactory.getLogger(AuctionController.class);

    private final AuctionService auctionService = AuctionService.getInstance();

    @Override
    public Response handle(Request request) {
        Action action = request.getAction();

        try {
            switch (action) {
                case GET_ALL_AUCTIONS:
                    return getAllAuctions();
                case GET_AUCTION_DETAIL:
                    return getAuctionDetail(request.getData());
                case GET_SELLER_ITEMS:
                    return getSellerItems(request.getData());
                case CREATE_AUCTION:
                    return createAuction(request.getData());
                case EDIT_AUCTION:
                    return editAuction(request.getData());
                case DELETE_AUCTION:
                    return deleteAuction(request.getData());
                case CLOSE_AUCTION:
                    return closeAuction(request.getData());
                default:
                    return new Response(Status.ERROR, "Unknown action: " + action, null);
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            return new Response(Status.ERROR, e.getMessage(), null);
        }
    }

    private Response getAllAuctions() {
            List<Auction> auctions = auctionService.getAllAuctions();
            logger.info("Get all auctions");
            return new Response(Status.SUCCESS, "List returned", auctions);
    }

    private Response getSellerItems(JsonElement data) {
        try {
            int sellerId = GsonUtil.fromJson(data, Integer.class);
            List<Auction> auctions = auctionService.getMyAuctions(sellerId);
            logger.info(auctions.toString());
            return new Response(Status.SUCCESS, "Seller Items List returned", auctions);
        } catch (Exception e){
            logger.error(e.getMessage(), e);
            return new Response(Status.ERROR, e.getMessage(), null);
        }
    }

    private Response getAuctionDetail(JsonElement data) {
        int auctionId = GsonUtil.fromJson(data, Integer.class);
        Auction auction = auctionService.getAuctionById(auctionId);
        logger.info("Auction ID: {}", auctionId);
        return new Response(Status.SUCCESS, "Auction detail", auction);
    }

    private Response createAuction(JsonElement data) {
        try {
            AuctionDTO auctionDTO = GsonUtil.fromJson(data, AuctionDTO.class);

            Auction auction = new Auction(auctionDTO);
            auctionService.createAuction(auction);
            logger.info("Created auction: " + auction);
            return new Response(Status.SUCCESS, "Auction created", auction);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return new Response(Status.ERROR, "Create auction failed: " + e.getMessage(), null);
        }
    }

    private Response editAuction(JsonElement data) {
        try {
            AuctionDTO auctionDTO = GsonUtil.fromJson(data, AuctionDTO.class);
            Auction auction = new Auction(auctionDTO);
            auctionService.editAuction(auction);
            return new Response(Status.SUCCESS, "Auction edited", auction);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return new Response(Status.ERROR, "Edit auction failed: " + e.getMessage(), null);
        }
    }

    private Response deleteAuction(JsonElement data) {
        try {
            int auctionId = GsonUtil.fromJson(data, Integer.class);
            auctionService.deleteAuction(auctionId);
            return new Response(Status.SUCCESS, "Auction deleted", null);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return new Response(Status.ERROR, "Delete auction failed: " + e.getMessage(), null);
        }
    }

    private Response closeAuction(JsonElement data) {
        int auctionId = GsonUtil.fromJson(data, Integer.class);
        auctionService.closeAuction(auctionId);
        logger.info("Closed auction: {}", auctionId);
        return new Response(Status.SUCCESS, "Auction closed", null);
    }
}
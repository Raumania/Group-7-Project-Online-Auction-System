package auction_system.server.controller;

import auction_system.common.dto.BidDTO;
import auction_system.common.enums.Action;
import auction_system.common.enums.Status;
import auction_system.common.enums.UserRole;
import auction_system.common.protocol.Request;
import auction_system.common.protocol.Response;
import auction_system.server.model.User;
import auction_system.server.service.BidService;
import auction_system.server.service.UserService;
import auction_system.server.util.GsonUtil;
import auction_system.server.exception.InvalidBidException;
import com.google.gson.JsonElement;

/**
 * Handle action PLACE_BID.
 * Use BidService.placeBid() and UserService to get User object with BIDDER role.
 */
public class BidController implements RequestHandler {

    private final BidService bidService = BidService.getInstance();
    private final UserService userService = UserService.getInstance();

    @Override
    public Response handle(Request request) {
        Action action = request.getAction();
        try {
            // Parse data from request (supports both Object and JsonElement)
            BidDTO bidDTO = parseData(request.getData(), BidDTO.class);

            // Get User from bidderId
            User bidder = userService.getUserById(bidDTO.getBidderId());

            /*
                Previously:
                if (!(user instanceof Bidder)) ...
                Bidder bidder = (Bidder) user;

                Now:
                No longer use instanceof.
                Check if user has BIDDER role.
            */
            if (!bidder.hasRole(UserRole.BIDDER)) {
                return new Response(Status.ERROR, action, null, "Only bidders can place bids");
            }

            bidService.placeBid(
                    bidDTO.getAuctionId(),
                    bidder,
                    bidDTO.getAmount()
            );

            // Get latest transaction to return (optional)
            var latestBid = bidService.getLatestBid(bidDTO.getAuctionId());

            // Return object directly, do not use GsonUtil.toJson()
            return new Response(Status.SUCCESS, action, latestBid, "Bid placed successfully");

        } catch (InvalidBidException e) {
            return new Response(Status.ERROR, action, null, e.getMessage());
        } catch (Exception e) {
            return new Response(Status.ERROR, action, null, "Place bid failed: " + e.getMessage());
        }
    }

    // Helper method to convert Object/JsonElement to object T
    private <T> T parseData(Object dataObj, Class<T> clazz) {
        if (dataObj instanceof JsonElement) {
            return GsonUtil.fromJson((JsonElement) dataObj, clazz);
        } else {
            String json = GsonUtil.toJson(dataObj);
            return GsonUtil.fromJson(json, clazz);
        }
    }
}

package auction_system.server.controller;

import auction_system.common.dto.AutoBidDTO;
import auction_system.common.enums.Action;
import auction_system.common.enums.Status;
import auction_system.common.protocol.Request;
import auction_system.common.protocol.Response;
import auction_system.server.service.AutoBidService;
import auction_system.server.util.GsonUtil;
import com.google.gson.JsonElement;

public class AutoBidController implements RequestHandler {

    private final AutoBidService autoBidService = AutoBidService.getInstance();

    @Override
    public Response handle(Request request) {
        Action action = request.getAction();
        try {
            AutoBidDTO dto = parseData(request.getData(), AutoBidDTO.class);

            if (action == Action.SET_AUTO_BID) {
                autoBidService.setAutoBid(
                        dto.getUserId(),
                        dto.getAuctionId(),
                        dto.getMaxBid(),
                        dto.getBidIncrement()
                );
                auction_system.server.model.User user = auction_system.server.service.UserService.getInstance().getUserById(dto.getUserId());
                return new Response(Status.SUCCESS, action, GsonUtil.getGson().toJsonTree(user.toDTO()), "Auto bid set successfully");
            } else if (action == Action.CANCEL_AUTO_BID) {
                autoBidService.cancelAutoBid(
                        dto.getUserId(),
                        dto.getAuctionId()
                );
                auction_system.server.model.User user = auction_system.server.service.UserService.getInstance().getUserById(dto.getUserId());
                return new Response(Status.SUCCESS, action, GsonUtil.getGson().toJsonTree(user.toDTO()), "Auto bid cancelled successfully");
            } else if (action == Action.GET_AUTO_BID_CONFIG) {
                auction_system.server.model.AutoBid ab = autoBidService.getAutoBidConfig(dto.getUserId(), dto.getAuctionId());
                if (ab != null) {
                    AutoBidDTO result = new AutoBidDTO(ab.getUserId(), ab.getAuctionId(), ab.getMaxBid(), ab.getBidIncrement());
                    return new Response(Status.SUCCESS, action, GsonUtil.getGson().toJsonTree(result), "Auto bid config retrieved");
                } else {
                    return new Response(Status.ERROR, action, null, "No active auto bid config");
                }
            } else {
                return new Response(Status.ERROR, action, null, "Invalid action for AutoBidController");
            }

        } catch (Exception e) {
            e.printStackTrace();
            return new Response(Status.ERROR, action, null, "Auto bid request failed: " + e.getMessage());
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

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
                return new Response(Status.SUCCESS, action, null, "Auto bid set successfully");
            } else if (action == Action.CANCEL_AUTO_BID) {
                autoBidService.cancelAutoBid(
                        dto.getUserId(),
                        dto.getAuctionId()
                );
                return new Response(Status.SUCCESS, action, null, "Auto bid cancelled successfully");
            } else {
                return new Response(Status.ERROR, action, null, "Invalid action for AutoBidController");
            }

        } catch (Exception e) {
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

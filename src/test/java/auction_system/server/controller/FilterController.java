package auction_system.server.controller;

import auction_system.common.enums.Action;
import auction_system.common.enums.Status;
import auction_system.common.protocol.Request;
import auction_system.common.protocol.Response;
import auction_system.server.model.Auction;
import auction_system.server.service.AuctionService;
import auction_system.server.service.BidService;
import auction_system.server.util.GsonUtil;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

import java.util.List;

public class FilterController implements RequestHandler{
    private final AuctionService auctionService=AuctionService.getInstance();
    @Override
    public Response handle(Request request) {
        Action action = request.getAction();

        try {
            switch (action) {
                case FILTER_CATEGORY:
                    return filterCategory(request.getData());
                case FILTER_STATUS:
                    return filterStatus(request.getData());
                default:
                    return new Response(Status.ERROR, "Unknown action: " + action, null);
            }
        } catch (Exception e) {
            return new Response(Status.ERROR, e.getMessage(), null);
        }
    }
    private Response filterCategory(JsonElement data){
        String category = GsonUtil.fromJson(data,String.class);
        List<Auction> auctionList=auctionService.findbyItemType(category);
        return new Response(Status.SUCCESS,"auctionlist by catagory return",auctionList);
    }
    private Response filterStatus(JsonElement data){
        String status = GsonUtil.fromJson(data,String.class);
        List<Auction> auctionList=auctionService.findbyStatus(status);
        return new Response(Status.SUCCESS,"auctionlist by status return",auctionList);
    }
}

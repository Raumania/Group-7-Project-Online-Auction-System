package auction_system.server.controller;

import auction_system.common.dto.BidDTO;
import auction_system.common.enums.Status;
import auction_system.common.protocol.Request;
import auction_system.common.protocol.Response;
import auction_system.server.model.BidTransaction;
import auction_system.server.service.BidService;
import com.google.gson.Gson;

import java.util.List;

public class BidHistoryController implements RequestHandler{

    private BidService bidService;
    private Gson gson;

    public BidHistoryController() {
        this.bidService = BidService.getInstance();
        this.gson = new Gson();
    }
    @Override
    public Response handle(Request request) {
        try {
            String jsonData = gson.toJson(request.getData());
            BidDTO data = gson.fromJson(jsonData, BidDTO.class);

            if (data == null || data.getAuctionId() <= 0) {
                return new Response(
                        Status.ERROR,
                        "BID_HISTORY",
                        "Auction id is invalid"
                );
            }

            List<BidTransaction> history =
                    bidService.getHistoryBid(data.getAuctionId());

            return new Response(
                    Status.SUCCESS,
                    "BID_HISTORY",
                    history
            );

        } catch (Exception e) {
            return new Response(
                    Status.ERROR,
                    "BID_HISTORY",
                    e.getMessage()
            );
        }
    }
}
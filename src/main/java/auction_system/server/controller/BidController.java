package auction_system.server.controller;

import auction_system.server.common.protocol.*;
import auction_system.server.model.Bidder;
import auction_system.server.model.User;
import auction_system.server.service.BidService;
import auction_system.server.service.UserService;
import com.google.gson.Gson;
import com.google.gson.JsonElement;

/**
 * Xử lý action PLACE_BID.
 * Sử dụng BidService.placeBid() và UserService để lấy đối tượng Bidder.
 */
public class BidController implements RequestHandler {

    private final Gson gson = new Gson();
    private final BidService bidService = new BidService();
    private final UserService userService = new UserService();

    @Override
    public Response handle(Request request) {
        if (!Action.PLACE_BID.equals(request.getAction())) {
            return new Response("ERROR", "BIDDING", null, "Invalid action for BidController");
        }
        try {
            // Parse dữ liệu từ request (hỗ trợ cả Object và JsonElement)
            BidData bidData = parseData(request.getData(), BidData.class);

            // Lấy User từ bidderId
            User user = userService.getUserById(bidData.getBidderId());
            if (!(user instanceof Bidder)) {
                return new Response("ERROR", "BIDDING", null, "Only bidders can place bids");
            }
            Bidder bidder = (Bidder) user;
            bidService.placeBid(bidData.getAuctionId(), bidder, bidData.getAmount());
            // Lấy giao dịch mới nhất để trả về (tuỳ chọn)
            var latestBid = bidService.getLatestBid(bidData.getAuctionId());
            // Trả về object trực tiếp, không dùng gson.toJson()
            return new Response("SUCCESS", "BIDDING", latestBid, "Bid placed successfully");
        } catch (Exception e) {
            return new Response("ERROR", "BIDDING", null, "Place bid failed: " + e.getMessage());
        }
    }

    // Helper method chuyển Object/JsonElement thành đối tượng T
    private <T> T parseData(Object dataObj, Class<T> clazz) {
        if (dataObj instanceof JsonElement) {
            return gson.fromJson((JsonElement) dataObj, clazz);
        } else {
            String json = gson.toJson(dataObj);
            return gson.fromJson(json, clazz);
        }
    }
}
package auction_system.server.controller;

import auction_system.common.dto.BidDTO;
import auction_system.common.enums.Status;
import auction_system.common.enums.UserRole;
import auction_system.common.protocol.Request;
import auction_system.common.protocol.Response;
import auction_system.server.model.User;
import auction_system.server.service.BidService;
import auction_system.server.service.UserService;
import auction_system.server.util.GsonUtil;
import com.google.gson.JsonElement;

/**
 * Xử lý action PLACE_BID.
 * Sử dụng BidService.placeBid() và UserService để lấy đối tượng User có role BIDDER.
 */
public class BidController implements RequestHandler {

    private final BidService bidService = BidService.getInstance();
    private final UserService userService = UserService.getInstance();

    @Override
    public Response handle(Request request) {
        try {
            // Parse dữ liệu từ request (hỗ trợ cả Object và JsonElement)
            BidDTO bidDTO = parseData(request.getData(), BidDTO.class);

            // Lấy User từ bidderId
            User bidder = userService.getUserById(bidDTO.getBidderId());

            /*
                Trước đây:
                if (!(user instanceof Bidder)) ...
                Bidder bidder = (Bidder) user;

                Bây giờ:
                Không dùng instanceof nữa.
                Kiểm tra user có role BIDDER không.
            */
            if (!bidder.hasRole(UserRole.BIDDER)) {
                return new Response(Status.ERROR, "Only bidders can place bids", null);
            }

            bidService.placeBid(
                    bidDTO.getAuctionId(),
                    bidder,
                    bidDTO.getAmount()
            );

            // Lấy giao dịch mới nhất để trả về (tuỳ chọn)
            var latestBid = bidService.getLatestBid(bidDTO.getAuctionId());

            // Trả về object trực tiếp, không dùng GsonUtil.toJson()
            return new Response(Status.SUCCESS, "Bid placed successfully", latestBid);

        } catch (Exception e) {
            return new Response(Status.ERROR, "Place bid failed: " + e.getMessage(), null);
        }
    }

    // Helper method chuyển Object/JsonElement thành đối tượng T
    private <T> T parseData(Object dataObj, Class<T> clazz) {
        try {
            if (dataObj instanceof JsonElement) {
                return GsonUtil.fromJson((JsonElement) dataObj, clazz);
            } else {
                String json = GsonUtil.toJson(dataObj);
                return GsonUtil.fromJson(json, clazz);
            }
        }catch (Exception e) {
            System.err.println("Parse data failed: " + e.getMessage());
            throw  new RuntimeException(e);
        }
    }
}
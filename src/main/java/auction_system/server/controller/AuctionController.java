package auction_system.server.controller;

import auction_system.server.common.protocol.Action;
import auction_system.server.common.protocol.CreateAuctionRequest;
import auction_system.server.common.protocol.Request;
import auction_system.server.common.protocol.Response;
import auction_system.server.model.Auction;
import auction_system.server.model.Item;
import auction_system.server.model.ItemType;
import auction_system.server.model.User;
import auction_system.server.service.AuctionService;
import auction_system.server.service.ItemService;
import auction_system.server.service.UserService;
import auction_system.server.util.GsonUtil;
import com.google.gson.JsonElement;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AuctionController implements RequestHandler {

    private final AuctionService auctionService = AuctionService.getInstance();
    private final UserService userService = new UserService();
    private final ItemService itemService = new ItemService();

    @Override
    public Response handle(Request request) {
        String action = request.getAction();

        try {
            switch (action) {
                case Action.GET_ALL_AUCTIONS:
                    return getAllAuctions();
                case Action.GET_AUCTION_DETAIL:
                    return getAuctionDetail(request.getData());
                case Action.CREATE_AUCTION:
                    return createAuction(request.getData());
                case Action.CLOSE_AUCTION:
                    return closeAuction(request.getData());
                default:
                    return new Response("ERROR", "AUCTION", null, "Unknown action: " + action);
            }
        } catch (Exception e) {
            return new Response("ERROR", "AUCTION", null, e.getMessage());
        }
    }

    /**
     * Phương thức chuyển đổi đối tượng Auction sang Map cấu trúc tùy chỉnh.
     * Tự động nhúng thông tin 'owner' vào bên trong 'item' dựa trên thông tin 'seller'
     * nhằm tái hiện chính xác định dạng JSON mong muốn của template cũ.
     */
    private Map<String, Object> convertAuctionToResponseMap(Auction auction) {
        if (auction == null) {
            return null;
        }

        Item item = auction.getItem();
        User seller = auction.getSeller();

        Map<String, Object> itemMap = new LinkedHashMap<>();
        itemMap.put("name", item.getName());
        itemMap.put("description", item.getDescription());
        itemMap.put("owner", seller);
        itemMap.put("type", item.getType());
        itemMap.put("startTime", item.getStartTime());
        itemMap.put("endTime", item.getEndTime());
        itemMap.put("id", item.getId());
        itemMap.put("createdAt", item.getCreatedAt());

        Map<String, Object> auctionMap = new LinkedHashMap<>();
        auctionMap.put("item", itemMap);
        auctionMap.put("seller", seller);
        auctionMap.put("startingPrice", auction.getStartingPrice());

    /*
        Có bidHistory thì trả về.
        Nếu chưa có bid nào thì nó sẽ là [].
    */
        auctionMap.put("bidHistory", auction.getBidHistory());

        auctionMap.put("status", auction.getStatus());
        auctionMap.put("id", auction.getId());
        auctionMap.put("createdAt", auction.getCreatedAt());

        return auctionMap;
    }
    private Response getAllAuctions() {
        List<Auction> auctions = auctionService.getAllAuctions();
        List<Map<String, Object>> listResponse = new ArrayList<>();
        if (auctions != null) {
            for (Auction auction : auctions) {
                listResponse.add(convertAuctionToResponseMap(auction));
            }
        }
        return new Response("SUCCESS", "AUCTION", listResponse, "List returned");
    }

    private Response getAuctionDetail(JsonElement data) {
        String auctionId = GsonUtil.fromJson(data, String.class);
        Auction auction = auctionService.getAuctionById(auctionId);
        return new Response("SUCCESS", "AUCTION", convertAuctionToResponseMap(auction), "Auction detail");
    }

    private Response createAuction(JsonElement data) {
        try {
            CreateAuctionRequest req = GsonUtil.fromJson(data, CreateAuctionRequest.class);
            User seller = userService.getSellerById(req.getSellerId());

            if (seller == null) {
                return new Response("ERROR", "AUCTION", null, "Seller not found");
            }

            System.out.println(req.getItemType());
            Item item = null;
            ItemType type = req.getItemType();
            LocalDateTime startTime = req.getStartTime();
            LocalDateTime endTime = req.getEndTime();
            double startingPrice = req.getStartingPrice();

            if (type == null) {
                return new Response("ERROR", "AUCTION", null, "Missing item type");
            }

            switch (type) {
                case ELECTRONICS:
                    item = itemService.createElectronics(
                            req.getName(),
                            req.getDescription(),
                            startTime,
                            endTime
                    );
                    break;

                case ART:
                    item = itemService.createArt(
                            req.getName(),
                            req.getDescription(),
                            startTime,
                            endTime
                    );
                    break;

                case VEHICLE:
                    item = itemService.createVehicle(
                            req.getName(),
                            req.getDescription(),
                            startTime,
                            endTime
                    );
                    break;

                default:
                    return new Response("ERROR", "AUCTION", null, "Unsupported item type: " + type);
            }

            // Bước 1: Lưu phiên đấu giá và thông tin thực thể liên quan vào Database thông qua Service
            Auction auction = auctionService.createAuction(item, seller, startingPrice, startTime, endTime);

            // Bước 2: Truy vấn lại phiên đấu giá vừa tạo từ CSDL bằng ID thực tế
            // Điều này đảm bảo lấy được đầy đủ các trường do DB sinh ra (ví dụ: các mốc thời gian, createdAt, id thật của các thực thể)
            Auction fullAuction = auctionService.getAuctionById(auction.getId());

            // Bước 3: Đưa qua bộ lọc cấu trúc Map để nhúng thủ công 'owner' vào và đóng gói Response
            Map<String, Object> responseData = convertAuctionToResponseMap(fullAuction);

            return new Response("SUCCESS", "AUCTION", responseData, "Auction created");

        } catch (Exception e) {
            e.printStackTrace();
            return new Response("ERROR", "AUCTION", null, "Create auction failed: " + e.getMessage());
        }
    }

    private Response closeAuction(JsonElement data) {
        String auctionId = GsonUtil.fromJson(data, String.class);
        auctionService.closeAuction(auctionId);
        return new Response("SUCCESS", "AUCTION", null, "Auction closed");
    }
}
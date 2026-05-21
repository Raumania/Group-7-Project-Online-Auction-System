//package auction_system.server.controller;
//
//import auction_system.server.common.protocol.*;
//import auction_system.server.model.User;
//import auction_system.server.service.NotificationService;
//import com.google.gson.Gson;
//import com.google.gson.JsonElement;
//
//public class NotificationController implements RequestHandler {
//    private final Gson gson = new Gson();
//    private final NotificationService notificationService = new NotificationService();
//    @Override
//    public Response handle(Request request) {
//        if (!Action.SUBSCRIBE.equals(request.getAction())) {
//            return new Response("ERROR", "BIDDING", null, "Invalid action for NotificationController");
//        } try {
//            SubscriberData data;
//            Object dataObj = request.getData();
//
//
//             if (dataObj instanceof JsonElement) {
//            data = gson.fromJson((JsonElement) dataObj, SubscriberData.class);
//            } else {
//                String json = gson.toJson(dataObj);
//                data = gson.fromJson(json, SubscriberData.class);
//            }
//
//        if (data.getAunctionID() == null || data.getUserID().trim().isEmpty()) {
//            throw new RuntimeException("AunctionID cannot be empty");
//        }
//
//        if (data.getUserID() == null || data.getUserID().trim().isEmpty()) {
//            throw new RuntimeException("Password cannot be empty");
//        }
//
//            /*
//                Không còn createBidder / createSeller theo nhánh if nữa.
//                Không còn truyền roles từ client nữa.
//
//                User mới mặc định có cả:
//                - BIDDER
//                - SELLER
//            */
//        notificationService.register(data.getAunctionID(), data.getUserID());
//
//            /*
//                Không gửi password về client.
//            */
//        return new Response(
//                "SUCCESS",
//                "SUBSCRIBE",
//                user,
//                "Register successfully"
//        );
//
//    } catch (Exception e) {
//        return new Response(
//                "ERROR",
//                "REGISTER",
//                null,
//                "Register failed: " + e.getMessage()
//        );
//    }
//    }
//}

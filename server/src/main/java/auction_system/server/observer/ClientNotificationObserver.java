package auction_system.server.observer;

import auction_system.common.enums.Action;
import auction_system.common.enums.Status;
import auction_system.common.protocol.Response;
import auction_system.server.model.Auction;
import auction_system.server.ClientHandler;
import auction_system.server.AuctionServer;
import auction_system.server.service.AuctionService;
import auction_system.server.util.GsonUtil;

public class ClientNotificationObserver implements AuctionObserver {
    private final AuctionService auctionService = AuctionService.getInstance();

    @Override
    public void onBidPlaced(BidEvent event) {
        System.out.println("[ClientNotificationObserver] Bid placed on auction: " + event.auctionId());
        
        // Fetch the updated auction and broadcast it as edited
        Auction auction = auctionService.getAuctionById(event.auctionId());
        if (auction != null) {
            onAuctionEdited(auction);
        }
    }

    @Override
    public void update(Auction auction, String message) {
        System.out.println("[ClientNotificationObserver] Auction status updated: " + auction.getId() + " - " + message);
    }

    @Override
    public void onAuctionCreated(Auction auction) {
        // Tạo Response gửi đi cho tất cả client khi có đấu giá mới được tạo
        Response response = new Response(Status.SUCCESS, Action.EVENT_NEW_AUCTION_ADDED, auction, "A new auction has been created!");
        String jsonResponse = GsonUtil.toJson(response);
        System.out.println("[ClientNotificationObserver] Broadcasting new auction created: " + ClientHandler.maskImageBase64(jsonResponse));
        AuctionServer.broadcast(jsonResponse);
    }

    @Override
    public void onAuctionEdited(Auction auction) {
        // Tạo Response gửi đi cho tất cả client khi có đấu giá được chỉnh sửa
        Response response = new Response(Status.SUCCESS, Action.EVENT_AUCTION_EDITED, auction, "An auction has been edited!");
        String jsonResponse = GsonUtil.toJson(response);
        System.out.println("[ClientNotificationObserver] Broadcasting auction edited: " + ClientHandler.maskImageBase64(jsonResponse));
        AuctionServer.broadcast(jsonResponse);
    }

    @Override
    public void onAuctionDeleted(int auctionId) {
        // Tạo Response gửi đi cho tất cả client khi có đấu giá bị xóa
        Response response = new Response(Status.SUCCESS, Action.EVENT_AUCTION_DELETED, auctionId, "An auction has been deleted!");
        String jsonResponse = GsonUtil.toJson(response);
        System.out.println("[ClientNotificationObserver] Broadcasting auction deleted: " + ClientHandler.maskImageBase64(jsonResponse));
        AuctionServer.broadcast(jsonResponse);
    }
}

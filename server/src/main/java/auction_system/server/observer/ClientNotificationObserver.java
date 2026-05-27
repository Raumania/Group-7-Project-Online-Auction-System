package auction_system.server.observer;

import auction_system.common.dto.BidTransactionDTO;
import auction_system.common.dto.UserDTO;
import auction_system.common.enums.Action;
import auction_system.common.enums.Status;
import auction_system.common.protocol.Response;
import auction_system.server.model.Auction;
import auction_system.server.model.BidTransaction;
import auction_system.server.model.User;
import auction_system.server.ClientHandler;
import auction_system.server.AuctionServer;
import auction_system.server.service.AuctionService;
import auction_system.server.service.BidService;
import auction_system.server.util.GsonUtil;

public class ClientNotificationObserver implements AuctionObserver {
    private final AuctionService auctionService = AuctionService.getInstance();
    private final BidService bidService = BidService.getInstance();

    @Override
    public void onBidPlaced(BidEvent event) {
        System.out.println("[ClientNotificationObserver] Bid placed on auction: " + event.auctionId());

        // 1. Broadcast updated auction data so all clients refresh price/bidder display
        Auction auction = auctionService.getAuctionById(event.auctionId());
        if (auction != null) {
            onAuctionEdited(auction);
        }

        // 2. Broadcast the new BidTransactionDTO so clients push it into BidTransactionStore
        try {
            BidTransaction latest = bidService.getLatestBid(event.auctionId());
            if (latest != null) {
                User bidder = latest.getBidder();
                UserDTO bidderDTO = new UserDTO();
                bidderDTO.setId(bidder.getId());
                bidderDTO.setUsername(bidder.getUsername());
                bidderDTO.setFullname(bidder.getFullname());

                BidTransactionDTO dto = new BidTransactionDTO(
                        latest.getId(),
                        event.auctionId(),
                        bidderDTO,
                        latest.getAmount(),
                        latest.getBidTime()
                );

                Response bidResponse = new Response(Status.SUCCESS, Action.EVENT_BID_PLACED, dto, "New bid placed!");
                String json = GsonUtil.toJson(bidResponse);
                System.out.println("[ClientNotificationObserver] Broadcasting EVENT_BID_PLACED for auction: " + event.auctionId());
                AuctionServer.broadcast(json);
            }
        } catch (Exception e) {
            System.err.println("[ClientNotificationObserver] Failed to broadcast bid event: " + e.getMessage());
        }
    }

    @Override
    public void update(Auction auction, String message) {
        System.out.println("[ClientNotificationObserver] Auction status updated: " + auction.getId() + " - " + message);
    }

    @Override
    public void onAuctionCreated(Auction auction) {
        Response response = new Response(Status.SUCCESS, Action.EVENT_NEW_AUCTION_ADDED, auction, "A new auction has been created!");
        String jsonResponse = GsonUtil.toJson(response);
        System.out.println("[ClientNotificationObserver] Broadcasting new auction created: " + ClientHandler.maskImageBase64(jsonResponse));
        AuctionServer.broadcast(jsonResponse);
    }

    @Override
    public void onAuctionEdited(Auction auction) {
        Response response = new Response(Status.SUCCESS, Action.EVENT_AUCTION_EDITED, auction, "An auction has been edited!");
        String jsonResponse = GsonUtil.toJson(response);
        System.out.println("[ClientNotificationObserver] Broadcasting auction edited: " + ClientHandler.maskImageBase64(jsonResponse));
        AuctionServer.broadcast(jsonResponse);
    }

    @Override
    public void onAuctionDeleted(int auctionId) {
        Response response = new Response(Status.SUCCESS, Action.EVENT_AUCTION_DELETED, auctionId, "An auction has been deleted!");
        String jsonResponse = GsonUtil.toJson(response);
        System.out.println("[ClientNotificationObserver] Broadcasting auction deleted: " + ClientHandler.maskImageBase64(jsonResponse));
        AuctionServer.broadcast(jsonResponse);
    }
}

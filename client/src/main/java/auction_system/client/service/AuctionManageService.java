package auction_system.client.service;

import auction_system.client.util.GsonUtil;
import auction_system.client.socket.SocketClient;
import auction_system.common.dto.AuctionDTO;
import auction_system.common.enums.Action;
import auction_system.common.enums.AuctionStatus;
import auction_system.common.enums.Status;
import auction_system.common.protocol.Request;
import auction_system.common.protocol.Response;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class AuctionManageService {
    //singleton for seller
    private static AuctionManageService instance;

    private AuctionManageService() {}

    public static AuctionManageService getInstance() {
        if (instance == null) {
            instance = new AuctionManageService();
        }
        return instance;
    }

    //core in below
    public boolean createAuction(AuctionDTO auctionDTO) {
        Request request = new Request(Action.CREATE_AUCTION, GsonUtil.getGson().toJsonTree(auctionDTO));
        Response response = SocketClient.getInstance().sendAndReceive(request);

        return response != null && response.getStatus() == Status.SUCCESS;
    }

    public List<AuctionDTO> getAuctionsBySellerId(int sellerId) {
        Request request = new Request(Action.GET_SELLER_ITEMS, GsonUtil.getGson().toJsonTree(sellerId));
        Response response = SocketClient.getInstance().sendAndReceive(request);

        if (response != null && response.getStatus() == Status.SUCCESS) {
            String jsonData = GsonUtil.toJson(response.getData());
            Type listType = new TypeToken<List<AuctionDTO>>(){}.getType();
            return GsonUtil.fromJson(jsonData, listType);
        } else {
            return new ArrayList<>();
        }
    }

    public boolean deleteAuction(AuctionDTO auctionDTO) {
        if (auctionDTO == null || auctionDTO.getStatus() != AuctionStatus.OPEN) {
            System.out.println("Cannot delete: Auction does not exist or status is not OPEN!");
            return false;
        }

        Request request = new Request(Action.DELETE_AUCTION, GsonUtil.getGson().toJsonTree(auctionDTO.getId()));
        Response response = SocketClient.getInstance().sendAndReceive(request);

        return response != null && response.getStatus() == Status.SUCCESS;
    }

    /**
     * Edit auction information with the condition that status must be OPEN
     * @param auctionDTO Auction object containing new info with old ID
     * @return true if edit succeeds, false if fails
     */
    public boolean editAuction(AuctionDTO auctionDTO) {
        // Check client-side condition: Can only edit when status is "OPEN"
        if (auctionDTO == null || auctionDTO.getStatus() != AuctionStatus.OPEN) {
            System.out.println("Cannot edit: Auction is not in OPEN status!");
            return false;
        }

        // Send edit Request to Server with Enum Action.EDIT_AUCTION
        Request request = new Request(Action.EDIT_AUCTION, GsonUtil.getGson().toJsonTree(auctionDTO));
        Response response = SocketClient.getInstance().sendAndReceive(request);

        return response != null && response.getStatus() == Status.SUCCESS;
    }

    public boolean cancelAuction(AuctionDTO auctionDTO) {
        if (auctionDTO == null || (auctionDTO.getStatus() != AuctionStatus.OPEN && auctionDTO.getStatus() != AuctionStatus.RUNNING)) {
            System.out.println("Cannot cancel: Auction is not in OPEN or RUNNING status!");
            return false;
        }

        Request request = new Request(Action.CANCEL_AUCTION, GsonUtil.getGson().toJsonTree(auctionDTO.getId()));
        Response response = SocketClient.getInstance().sendAndReceive(request);

        return response != null && response.getStatus() == Status.SUCCESS;
    }
}

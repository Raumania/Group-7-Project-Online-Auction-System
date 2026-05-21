package auction_system.client.service;

import auction_system.client.Util.GsonUtil;
import auction_system.client.socket.SocketClient;
import auction_system.client.store.AuctionStore;
import auction_system.common.dto.AuctionDTO;
import auction_system.common.enums.Action;
import auction_system.common.enums.Status;
import auction_system.common.protocol.Request;
import auction_system.common.protocol.Response;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

public class AuctionListService {
    //singleton for Auction List
    private static AuctionListService instance;

    private AuctionListService() {}

    public static AuctionListService getInstance() {
        if (instance == null) {
            instance = new AuctionListService();
        }
        return instance;
    }
    //core in below
    public void fetchAllAuctions() {
        Request request = new Request(Action.GET_ALL_AUCTIONS, null);
        SocketClient.getInstance().send(request);
        Response response = SocketClient.getInstance().receive();

        if(response != null && response.getStatus() == Status.SUCCESS) {
            String jsonData = GsonUtil.toJson(response.getData());
            Type listType = new TypeToken<List<AuctionDTO>>(){}.getType();
            List<AuctionDTO> auctions = GsonUtil.fromJson(jsonData, listType);
            if (auctions != null) {
                AuctionStore.getInstance().setAuctions(auctions);
            }
        }
    }
}
package auction_system.client.service;

import auction_system.client.util.GsonUtil;
import auction_system.client.socket.SocketClient;
import auction_system.common.dto.BidDTO;
import auction_system.common.dto.BidTransactionDTO;
import auction_system.common.enums.Action;
import auction_system.common.enums.Status;
import auction_system.common.protocol.Request;
import auction_system.common.protocol.Response;
import com.google.gson.reflect.TypeToken;

import auction_system.common.dto.AutoBidDTO;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class BidService {
    //singleton for bid service
    private static BidService instance;
    private final SocketClient socketClient = SocketClient.getInstance();

    private BidService() {}

    public static BidService getInstance() {
        if(instance == null) {
            instance = new BidService();
        }
        return instance;
    }

    //core in below
    public Response placeBid(int auctionId, BigDecimal amount, int bidderId) {
        BidDTO bidDTO = new BidDTO(auctionId, amount, bidderId);
        Request request = new Request(Action.PLACE_BID, GsonUtil.getGson().toJsonTree(bidDTO));
        return socketClient.sendAndReceive(request);
    }

    public List<BidTransactionDTO> getBidHistory(int auctionId) {
        BidDTO bidDTO = new BidDTO(auctionId, BigDecimal.ZERO, 0);
        Request request = new Request(Action.GET_BID_HISTORY, GsonUtil.getGson().toJsonTree(bidDTO));
        Response response = socketClient.sendAndReceive(request);

        if (response != null && response.getStatus() == Status.SUCCESS) {
            String jsonData = GsonUtil.toJson(response.getData());
            Type listType = new TypeToken<List<BidTransactionDTO>>(){}.getType();
            return GsonUtil.fromJson(jsonData, listType);
        } else {
            return new ArrayList<>();
        }
    }

    public Response setAutoBid(int userId, int auctionId, BigDecimal maxBid, BigDecimal bidIncrement) {
        AutoBidDTO autoBidDTO = new AutoBidDTO(userId, auctionId, maxBid, bidIncrement);
        Request request = new Request(Action.SET_AUTO_BID, GsonUtil.getGson().toJsonTree(autoBidDTO));
        return socketClient.sendAndReceive(request);
    }

    public Response cancelAutoBid(int userId, int auctionId) {
        AutoBidDTO autoBidDTO = new AutoBidDTO(userId, auctionId, BigDecimal.ZERO, BigDecimal.ZERO);
        Request request = new Request(Action.CANCEL_AUTO_BID, GsonUtil.getGson().toJsonTree(autoBidDTO));
        return socketClient.sendAndReceive(request);
    }

    public Response getAutoBidConfig(int userId, int auctionId) {
        AutoBidDTO autoBidDTO = new AutoBidDTO(userId, auctionId, BigDecimal.ZERO, BigDecimal.ZERO);
        Request request = new Request(Action.GET_AUTO_BID_CONFIG, GsonUtil.getGson().toJsonTree(autoBidDTO));
        return socketClient.sendAndReceive(request);
    }
}


package auction_system.client.service;

import auction_system.client.Util.GsonUtil;
import auction_system.client.socket.SocketClient;
import auction_system.common.dto.BidDTO;
import auction_system.common.dto.BidTransactionDTO;
import auction_system.common.enums.Action;
import auction_system.common.enums.Status;
import auction_system.common.protocol.Request;
import auction_system.common.protocol.Response;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class BidService {
    //singleton for bid service
    private static BidService instance;
    private BidService() {}

    public static BidService getInstance() {
        if(instance == null) {
            instance = new BidService();
        }
        return instance;
    }

    //core in below
    public Response placeBid(int auctionId, BigDecimal amount, int bidderId) {
        BidDTO bidDTO = new BidDTO(auctionId, amount.doubleValue(), bidderId);
        Request request = new Request(Action.PLACE_BID, GsonUtil.getGson().toJsonTree(bidDTO));
        SocketClient.getInstance().send(request);
        return SocketClient.getInstance().receive();
    }

    public List<BidTransactionDTO> getBidHistory(int auctionId) {
        BidDTO bidDTO = new BidDTO(auctionId, 0.0, 0);
        Request request = new Request(Action.GET_BID_HISTORY, GsonUtil.getGson().toJsonTree(bidDTO));
        SocketClient.getInstance().send(request);
        Response response = SocketClient.getInstance().receive();

        if (response != null && response.getStatus() == Status.SUCCESS) {
            String jsonData = GsonUtil.toJson(response.getData());
            Type listType = new TypeToken<List<BidTransactionDTO>>(){}.getType();
            return GsonUtil.fromJson(jsonData, listType);
        } else {
            return new ArrayList<>();
        }
    }
}

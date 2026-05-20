package auction_system.client.service;

import auction_system.client.socket.SocketClient;
import auction_system.common.dto.AuctionDTO;
import auction_system.common.enums.Action;
import auction_system.common.protocol.Request;
import auction_system.common.protocol.Response;
import javafx.collections.ObservableList;

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

}

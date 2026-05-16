package auction_system.client.service;

import auction_system.client.socket.SocketClient;
import auction_system.common.dto.AuctionDTO;
import auction_system.common.protocol.MessageType;
import auction_system.common.protocol.Request;
import auction_system.common.protocol.Response;

public class AuctionManageService {
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
        Request<AuctionDTO> request = new Request<>(MessageType.CREATE_AUCTION,auctionDTO);
        SocketClient.getInstance().send(request);
        Response response = SocketClient.getInstance().receive();
        if(response.getStatus().equals("SUCCESS")) {
            return true;
        }
        else {
            return false;
        }
    }
}

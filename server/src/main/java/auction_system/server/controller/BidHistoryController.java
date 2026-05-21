package auction_system.server.controller;

import auction_system.common.dto.BidDTO;
import auction_system.common.dto.BidTransactionDTO;
import auction_system.common.dto.UserDTO;
import auction_system.server.model.User;
import auction_system.common.enums.Action;
import auction_system.common.enums.Status;
import auction_system.common.protocol.Request;
import auction_system.common.protocol.Response;
import auction_system.server.model.BidTransaction;
import auction_system.server.service.BidService;
import auction_system.server.util.GsonUtil;

import java.util.ArrayList;
import java.util.List;

public class BidHistoryController implements RequestHandler {

    private final BidService bidService;

    public BidHistoryController() {
        this.bidService = BidService.getInstance();
    }

    @Override
    public Response handle(Request request) {
        Action action = request.getAction();
        try {
            BidDTO data = GsonUtil.fromJson(request.getData(), BidDTO.class);

            if (data == null || data.getAuctionId() <= 0) {
                return new Response(
                        Status.ERROR,
                        action,
                        null,
                        "Auction id is invalid"
                );
            }

            List<BidTransaction> history = bidService.getHistoryBid(data.getAuctionId());

            List<BidTransactionDTO> historyDTO = new ArrayList<>();
            for (BidTransaction tx : history) {
                User bidder = tx.getBidder();
                UserDTO bidderDTO = new UserDTO(bidder.getId(), bidder.getFullname(), bidder.getUsername(), bidder.getRoles(), bidder.getBalance().doubleValue());
                BidTransactionDTO txDTO = new BidTransactionDTO(tx.getId(), bidderDTO, tx.getAmount(), tx.getBidTime());
                historyDTO.add(txDTO);
            }

            return new Response(
                    Status.SUCCESS,
                    action,
                    historyDTO,
                    "Get bid history successfully"
            );

        } catch (Exception e) {
            return new Response(
                    Status.ERROR,
                    action,
                    null,
                    "Get bid history failed: " + e.getMessage()
            );
        }
    }
}
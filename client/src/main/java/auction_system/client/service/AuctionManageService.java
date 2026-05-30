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
            System.out.println("Không thể xóa: Phiên đấu giá không tồn tại hoặc trạng thái không phải là OPEN!");
            return false;
        }

        Request request = new Request(Action.DELETE_AUCTION, GsonUtil.getGson().toJsonTree(auctionDTO.getId()));
        Response response = SocketClient.getInstance().sendAndReceive(request);

        return response != null && response.getStatus() == Status.SUCCESS;
    }

    /**
     * Chỉnh sửa thông tin phiên đấu giá với điều kiện trạng thái phải là OPEN
     * @param auctionDTO Đối tượng phiên đấu giá chứa thông tin mới kèm ID cũ
     * @return true nếu sửa thành công, false nếu thất bại
     */
    public boolean editAuction(AuctionDTO auctionDTO) {
        // Kiểm tra điều kiện phía Client: Chỉ được sửa khi trạng thái là "OPEN"
        if (auctionDTO == null || auctionDTO.getStatus() != AuctionStatus.OPEN) {
            System.out.println("Không thể chỉnh sửa: Phiên đấu giá không ở trạng thái OPEN!");
            return false;
        }

        // Gửi Request chỉnh sửa lên Server với Enum Action.EDIT_AUCTION
        Request request = new Request(Action.EDIT_AUCTION, GsonUtil.getGson().toJsonTree(auctionDTO));
        Response response = SocketClient.getInstance().sendAndReceive(request);

        return response != null && response.getStatus() == Status.SUCCESS;
    }

    public boolean cancelAuction(AuctionDTO auctionDTO) {
        if (auctionDTO == null || (auctionDTO.getStatus() != AuctionStatus.OPEN && auctionDTO.getStatus() != AuctionStatus.RUNNING)) {
            System.out.println("Không thể hủy: Phiên đấu giá không ở trạng thái OPEN hoặc RUNNING!");
            return false;
        }

        Request request = new Request(Action.CANCEL_AUCTION, GsonUtil.getGson().toJsonTree(auctionDTO.getId()));
        Response response = SocketClient.getInstance().sendAndReceive(request);

        return response != null && response.getStatus() == Status.SUCCESS;
    }
}

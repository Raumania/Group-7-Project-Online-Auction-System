package auction_system.server.service;

import auction_system.server.dao.AuctionDAO;
import auction_system.server.exception.AuthorizationException;
import auction_system.server.exception.ItemInformationException;
import auction_system.server.model.Auction;
import auction_system.server.model.Item;
import auction_system.server.model.User;
import auction_system.server.model.UserRole;

import java.time.LocalDateTime;
import java.util.List;

public class AuctionService {

    private static AuctionService instance;

    private AuctionDAO auctionDAO;

    /*
        Constructor private để bên ngoài không thể:
        new AuctionService()
    */
    private AuctionService() {
        this.auctionDAO = new AuctionDAO();
    }

    /*
        Hàm lấy instance duy nhất của AuctionService.
    */
    public static AuctionService getInstance() {
        if (instance == null) {
            instance = new AuctionService();
        }

        return instance;
    }

    /*
        Tạo auction mới.

        Theo DB mới:
        - Auction tạo trước trong bảng auctions
        - Sau đó Item được lưu vào bảng items với items.id = auctions.id
        - startingTime và endingTime nằm ở bảng auctions
    */
    public Auction createAuction(Item item,
                                 User seller,
                                 double startingPrice,
                                 LocalDateTime startingTime,
                                 LocalDateTime endingTime) {

        if (item == null) {
            throw new NullPointerException("Item cannot be null");
        }

        if (seller == null) {
            throw new NullPointerException("Seller cannot be null");
        }

        if (!seller.hasRole(UserRole.SELLER)) {
            throw new AuthorizationException("Seller must have SELLER role");
        }

        if (item.getOwner() == null) {
            throw new ItemInformationException("Item must have owner");
        }

        if (!item.getOwner().getId().equals(seller.getId())) {
            throw new AuthorizationException("Seller does not own this item");
        }

        if (startingPrice <= 0) {
            throw new RuntimeException("Starting price must be greater than 0");
        }

        if (startingTime == null) {
            throw new NullPointerException("Starting time cannot be null");
        }

        if (endingTime == null) {
            throw new NullPointerException("Ending time cannot be null");
        }

        if (!endingTime.isAfter(startingTime)) {
            throw new RuntimeException("Ending time must be after starting time");
        }

        /*
            Tạo object Auction.
            currentPrice có thể để null trong constructor Auction
            để biểu thị chưa có ai bid.
        */
        Auction auction = new Auction(item, seller, startingPrice);

        /*
            Lưu auction trước.
            AuctionDAO sẽ:
            1. INSERT auctions
            2. Lấy generated auction id
            3. INSERT items với id = auctionId
        */
        auctionDAO.save(auction, startingTime, endingTime);

        return auction;
    }

    /*
        Lấy auction theo id.
    */
    public Auction getAuctionById(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new NullPointerException("Auction id cannot be null or empty");
        }

        Auction auction = auctionDAO.findById(id);

        if (auction == null) {
            throw new NullPointerException("Auction not found");
        }

        return auction;
    }

    /*
        Lấy tất cả auction.
    */
    public List<Auction> getAllAuctions() {
        return auctionDAO.findAll();
    }

    /*
        Lấy auction đang RUNNING.
    */
    public List<Auction> getOpenAuctions() {
        return auctionDAO.findOpenAuctions();
    }

    /*
        Start auction rồi update database.
    */
    public void startAuction(String auctionId) {
        Auction auction = getAuctionById(auctionId);

        auction.startAuction();

        boolean updated = auctionDAO.update(auction);

        if (!updated) {
            throw new RuntimeException("Cannot update auction");
        }
    }

    /*
        Close auction rồi update database.
    */
    public void closeAuction(String auctionId) {
        Auction auction = getAuctionById(auctionId);

        auction.closeAuction();

        boolean updated = auctionDAO.update(auction);

        if (!updated) {
            throw new RuntimeException("Cannot update auction");
        }
    }

    /*
        Cancel auction rồi update database.
    */
    public void cancelAuction(String auctionId) {
        Auction auction = getAuctionById(auctionId);

        auction.cancelAuction();

        boolean updated = auctionDAO.update(auction);

        if (!updated) {
            throw new RuntimeException("Cannot update auction");
        }
    }

    /*
        Xóa auction.
        Vì items có ON DELETE CASCADE,
        xóa auction thì item tương ứng cũng tự bị xóa.
    */
    public void removeAuction(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new RuntimeException("Auction id cannot be null or empty");
        }

        boolean removed = auctionDAO.deleteById(id);

        if (!removed) {
            throw new RuntimeException("Auction not found to remove");
        }
    }
}
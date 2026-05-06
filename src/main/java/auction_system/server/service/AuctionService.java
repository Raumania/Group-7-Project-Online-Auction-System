package auction_system.server.service;

import auction_system.model.Auction;
import auction_system.model.Item;
import auction_system.model.Seller;
import auction_system.server.dao.AuctionDAO;

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
    */
    public Auction createAuction(Item item, Seller seller) {
        if (item == null) {
            throw new RuntimeException("Item cannot be null");
        }

        if (seller == null) {
            throw new RuntimeException("Seller cannot be null");
        }

        if (item.getOwner() == null) {
            throw new RuntimeException("Item must have owner");
        }

        /*
            Không nên so sánh object bằng equals nếu bạn chưa override equals().
            So sánh id chắc hơn.
        */
        if (!item.getOwner().getId().equals(seller.getId())) {
            throw new RuntimeException("Seller does not own this item");
        }

        Auction auction = new Auction(item, seller);

        auctionDAO.save(auction);

        return auction;
    }

    /*
        Lấy auction theo id.
    */
    public Auction getAuctionById(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new RuntimeException("Auction id cannot be null or empty");
        }

        Auction auction = auctionDAO.findById(id);

        if (auction == null) {
            throw new RuntimeException("Auction not found");
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
        Lấy auction đang OPEN hoặc RUNNING.
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
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

    private AuctionService() {
        this.auctionDAO = new AuctionDAO();
    }

    public static AuctionService getInstance() {
        if (instance == null) {
            instance = new AuctionService();
        }

        return instance;
    }

    public Auction createAuction(Item item,
                                 User seller,
                                 double startingPrice,
                                 LocalDateTime startTime,
                                 LocalDateTime endTime) {

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

        if (startTime == null) {
            throw new NullPointerException("Starting time cannot be null");
        }

        if (endTime == null) {
            throw new NullPointerException("Ending time cannot be null");
        }

        if (!endTime.isAfter(startTime)) {
            throw new RuntimeException("Ending time must be after starting time");
        }

        Auction auction = new Auction(item, seller, startingPrice);

        auctionDAO.save(auction, startTime, endTime);

        return auction;
    }

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

    public List<Auction> getAllAuctions() {
        return auctionDAO.findAll();
    }

    public List<Auction> getOpenAuctions() {
        return auctionDAO.findOpenAuctions();
    }

    public void startAuction(String auctionId) {
        Auction auction = getAuctionById(auctionId);

        auction.startAuction();

        boolean updated = auctionDAO.update(auction);

        if (!updated) {
            throw new RuntimeException("Cannot update auction");
        }
    }

    public void closeAuction(String auctionId) {
        Auction auction = getAuctionById(auctionId);

        auction.closeAuction();

        boolean updated = auctionDAO.update(auction);

        if (!updated) {
            throw new RuntimeException("Cannot update auction");
        }
    }

    public void cancelAuction(String auctionId) {
        Auction auction = getAuctionById(auctionId);

        auction.cancelAuction();

        boolean updated = auctionDAO.update(auction);

        if (!updated) {
            throw new RuntimeException("Cannot update auction");
        }
    }

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

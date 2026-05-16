package auction_system.server.service;

import auction_system.server.AuctionServer;
import auction_system.server.dao.AuctionDAO;
import auction_system.server.dao.ItemDAO;
import auction_system.server.model.*;

import java.time.LocalDateTime;
import java.util.List;
public class AuctionService {
    //singleton for service
    private static AuctionService instance;

    private AuctionService() {
    }

    public static AuctionService getInstance() {
        if (instance == null) {
            instance = new AuctionService();
        }
        return instance;
    }
    //core in below

    private final AuctionDAO auctionDAO = new AuctionDAO();
    private final ItemDAO itemDAO = new ItemDAO();

    public void createAuction(Auction auction) {
        int id = auctionDAO.save(auction);
        itemDAO.save(auction, id);
    }

    public void deleteAuction(int id) {
        itemDAO.delete(id);
        auctionDAO.delete(id);
    }

    public void editAuction(Auction auction) {
        auctionDAO.update(auction);
        itemDAO.update(auction);
    }

    public Auction getAuctionById(int id) {
        return auctionDAO.findById(id);
    }

    public List<Auction> getAllAuctions() {
        return auctionDAO.findAll();
    }

    public List<Auction> getMyAuctions(int seller_id) {
        return auctionDAO.findAllBySellerId(seller_id);
    }

    public void closeAuction(int auctionId) {
        Auction auction = getAuctionById(auctionId);
        if (auction != null) {
            // Perform any business logic for closing an auction
            auctionDAO.update(auction);
        }
    }

    public Electronics createElectronics(String name, String description, LocalDateTime startTime, LocalDateTime endTime) {
        return new Electronics(name, description, startTime, endTime);
    }

    public Art createArt(String name, String description, LocalDateTime startTime, LocalDateTime endTime) {
        return new Art(name, description, startTime, endTime);
    }

    public Vehicle createVehicle(String name, String description, LocalDateTime startTime, LocalDateTime endTime) {
        return new Vehicle(name, description, startTime, endTime);
    }

    public Item getItemById(int id) {
        Item item = itemDAO.findById(id);

        if (item == null) {
            throw new RuntimeException("Item not found");
        }
        return item;
    }
}
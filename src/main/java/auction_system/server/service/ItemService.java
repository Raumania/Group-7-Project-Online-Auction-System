package auction_system.server.service;

import auction_system.server.dao.ItemDAO;
import auction_system.server.model.*;
import auction_system.server.service.AuctionService;

import java.time.LocalDateTime;

public class ItemService {

    private ItemDAO itemDAO;
    private AuctionService aucionService;

    public ItemService() {
        this.itemDAO = new ItemDAO();
    }

    public Electronics createElectronics(String name,
                                         String description,
                                         User owner,
                                         LocalDateTime startTime,
                                         LocalDateTime endTime) {
        aucionService.validateItemData(name, description, startTime, endTime);

        return new Electronics(
                name,
                description,
                startTime,
                endTime
        );
    }

    public Art createArt(String name,
                         String description,
                         LocalDateTime startTime,
                         LocalDateTime endTime) {
        aucionService.validateItemData(name, description, startTime, endTime);

        return new Art(name, description, startTime, endTime);
    }

    public Vehicle createVehicle(String name,
                                 String description,
                                 LocalDateTime startTime,
                                 LocalDateTime endTime) {
        aucionService.validateItemData(name, description, startTime, endTime);

        return new Vehicle(
                name,
                description,
                startTime,
                endTime
        );
    }

    public Item getItemById(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new RuntimeException("Item id cannot be null or empty");
        }

        Item item = itemDAO.findById(id);

        if (item == null) {
            throw new RuntimeException("Item not found");
        }

        return item;
    }
}
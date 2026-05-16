package auction_system.server.service;

import auction_system.server.dao.ItemDAO;
import auction_system.server.model.*;

import java.time.LocalDateTime;

public class ItemService {

    private ItemDAO itemDAO;

    public ItemService() {
        this.itemDAO = new ItemDAO();
    }

    public Electronics createElectronics(String name,
                                         String description,
                                         User owner,
                                         LocalDateTime startTime,
                                         LocalDateTime endTime) {
        validateItemData(name, description, owner, startTime, endTime);

        return new Electronics(
                name,
                description,
                owner,
                startTime,
                endTime
        );
    }

    public Art createArt(String name,
                         String description,
                         User owner,
                         LocalDateTime startTime,
                         LocalDateTime endTime) {
        validateItemData(name, description, owner, startTime, endTime);

        return new Art(
                name,
                description,
                owner,
                startTime,
                endTime
        );
    }

    public Vehicle createVehicle(String name,
                                 String description,
                                 User owner,
                                 LocalDateTime startTime,
                                 LocalDateTime endTime) {
        validateItemData(name, description, owner, startTime, endTime);

        return new Vehicle(
                name,
                description,
                owner,
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

    private void validateItemData(String name,
                                  String description,
                                  User owner,
                                  LocalDateTime startTime,
                                  LocalDateTime endTime) {
        if (name == null || name.trim().isEmpty()) {
            throw new RuntimeException("Item name cannot be null or empty");
        }

        if (description == null || description.trim().isEmpty()) {
            throw new RuntimeException("Item description cannot be null or empty");
        }

        validateSeller(owner);

        if (startTime == null) {
            throw new RuntimeException("Starting time cannot be null");
        }

        if (endTime == null) {
            throw new RuntimeException("Ending time cannot be null");
        }

        if (!endTime.isAfter(startTime)) {
            throw new RuntimeException("Ending time must be after starting time");
        }
    }

    private void validateSeller(User owner) {
        if (owner == null) {
            throw new RuntimeException("Owner cannot be null");
        }

        if (!owner.hasRole(UserRole.SELLER)) {
            throw new RuntimeException("Owner must have SELLER role");
        }
    }
}
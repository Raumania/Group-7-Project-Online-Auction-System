package auction_system.server.service;

import auction_system.server.dao.ItemDAO;
import auction_system.server.model.*;

import java.time.LocalDateTime;

public class ItemService {

    private ItemDAO itemDAO;

    public ItemService() {
        this.itemDAO = new ItemDAO();
    }

    /*
        Tạo object Electronics trong RAM.
        Không lưu DB ở đây.
        Item sẽ được lưu trong AuctionDAO.save(...)
        sau khi auction đã có id.
    */
    public Electronics createElectronics(String name,
                                         String description,
                                         User owner,
                                         LocalDateTime startingTime,
                                         LocalDateTime endingTime) {
        validateItemData(name, description, owner, startingTime, endingTime);

        return new Electronics(
                name,
                description,
                owner,
                startingTime,
                endingTime
        );
    }

    /*
        Tạo object Art trong RAM.
        Không lưu DB ở đây.
    */
    public Art createArt(String name,
                         String description,
                         User owner,
                         LocalDateTime startingTime,
                         LocalDateTime endingTime) {
        validateItemData(name, description, owner, startingTime, endingTime);

        return new Art(
                name,
                description,
                owner,
                startingTime,
                endingTime
        );
    }

    /*
        Tạo object Vehicle trong RAM.
        Không lưu DB ở đây.
    */
    public Vehicle createVehicle(String name,
                                 String description,
                                 User owner,
                                 LocalDateTime startingTime,
                                 LocalDateTime endingTime) {
        validateItemData(name, description, owner, startingTime, endingTime);

        return new Vehicle(
                name,
                description,
                owner,
                startingTime,
                endingTime
        );
    }

    /*
        Tìm item theo id.
        Vì items.id = auctions.id,
        id truyền vào đây chính là auctionId.
    */
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

    /*
        Kiểm tra dữ liệu item.
    */
    private void validateItemData(String name,
                                  String description,
                                  User owner,
                                  LocalDateTime startingTime,
                                  LocalDateTime endingTime) {
        if (name == null || name.trim().isEmpty()) {
            throw new RuntimeException("Item name cannot be null or empty");
        }

        if (description == null || description.trim().isEmpty()) {
            throw new RuntimeException("Item description cannot be null or empty");
        }

        validateSeller(owner);

        if (startingTime == null) {
            throw new RuntimeException("Starting time cannot be null");
        }

        if (endingTime == null) {
            throw new RuntimeException("Ending time cannot be null");
        }

        if (!endingTime.isAfter(startingTime)) {
            throw new RuntimeException("Ending time must be after starting time");
        }
    }

    /*
        Hàm phụ để kiểm tra quyền Seller.
    */
    private void validateSeller(User owner) {
        if (owner == null) {
            throw new RuntimeException("Owner cannot be null");
        }

        if (!owner.hasRole(UserRole.SELLER)) {
            throw new RuntimeException("Owner must have SELLER role");
        }
    }
}
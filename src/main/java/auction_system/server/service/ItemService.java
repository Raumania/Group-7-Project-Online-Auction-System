package auction_system.server.service;

import auction_system.server.dao.ItemDAO;
import auction_system.server.model.*;

public class ItemService {

    private ItemDAO itemDAO;

    public ItemService() {
        this.itemDAO = new ItemDAO();
    }

    /*
        Tạo item loại Electronics.

        Service sẽ:
        1. Kiểm tra owner có null không
        2. Kiểm tra owner có role SELLER không
        3. Tạo object Electronics
        4. Gọi ItemDAO để lưu vào database
        5. Trả item vừa tạo về
    */
    public Electronics createElectronics(String name, String description, double startingPrice,
                                         User owner, String brand, String model) {
        if (owner == null) {
            throw new RuntimeException("Owner cannot be null");
        }

        if (!owner.hasRole(UserRole.SELLER)) {
            throw new RuntimeException("Owner must have SELLER role");
        }

        Electronics electronics = new Electronics(
                name,
                description,
                startingPrice,
                owner,
                brand,
                model
        );

        itemDAO.save(electronics);

        return electronics;
    }

    /*
        Tạo item loại Art.
    */
    public Art createArt(String name, String description, double startingPrice,
                         User owner, String artist, int year) {
        if (owner == null) {
            throw new RuntimeException("Owner cannot be null");
        }

        if (!owner.hasRole(UserRole.SELLER)) {
            throw new RuntimeException("Owner must have SELLER role");
        }

        Art art = new Art(
                name,
                description,
                startingPrice,
                owner,
                artist,
                year
        );

        itemDAO.save(art);

        return art;
    }

    /*
        Tạo item loại Vehicle.
    */
    public Vehicle createVehicle(String name, String description, double startingPrice,
                                 User owner, String brand, int year) {
        if (owner == null) {
            throw new RuntimeException("Owner cannot be null");
        }

        if (!owner.hasRole(UserRole.SELLER)) {
            throw new RuntimeException("Owner must have SELLER role");
        }

        Vehicle vehicle = new Vehicle(
                name,
                description,
                startingPrice,
                owner,
                brand,
                year
        );

        itemDAO.save(vehicle);

        return vehicle;
    }

    /*
        Tìm item theo id.
    */
    public Item getItemById(String id) {
        Item item = itemDAO.findById(id);

        if (item == null) {
            throw new RuntimeException("Item not found");
        }

        return item;
    }
}
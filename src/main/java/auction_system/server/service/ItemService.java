package auction_system.server.service;

import auction_system.model.Art;
import auction_system.model.Electronics;
import auction_system.model.Item;
import auction_system.model.Seller;
import auction_system.model.Vehicle;
import auction_system.server.dao.ItemDAO;

public class ItemService {

    private ItemDAO itemDAO;

    public ItemService() {
        this.itemDAO = new ItemDAO();
    }

    /*
        Tạo item loại Electronics.

        Service sẽ:
        1. Kiểm tra seller có null không
        2. Tạo object Electronics
        3. Gọi ItemDAO để lưu vào database
        4. Trả item vừa tạo về
    */
    public Electronics createElectronics(String name, String description, double startingPrice,
                                         Seller owner, String brand, String model) {
        if (owner == null) {
            throw new RuntimeException("Owner cannot be null");
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
                         Seller owner, String artist, int year) {
        if (owner == null) {
            throw new RuntimeException("Owner cannot be null");
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
                                 Seller owner, String brand, int year) {
        if (owner == null) {
            throw new RuntimeException("Owner cannot be null");
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
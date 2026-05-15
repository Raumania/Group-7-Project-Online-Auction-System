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
        Tạo item loại Electronics.
        CẬP NHẬT: Nhận thời gian bắt đầu/kết thúc, loại bỏ brand/model.
    */
    public Electronics createElectronics(String name, String description, User owner,
                                         LocalDateTime startingTime, LocalDateTime endingTime) {
        validateSeller(owner);

        Electronics electronics = new Electronics(name, description, owner, startingTime, endingTime);
        itemDAO.save(electronics);
        return electronics;
    }

    /*
        Tạo item loại Art.
        CẬP NHẬT: Nhận thời gian bắt đầu/kết thúc, loại bỏ artist/year.
    */
    public Art createArt(String name, String description, User owner,
                         LocalDateTime startingTime, LocalDateTime endingTime) {
        validateSeller(owner);

        Art art = new Art(name, description, owner, startingTime, endingTime);
        itemDAO.save(art);
        return art;
    }

    /*
        Tạo item loại Vehicle.
        CẬP NHẬT: Nhận thời gian bắt đầu/kết thúc, loại bỏ brand/year.
    */
    public Vehicle createVehicle(String name, String description, User owner,
                                 LocalDateTime startingTime, LocalDateTime endingTime) {
        validateSeller(owner);

        Vehicle vehicle = new Vehicle(name, description, owner, startingTime, endingTime);
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

    /*
        Hàm phụ để kiểm tra quyền Seller (tránh lặp code)
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
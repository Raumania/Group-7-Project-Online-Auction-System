package auction_system.server.model;

import java.time.LocalDateTime;

public class Vehicle extends Item {

    /*
        CẬP NHẬT: Loại bỏ hoàn toàn brand và year.
        Chỉ giữ lại constructor để truyền thông tin về lớp cha Item.
    */
    public Vehicle(String name, String description, User owner,
                   LocalDateTime startingTime, LocalDateTime endingTime) {

        // Truyền ItemType.VEHICLE cùng thời gian đấu giá vào super
        super(name, description, owner, ItemType.VEHICLE, startingTime, endingTime);
    }

    @Override
    public String toString() {
        // Trả về định dạng chung của Item, bắt đầu bằng tiền tố "Vehicle"
        return "Vehicle" + super.toString().substring(4);
    }
}
package auction_system.server.model;

import java.time.LocalDateTime;

public class Art extends Item {

    /*
        CẬP NHẬT: Loại bỏ artist và year.
        Constructor bây giờ tập trung vào thông tin đấu giá và thời gian.
    */
    public Art(String name, String description, User owner,
               LocalDateTime startTime, LocalDateTime endTime) {

        // Truyền ItemType.ART và các thông tin thời gian vào lớp cha
        super(name, description, owner, ItemType.ART, startTime, endTime);
    }

    @Override
    public String toString() {
        return "Art" + super.toString().substring(4);
    }
}
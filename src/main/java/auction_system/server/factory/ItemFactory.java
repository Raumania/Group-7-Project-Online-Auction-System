package auction_system.server.factory;

import auction_system.server.model.*;
import java.time.LocalDateTime;

public class ItemFactory {

    /*
        CẬP NHẬT: extraParams bây giờ sẽ chứa:
        - index 0: LocalDateTime startingTime
        - index 1: LocalDateTime endingTime
    */
    public static Item createItem(ItemType type,
                                  String name,
                                  String description,
                                  User owner,
                                  Object... extraParams) {

        if (type == null) {
            throw new RuntimeException("Item type cannot be null");
        }

        if (name == null || name.trim().isEmpty()) {
            throw new RuntimeException("Item name cannot be null or empty");
        }

        if (description == null || description.trim().isEmpty()) {
            throw new RuntimeException("Item description cannot be null or empty");
        }

        if (owner == null) {
            throw new RuntimeException("Owner cannot be null");
        }

        // Kiểm tra xem đã truyền đủ 2 mốc thời gian chưa
        if (extraParams.length < 2 || !(extraParams[0] instanceof LocalDateTime) || !(extraParams[1] instanceof LocalDateTime)) {
            throw new RuntimeException("Starting time and Ending time (LocalDateTime) are required in extraParams");
        }

        LocalDateTime start = (LocalDateTime) extraParams[0];
        LocalDateTime end = (LocalDateTime) extraParams[1];

        switch (type) {
            case ELECTRONICS:
                return new Electronics(name, description, owner, start, end);

            case ART:
                return new Art(name, description, owner, start, end);

            case VEHICLE:
                return new Vehicle(name, description, owner, start, end);

            default:
                throw new RuntimeException("Invalid item type: " + type);
        }
    }
}
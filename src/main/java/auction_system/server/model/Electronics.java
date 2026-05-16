package auction_system.server.model;

import auction_system.common.enums.ItemType;

import java.time.LocalDateTime;

public class Electronics extends Item {

    public Electronics(String name, String description, LocalDateTime startingTime, LocalDateTime endingTime) {
        super(name, description, ItemType.ELECTRONICS, startingTime, endingTime);
    }

    @Override
    public String toString() {
        return "Electronics" + super.toString().substring(4);
    }
}
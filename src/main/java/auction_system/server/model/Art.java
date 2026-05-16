package auction_system.server.model;

import auction_system.common.enums.ItemType;

import java.time.LocalDateTime;

public class Art extends Item {

    public Art(String name, String description, LocalDateTime startTime, LocalDateTime endTime) {
        super(name, description, ItemType.ART, startTime, endTime);
    }

    @Override
    public String toString() {
        return "Art" + super.toString().substring(4);
    }
}
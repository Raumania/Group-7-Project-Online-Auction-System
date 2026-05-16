package auction_system.server.model;

import auction_system.common.enums.ItemType;

import java.time.LocalDateTime;

public class Vehicle extends Item {

    public Vehicle(String name, String description, LocalDateTime startingTime, LocalDateTime endingTime) {
        super(name, description, ItemType.VEHICLE, startingTime, endingTime);
    }

    @Override
    public String toString() {
        return "Vehicle" + super.toString().substring(4);
    }
}
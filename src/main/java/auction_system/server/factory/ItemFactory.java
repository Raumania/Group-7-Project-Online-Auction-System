package auction_system.server.factory;

import auction_system.server.model.*;

public class ItemFactory {

    public static Item createItem(ItemType type,
                                  String name,
                                  String description,
                                  double startingPrice,
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

        if (startingPrice <= 0) {
            throw new RuntimeException("Starting price must be greater than 0");
        }

        if (owner == null) {
            throw new RuntimeException("Owner cannot be null");
        }

        switch (type) {
            case ELECTRONICS:
                if (extraParams.length < 2) {
                    throw new RuntimeException("Electronics needs brand and model");
                }

                String electronicsBrand = (String) extraParams[0];
                String electronicsModel = (String) extraParams[1];

                return new Electronics(
                        name,
                        description,
                        startingPrice,
                        owner,
                        electronicsBrand,
                        electronicsModel
                );

            case ART:
                if (extraParams.length < 2) {
                    throw new RuntimeException("Art needs artist and year");
                }

                String artist = (String) extraParams[0];
                int artYear = (int) extraParams[1];

                return new Art(
                        name,
                        description,
                        startingPrice,
                        owner,
                        artist,
                        artYear
                );

            case VEHICLE:
                if (extraParams.length < 2) {
                    throw new RuntimeException("Vehicle needs brand and year");
                }

                String vehicleBrand = (String) extraParams[0];
                int vehicleYear = (int) extraParams[1];

                return new Vehicle(
                        name,
                        description,
                        startingPrice,
                        owner,
                        vehicleBrand,
                        vehicleYear
                );

            default:
                throw new RuntimeException("Invalid item type");
        }
    }
}
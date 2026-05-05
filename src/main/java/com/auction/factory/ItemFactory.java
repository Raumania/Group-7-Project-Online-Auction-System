package com.auction.factory;

import com.auction.model.Art;
import com.auction.model.Electronics;
import com.auction.model.Item;
import com.auction.model.ItemType;
import com.auction.model.Vehicle;

public class ItemFactory {

    public static Item createItem(ItemType type, String name, String description, double startingPrice) {
        switch (type) {
            case ELECTRONICS:
                return new Electronics(name, description, startingPrice, "Unknown", 0);

            case ART:
                return new Art(name, description, startingPrice, "Unknown", 0);

            case VEHICLE:
                return new Vehicle(name, description, startingPrice, "Unknown", 0, 0);

            default:
                return null;
        }
    }
}
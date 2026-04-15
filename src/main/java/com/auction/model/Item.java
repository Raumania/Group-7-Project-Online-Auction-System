package com.auction.model;

import com.auction.util.IdGenerator;

public abstract class Item extends Entity {
    protected String name;
    protected String description;
    protected double startingPrice;

    public Item(String name, String description, double startingPrice) {
        super();
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.id = IdGenerator.generationItemId();
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    @Override
    public String toString() {
        return "Item{id='" + id + "', name='" + name + "', startingPrice=" + startingPrice + "}";
    }
}
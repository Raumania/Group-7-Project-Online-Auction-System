package auction_system.model;

import auction_system.model.Entity;
import auction_system.util.IdGenerator;

public abstract class Item extends Entity {
    protected String name;
    protected String description;
    protected double startingPrice;
    protected  Seller owner;
    protected ItemType type;
    public Item(String name, String description, double startingPrice, Seller owner, ItemType type) {
        super();

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

        if (type == null) {
            throw new RuntimeException("Item type cannot be null");
        }

        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.id = IdGenerator.generationItemId();
        this.owner = owner;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Seller getOwner(){
        return owner;
    }
    public double getStartingPrice() {
        return startingPrice;
    }

    public ItemType getType(){
        return type;
    }
    @Override
    public String toString() {
        return "Item{id='" + id + "', name='" + name + "', startingPrice=" + startingPrice + "}";
    }
}
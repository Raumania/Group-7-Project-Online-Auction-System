package auction_system.server.model;

import auction_system.util.IdGenerator;

public abstract class Item extends Entity {
    protected String name;
    protected String description;
    protected double startingPrice;

    /*
        Trước đây owner là Seller.

        Bây giờ:
        - User không còn chia cứng thành Seller/Bidder nữa
        - Một User có thể có nhiều role
        - Vì vậy owner là User
        - Nhưng User này bắt buộc phải có role SELLER
    */
    protected User owner;

    protected ItemType type;

    public Item(String name, String description, double startingPrice, User owner, ItemType type) {
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

        /*
            Owner phải là user có quyền SELLER.
            Không dùng instanceof Seller nữa.
        */
        if (!owner.hasRole(UserRole.SELLER)) {
            throw new RuntimeException("Owner must have SELLER role");
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

    public User getOwner() {
        return owner;
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    public ItemType getType() {
        return type;
    }

    @Override
    public String toString() {
        return "Item{id='" + id + "', name='" + name + "', startingPrice=" + startingPrice + "}";
    }
}
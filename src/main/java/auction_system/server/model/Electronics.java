package auction_system.server.model;

import auction_system.server.exception.ItemInformationException;

public class Electronics extends Item {

    private String brand;
    private String model;

    public Electronics(String name, String description, double startingPrice,
                       User owner, String brand, String model) {

        super(name, description, startingPrice, owner, ItemType.ELECTRONICS);

        if (brand == null || brand.trim().isEmpty()) {
            throw new ItemInformationException("Brand cannot be empty");
        }

        if (model == null || model.trim().isEmpty()) {
            throw new ItemInformationException("Model cannot be empty");
        }

        this.brand = brand;
        this.model = model;
    }

    public String getBrand() {
        return brand;
    }

    public String getModel() {
        return model;
    }

    @Override
    public String toString() {
        return super.toString() +
                ", brand='" + brand + '\'' +
                ", model='" + model + '\'';
    }
}
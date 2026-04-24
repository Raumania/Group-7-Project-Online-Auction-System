package auction_system.model;

public class Electronics extends Item {

    private String brand;
    private String model;

    public Electronics(String name, String description, double startingPrice,
                       Seller owner, String brand, String model) {

        super(name, description, startingPrice, owner, ItemType.ELECTRONICS);

        if (brand == null || brand.isEmpty()) {
            throw new RuntimeException("Brand cannot be empty");
        }

        if (model == null || model.isEmpty()) {
            throw new RuntimeException("Model cannot be empty");
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
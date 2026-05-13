package auction_system.server.model;

import auction_system.server.exception.ItemInformationException;

public class Vehicle extends Item {

    private String brand;
    private int year;

    public Vehicle(String name, String description, double startingPrice,
                   User owner, String brand, int year) {

        super(name, description, startingPrice, owner, ItemType.VEHICLE);

        if (brand == null || brand.trim().isEmpty()) {
            throw new ItemInformationException("Brand cannot be empty");
        }

        if (year <= 0) {
            throw new ItemInformationException("Year must be valid");
        }

        this.brand = brand;
        this.year = year;
    }

    public String getBrand() {
        return brand;
    }

    public int getYear() {
        return year;
    }

    @Override
    public String toString() {
        return super.toString() +
                ", brand='" + brand + '\'' +
                ", year=" + year;
    }
}
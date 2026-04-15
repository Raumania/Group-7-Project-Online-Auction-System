package com.auction.model;

public class Vehicle extends Item {
    private int mileage;
    private String model;
    private int year;

    public Vehicle(String name, String description, double startingPrice, String model, int mileage, int year) {
        super(name, description, startingPrice);
        this.model = model;
        this.mileage = mileage;
        this.year = year;
    }

    public String getModel() {
        return model;
    }

    public int getMileage() {
        return mileage;
    }

    public int getYear() {
        return year;
    }

    @Override
    public String toString() {
        return "Vehicle{name='" + name + "', model='" + model +
                "', mileage=" + mileage + ", year=" + year +
                ", startingPrice=" + startingPrice + "}";
    }
}
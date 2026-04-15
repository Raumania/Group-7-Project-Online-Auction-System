package com.auction.model;

public class Electronics extends Item {
    private String brand;
    private int warrantyMonths;

    public Electronics(String name, String description, double startingPrice, String brand, int warrantyMonths) {
        super(name, description, startingPrice);
        this.brand = brand;
        this.warrantyMonths = warrantyMonths;
    }

    public String getBrand() {
        return brand;
    }

    public int getWarrantyMonths() {
        return warrantyMonths;
    }

    @Override
    public String toString() {
        return "Electronics{name='" + name + "', brand='" + brand +
                "', warrantyMonths=" + warrantyMonths +
                ", startingPrice=" + startingPrice + "}";
    }
}
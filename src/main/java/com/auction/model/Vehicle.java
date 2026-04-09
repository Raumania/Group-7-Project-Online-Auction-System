package com.auction.model;

public class Vehicle extends Item{
    private int milage;
    private String model;
    private int year;
    public Vehicle(String name, String description, double startingprice,String model,int milage,int year){
        super(name,description,startingprice);
        this.model=model;
        this.milage=milage;
        this.year=year;
    }

    public String getModel() {
        return model;
    }

    public int getMilage() {
        return milage;
    }
    public int year(){
        return year;
    }
}

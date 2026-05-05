package com.auction.model;

public class Electronics extends Item{
    private String brand;
    private int warrantymonths;
    public Electronics(String name,String description,double startingprice,String brand,int warrantymonths){
        super(name,description,startingprice);
        this.brand=brand;
        this.warrantymonths=warrantymonths;
    }
    public String getBrand(){
        return brand;
    }
    public int getWarrantymonths(){
        return warrantymonths;
    }
}

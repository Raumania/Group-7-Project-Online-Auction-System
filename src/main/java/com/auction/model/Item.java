package com.auction.model;

import com.auction.util.IdGenerator;

public abstract class Item extends Entity {
    protected String name;
    protected String description;
    protected double startingprice;
    public Item(String name,String description,double startingprice){
        super();
        this.name=name;
        this.description=description;
        this.startingprice=startingprice;
        this.id= IdGenerator.generationItemId();
    }
    public String getName(){
        return name;
    }
    public String getDescription(){
        return description;
    }
    public double getStartingprice(){
        return startingprice;
    }
    public String getId(){
        return this.id;
    }

}

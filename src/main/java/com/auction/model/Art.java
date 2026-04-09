package com.auction.model;

public class Art extends Item{
    private String artist;
    private int years;
    public Art(String name,String description,double startingprice,String artist,int years) {
        super(name, description, startingprice);
        this.artist = artist;
        this.years = years;
    }
    public String getArtist(){
        return artist;
    }
    public int getYears(){
        return years;
    }
}

package com.auction.model;

import com.auction.util.IdGenerator;

public class Bidder extends User {

    public Bidder(String username, String password, String email) {
        super(username, password, email);
        this.id= IdGenerator.generationBidId();
    }
    public String getId(){
        return this.id;
    }

}
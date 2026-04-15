package com.auction.model;

import com.auction.util.IdGenerator;

public class Seller extends User {

    public Seller(String username, String password, String email) {
        super(username, password, email);
        this.id = IdGenerator.generationSellerId();
    }
}
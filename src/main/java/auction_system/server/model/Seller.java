package auction_system.server.model;

import auction_system.server.util.IdGenerator;

public class Seller extends User {

    public Seller(String username, String password, String email) {
        super(username, password, email);
        this.id = IdGenerator.generationSellerId();
    }
}
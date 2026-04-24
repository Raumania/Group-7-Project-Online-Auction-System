package auction_system.model;

import auction_system.model.User;
import auction_system.util.IdGenerator;

public class Seller extends User {

    public Seller(String username, String password, String email) {
        super(username, password, email);
        this.id = IdGenerator.generationSellerId();
    }
}
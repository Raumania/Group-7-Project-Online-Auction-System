package auction_system.server.model;

import auction_system.util.IdGenerator;

public class Bidder extends User {

    public Bidder(String username, String password, String email) {
        super(username, password, email, UserRole.BIDDER);
        this.id = IdGenerator.generationBidId();
    }
}
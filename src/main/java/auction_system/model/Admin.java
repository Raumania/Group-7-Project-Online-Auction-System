package auction_system.model;

import auction_system.util.IdGenerator;

public class Admin extends User {

    public Admin(String username, String password, String email) {
        super(username, password, email, UserRole.ADMIN);
        this.id = IdGenerator.generationAdminId();
    }
}
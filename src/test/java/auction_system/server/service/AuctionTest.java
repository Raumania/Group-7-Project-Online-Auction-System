package auction_system.server.service;
import auction_system.common.enums.UserRole;
import auction_system.server.model.User;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AuctionTest {
    @Test
    void getDT () {
        UserService userService = new UserService();
        Set<UserRole> userRoles = new HashSet<>();
        userRoles.add(UserRole.SELLER);
        userRoles.add(UserRole.BIDDER);
        User user = new User ("1","1", "1", userRoles );
        user.setId(15);
        assertEquals(userService.getUserById(15), user);

    }
}

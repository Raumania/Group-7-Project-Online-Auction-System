package auction_system.server.service.Test;

import auction_system.common.enums.UserRole;
import auction_system.server.model.User;
import auction_system.server.service.UserService;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserServiceTest {
    @Test
    void getUser() {
        UserService userService = UserService.getInstance();
        Set<UserRole> roles = Set.of(UserRole.BIDDER, UserRole.SELLER);
        User user = new User("1", "1", "1", roles);
        user.setId(15);
        assertEquals(userService.getUserById(15), user);
    }
}

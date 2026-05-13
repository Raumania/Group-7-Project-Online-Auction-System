
import auction_system.server.exception.*;
import auction_system.server.model.*;
import auction_system.server.service.AuctionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class AuctionTest {
    User b1 = new User("leeduc", "leduc2703", "abc@gmail.com", Collections.singleton(UserRole.BIDDER));
    User s1 = new User("chuong", "chuongw", "cde@gmail.com", Collections.singleton(UserRole.SELLER));
    User s2 = new User("ducbanh", "ducbanh", "fgh@gmail.com", Collections.singleton(UserRole.SELLER));
    Item phone = new Electronics("phone", "Flagship", Double.MIN_VALUE, s1, "apple", "iphone 16");
    Auction auction = new Auction(phone, s1);

   @Test
    void testValidBid() {
        auction.placeBid(b1, Double.MIN_VALUE);
        assertEquals(Double.MIN_VALUE, auction.getCurrentPrice());

       auction.placeBid(b1, Double.MIN_VALUE + 0.0001);
       assertEquals(Double.MIN_VALUE + 0.0001, auction.getCurrentPrice());

       auction.placeBid(b1, 36);
       assertEquals(36, auction.getCurrentPrice());

       auction.placeBid(b1, Double.MAX_VALUE);
       assertEquals(Double.MAX_VALUE, auction.getCurrentPrice());
    }

    @Test
    void testInvalidBid() {
        auction.placeBid(b1, 36);

        assertThrowsExactly(InvalidBidException.class, () -> {auction.placeBid(b1, 35);});
    }

    @Test
    void testAuctionClosed () {
        auction.placeBid(b1, 36);
        auction.closeAuction();

        assertThrowsExactly(StatusException.class, () -> {auction.placeBid(b1, 37);});
    }

    @Test
    void testAuthentication () {
        User no_one = null;

        assertThrowsExactly(NullPointerException.class, () -> {auction.placeBid(no_one, 100);});
    }

    @Test
    void testAuthorization () {
        assertThrowsExactly(AuthorizationException.class, () -> {new Auction(phone, s2);});
    }

    @Test
    void testItemInfo () {
        User no_one = null;
        assertThrowsExactly(ItemInformationException.class, () -> {new Electronics("phone", "Flagship", 36, no_one, "apple", "iphone 16");});
    }
}

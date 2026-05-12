import com.auction_system.exception.*;
import com.auction_system.model.*;
import com.auction_system.service.AuctionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

public class AuctionTest {
    Bidder b1 = new Bidder("leeduc", "leduc2703", "abc@gmail.com");
    Seller s1 = new Seller("chuong", "chuongw", "cde@gmail.com");
    Seller s2 = new Seller("ducbanh", "ducbanh", "fgh@gmail.com");
    Item phone = new Electronics("phone", "Flagship", Double.MIN_VALUE, s1, "apple", "iphone 16");
    AuctionService as = new AuctionService();
    Auction auction = as.createAuction(phone, s1);

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

        assertThrowsExactly(AuctionClosedException.class, () -> {auction.placeBid(b1, 37);});
    }

    @Test
    void testAuthentication () {
        Bidder no_one = null;

        assertThrowsExactly(AuthenticationException.class, () -> {auction.placeBid(no_one, 100);});
    }

    @Test
    void testAuthorization () {
        assertThrowsExactly(AuthorizationException.class, () -> {as.createAuction(phone, s2);});
    }

    @Test
    void testNotOwner () {
        Seller no_one = null;
        Item ipad = new Electronics("phone", "Flagship", 36, no_one, "apple", "iphone 16");
        assertThrowsExactly(NotOwnerException.class, () -> {as.createAuction(ipad, s1);});

    }
}

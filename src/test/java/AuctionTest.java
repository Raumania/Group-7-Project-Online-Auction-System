import auction_system.server.exception.*;
import auction_system.server.model.*;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

public class AuctionTest {
    User b1 = new User("leeduc", "leduc2703", "abc@gmail.com", Collections.singleton(UserRole.BIDDER));
    User s1 = new User("chuong", "chuongw", "cde@gmail.com", Collections.singleton(UserRole.SELLER));
    User s2 = new User("ducbanh", "ducbanh", "fgh@gmail.com", Collections.singleton(UserRole.SELLER));

    // CẬP NHẬT: Khởi tạo Electronics không còn startingPrice
    Item phone = new Electronics("phone", "Flagship", s1, "apple", "iphone 16");

    // CẬP NHẬT: Khởi tạo Auction có thêm startingPrice (dùng constructor 3 tham số)
    Auction auction = new Auction(phone, s1, Double.MIN_VALUE);

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

        assertThrowsExactly(InvalidBidException.class, () -> {
            auction.placeBid(b1, 35);
        });
    }

    @Test
    void testAuctionClosed () {
        auction.placeBid(b1, 36);
        auction.closeAuction();

        assertThrowsExactly(StatusException.class, () -> {
            auction.placeBid(b1, 37);
        });
    }

    @Test
    void testAuthentication () {
        User no_one = null;

        assertThrowsExactly(NullPointerException.class, () -> {
            auction.placeBid(no_one, 100);
        });
    }

    @Test
    void testAuthorization () {
        // CẬP NHẬT: Thêm startingPrice vào constructor
        // Đổi s2 thành b1 để test bắt đúng AuthorizationException (vì b1 không có role SELLER)
        assertThrowsExactly(AuthorizationException.class, () -> {
            new Auction(phone, b1, 10.0);
        });
    }

    @Test
    void testItemInfo () {
        User no_one = null;

        // CẬP NHẬT: Xóa tham số startingPrice (36) khỏi constructor Electronics
        assertThrowsExactly(ItemInformationException.class, () -> {
            new Electronics("phone", "Flagship", no_one, "apple", "iphone 16");
        });
    }
}
import auction_system.server.model.Auction;
import auction_system.common.dto.AuctionDTO;
import auction_system.common.enums.ItemType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AuctionTest {

    @Test
    void testAuctionCreationFromDTO() {
        AuctionDTO dto = new AuctionDTO();
        dto.setName("Test Auction");
        dto.setDescription("A test auction.");
        dto.setType(ItemType.ELECTRONICS);
        dto.setSellerId(1);
        dto.setStartingPrice(100.0);
        dto.setStartTime(LocalDateTime.now());
        dto.setEndTime(LocalDateTime.now().plusDays(1));

        Auction auction = new Auction(dto);

        assertEquals("Test Auction", auction.getName());
        assertEquals("A test auction.", auction.getDescription());
        assertEquals(ItemType.ELECTRONICS, auction.getType());
        assertEquals(1, auction.getSellerId());
        assertEquals(100.0, auction.getStartingPrice());
    }
}
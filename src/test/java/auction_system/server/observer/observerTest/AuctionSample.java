package auction_system.server.observer.observerTest;

import auction_system.common.dto.AuctionDTO;
import auction_system.common.enums.ItemType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuctionSample {
    public static List<AuctionDTO> createSampleAuctions() {
        List<AuctionDTO> auctions = new ArrayList<>();
        //0
        auctions.add(new AuctionDTO(
                "iPhone 15 Pro Max",
                "Điện thoại mới 99%, full box",
                ItemType.ELECTRONICS,
                1,
                25000000,
                LocalDateTime.now().plusHours(1),
                LocalDateTime.now().plusDays(3)
        ));
        //1
        auctions.add(new AuctionDTO(
                "MacBook Air M2",
                "Laptop dùng cho học tập và lập trình",
                ItemType.ELECTRONICS,
                2,
                18000000,
                LocalDateTime.now().minusHours(2),
                LocalDateTime.now().plusDays(2)
        ));
        //2
        auctions.add(new AuctionDTO(
                "Ghế Gaming RGB",
                "Ghế gaming công thái học",
                ItemType.ELECTRONICS,
                3,
                3500000,
                LocalDateTime.now().plusHours(5),
                LocalDateTime.now().plusDays(5)
        ));
        //3
        auctions.add(new AuctionDTO(
                "Nike Air Force 1",
                "Giày size 42, còn mới",
                ItemType.ART,
                4,
                1200000,
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().minusHours(2)
        ));
        //4
        auctions.add(new AuctionDTO(
                "Bàn phím cơ Keychron K8",
                "Switch brown, LED trắng",
                ItemType.ELECTRONICS,
                5,
                1500000,
                LocalDateTime.now(),
                LocalDateTime.now().plusDays(1)
        ));
        //5
        auctions.add(new AuctionDTO(
                "Sách Clean Code",
                "Sách lập trình nổi tiếng của Robert C. Martin",
                ItemType.ART,
                6,
                200000,
                LocalDateTime.now().plusMinutes(30),
                LocalDateTime.now().plusDays(7)
        ));
        //6
        auctions.add(new AuctionDTO(
                "Xe đạp thể thao",
                "Xe đạp địa hình màu đen",
                ItemType.VEHICLE,
                7,
                4500000,
                LocalDateTime.now().minusHours(5),
                LocalDateTime.now().plusHours(10)
        ));

        return auctions;
    }
}

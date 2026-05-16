package auction_system.server.common.protocol;

import auction_system.server.model.ItemType;
import java.time.LocalDateTime;

public class CreateAuctionRequest {
    private String sellerId;
    private ItemType type;

    // Thông tin chung của sản phẩm
    private String name;
    private String description;
    private double startingPrice;

    // CẬP NHẬT: Thêm thời gian bắt đầu và kết thúc đấu giá
    private LocalDateTime startTime;
    private LocalDateTime endTime;

    /*
        LƯU Ý: Các trường brand, model, artist, year, material, licensePlate
        đã được loại bỏ để khớp với cấu trúc tối giản của hệ thống.
    */

    public CreateAuctionRequest() {}

    // Getters & Setters
    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }

    public ItemType getItemType() { return type; }
    public void setItemType(ItemType type) { this.type = type; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getStartingPrice() { return startingPrice; }
    public void setStartingPrice(double startingPrice) { this.startingPrice = startingPrice; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
}

package auction_system.server.common.protocol;

import java.time.LocalDateTime;

public class CreateAuctionRequest {
    private String sellerId;
    private String itemType; // "ELECTRONICS", "ART", "VEHICLE"

    // Thông tin chung của sản phẩm
    private String name;
    private String description;
    private double startingPrice;

    // CẬP NHẬT: Thêm thời gian bắt đầu và kết thúc đấu giá
    private LocalDateTime startingTime;
    private LocalDateTime endingTime;

    /*
        LƯU Ý: Các trường brand, model, artist, year, material, licensePlate
        đã được loại bỏ để khớp với cấu trúc tối giản của hệ thống.
    */

    public CreateAuctionRequest() {}

    // Getters & Setters
    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }

    public String getItemType() { return itemType; }
    public void setItemType(String itemType) { this.itemType = itemType; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getStartingPrice() { return startingPrice; }
    public void setStartingPrice(double startingPrice) { this.startingPrice = startingPrice; }

    public LocalDateTime getStartingTime() { return startingTime; }
    public void setStartingTime(LocalDateTime startingTime) { this.startingTime = startingTime; }

    public LocalDateTime getEndingTime() { return endingTime; }
    public void setEndingTime(LocalDateTime endingTime) { this.endingTime = endingTime; }
}
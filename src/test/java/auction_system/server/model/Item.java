package auction_system.server.model;

import auction_system.common.dto.AuctionDTO;
import auction_system.common.enums.ItemType;

import java.time.LocalDateTime;

/**
 * Lớp trừu tượng đại diện cho một sản phẩm đấu giá chung.
 * Chỉ chứa dữ liệu, không chứa logic validate (kiểm tra rỗng, kiểm tra thời gian).
 */
public abstract class Item extends Entity {
    protected String name;
    protected String description;
    protected ItemType type;
    LocalDateTime startTime;
    LocalDateTime endTime;

    public Item(int id, String name, String description, ItemType type, LocalDateTime startTime, LocalDateTime endTime) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.type = type;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public Item(String name, String description, ItemType type, LocalDateTime startTime, LocalDateTime endTime) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public static Item createFromDTO(AuctionDTO dto) {
        Item item;
        switch (dto.getType()) {
            case ART:
                item = new Art(dto.getName(), dto.getDescription(), dto.getStartTime(), dto.getEndTime());
                break;
            case ELECTRONICS:
                item = new Electronics(dto.getName(), dto.getDescription(), dto.getStartTime(), dto.getEndTime());
                break;
            case VEHICLE:
                item = new Vehicle(dto.getName(), dto.getDescription(), dto.getStartTime(), dto.getEndTime());
                break;
            default:
                throw new IllegalArgumentException("Invalid item type: " + dto.getType());
        }
        return item;
    }

    // =========================================
    // GETTERS & SETTERS (Đã bổ sung đầy đủ)
    // =========================================

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ItemType getType() {
        return type;
    }

    public void setType(ItemType type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return "Item{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", type=" + type +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                '}';
    }
}
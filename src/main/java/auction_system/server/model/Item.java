package auction_system.server.model;

import auction_system.server.exception.AuthorizationException;
import auction_system.server.exception.ItemInformationException;
import java.time.LocalDateTime; // Import để xử lý thời gian

public abstract class Item extends Entity {
    protected String name;
    protected String description;
    protected User owner;
    protected ItemType type;

    // CẬP NHẬT: Thêm hai trường thời gian
    protected LocalDateTime startingTime;
    protected LocalDateTime endingTime;

    /*
        Constructor cập nhật để nhận thêm thời gian bắt đầu và kết thúc
    */
    public Item(String name, String description, User owner, ItemType type, LocalDateTime startingTime, LocalDateTime endingTime) {
        super();

        // Kiểm tra dữ liệu cơ bản
        if (name == null || name.trim().isEmpty()) {
            throw new ItemInformationException("Item name cannot be null or empty");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new ItemInformationException("Item description cannot be null or empty");
        }
        if (owner == null) {
            throw new ItemInformationException("Owner cannot be null");
        }

        // Kiểm tra quyền Seller
        if (!owner.hasRole(UserRole.SELLER)) {
            throw new AuthorizationException("Owner must have SELLER role");
        }

        if (type == null) {
            throw new NullPointerException("Item type cannot be null");
        }

        // CẬP NHẬT: Kiểm tra tính hợp lệ của thời gian
        if (startingTime == null || endingTime == null) {
            throw new ItemInformationException("Starting and ending time cannot be null");
        }
        if (endingTime.isBefore(startingTime)) {
            throw new ItemInformationException("Ending time must be after starting time");
        }

        this.name = name;
        this.description = description;
        this.owner = owner;
        this.type = type;
        this.startingTime = startingTime;
        this.endingTime = endingTime;

        // Id sẽ được gán sau khi lưu vào DB (AUTO_INCREMENT)
        this.id = null;
    }

    // --- Getters ---
    public String getName() { return name; }
    public String getDescription() { return description; }
    public User getOwner() { return owner; }
    public ItemType getType() { return type; }
    public LocalDateTime getStartingTime() { return startingTime; }
    public LocalDateTime getEndingTime() { return endingTime; }

    // --- Setters ---
    // (Cần thiết khi ItemDAO đọc dữ liệu từ DB và gán ngược lại vào Object)
    public void setStartingTime(LocalDateTime startingTime) {
        this.startingTime = startingTime;
    }

    public void setEndingTime(LocalDateTime endingTime) {
        this.endingTime = endingTime;
    }

    @Override
    public String toString() {
        return "Item{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", startingTime=" + startingTime +
                ", endingTime=" + endingTime +
                '}';
    }
}
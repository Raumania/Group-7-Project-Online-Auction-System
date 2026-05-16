package auction_system.server.model;

import auction_system.server.exception.AuthorizationException;
import auction_system.server.exception.ItemInformationException;
import java.time.LocalDateTime;

public abstract class Item extends Entity {
    protected String name;
    protected String description;
    protected ItemType type;

    protected LocalDateTime startTime;
    protected LocalDateTime endTime;

    public Item(String name, String description, ItemType type, LocalDateTime startTime, LocalDateTime endTime) {
        super();

        if (name == null || name.trim().isEmpty()) {
            throw new ItemInformationException("Item name cannot be null or empty");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new ItemInformationException("Item description cannot be null or empty");
        }

        if (type == null) {
            throw new NullPointerException("Item type cannot be null");
        }

        if (startTime == null || endTime == null) {
            throw new ItemInformationException("Starting and ending time cannot be null");
        }
        if (endTime.isBefore(startTime)) {
            throw new ItemInformationException("Ending time must be after starting time");
        }

        this.name = name;
        this.description = description;
        this.type = type;
        this.startTime = startTime;
        this.endTime = endTime;

        this.id = null;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public ItemType getType() { return type; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    @Override
    public String toString() {
        return "Item{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                '}';
    }
}
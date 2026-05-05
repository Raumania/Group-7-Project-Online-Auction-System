package auction_system.server.model;

import auction_system.server.util.IdGenerator;

public abstract class Entity {
    protected String id;
    protected long createdAt;

    public Entity() {
        this.createdAt = System.currentTimeMillis();
        this.id = IdGenerator.generateEmtityId();
    }

    public String getId() {
        return id;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
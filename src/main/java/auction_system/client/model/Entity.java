package auction_system.client.model;

public abstract class Entity {
    protected String id;
    protected long createdAt;

    public String getId() {
        return id;
    }
    public void setId(String id){
        this.id=id;
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
package auction_system.server.model;

public class CreateAuctionData {
    private Item item;
    private User seller;
    public void setItem(Item item) { this.item = item; }
    public void setSeller(User seller) { this.seller = seller; }
    public Item getItem() { return item; }
    public User getSeller() { return seller; }
}
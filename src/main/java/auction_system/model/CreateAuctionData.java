package auction_system.model;

public class CreateAuctionData {
    private Item item;
    private Seller seller;
    public void setItem(Item item) { this.item = item; }
    public void setSeller(Seller seller) { this.seller = seller; }
    public Item getItem() { return item; }
    public Seller getSeller() { return seller; }
}
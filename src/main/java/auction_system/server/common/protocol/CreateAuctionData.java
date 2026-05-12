package auction_system.server.common.protocol;

import auction_system.server.model.Item;
import auction_system.server.model.Seller;

public class CreateAuctionData {
    private Item item;
    private Seller seller;

    public CreateAuctionData() {}

    public CreateAuctionData(Item item, Seller seller) {
        this.item = item;
        this.seller = seller;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public Seller getSeller() {
        return seller;
    }

    public void setSeller(Seller seller) {
        this.seller = seller;
    }
}
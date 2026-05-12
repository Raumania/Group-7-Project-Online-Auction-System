package auction_system.server.common.protocol;

import auction_system.server.model.Item;
import auction_system.server.model.User;

public class CreateAuctionData {
    private Item item;
    private User seller;

    public CreateAuctionData() {}

    public CreateAuctionData(Item item, User seller) {
        this.item = item;
        this.seller = seller;
    }

    public Item getItem() {
        return item;
    }

    public void setItem(Item item) {
        this.item = item;
    }

    public User getSeller() {
        return seller;
    }

    public void setSeller(User seller) {
        this.seller = seller;
    }
}
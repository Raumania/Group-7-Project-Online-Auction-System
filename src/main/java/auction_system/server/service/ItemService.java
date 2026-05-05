package auction_system.server.service;

import auction_system.server.factory.ItemFactory;
import auction_system.server.model.Item;
import auction_system.server.model.ItemType;
import auction_system.server.model.Seller;

import java.util.List;

public class ItemService {

    private ItemManager itemManager;

    public ItemService() {
        this.itemManager = ItemManager.getInstance();
    }

    public Item createItem(ItemType type, String name, String description, double startingPrice, Seller owner, Object... extraParams) {
        Item item = ItemFactory.createItem(type, name, description, startingPrice, owner, extraParams);
        itemManager.addItem(item);
        return item;
    }

    public void addItem(Item item) {
        itemManager.addItem(item);
    }

    public Item findItemById(String id) {
        Item item = itemManager.findItemById(id);

        if (item == null) {
            throw new RuntimeException("Item not found with id: " + id);
        }

        return item;
    }

    public List<Item> getAllItems() {
        return itemManager.getAllItems();
    }

    public List<Item> getItemsByType(ItemType type) {
        return itemManager.getItemsByType(type);
    }

    public List<Item> getItemsBySeller(Seller seller) {
        return itemManager.getItemsBySeller(seller);
    }

    public boolean removeItem(String id) {
        return itemManager.removeItemById(id);
    }
}
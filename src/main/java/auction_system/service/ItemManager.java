package auction_system.service;

import auction_system.model.Item;
import auction_system.model.ItemType;
import auction_system.model.Seller;

import java.util.ArrayList;
import java.util.List;

public class ItemManager {

    private static ItemManager instance;
    private List<Item> items;

    private ItemManager() {
        items = new ArrayList<>();
    }

    public static ItemManager getInstance() {
        if (instance == null) {
            instance = new ItemManager();
        }
        return instance;
    }

    public void addItem(Item item) {
        if (item == null) {
            throw new RuntimeException("Item cannot be null");
        }
        items.add(item);
    }

    public Item findItemById(String id) {
        if (id == null || id.trim().isEmpty()) {
            throw new RuntimeException("Item id cannot be null or empty");
        }

        for (Item item : items) {
            if (item.getId().equals(id)) {
                return item;
            }
        }
        return null;
    }

    public List<Item> getAllItems() {
        return new ArrayList<>(items);
    }

    public List<Item> getItemsByType(ItemType type) {
        List<Item> result = new ArrayList<>();

        if (type == null) {
            throw new RuntimeException("Item type cannot be null");
        }

        for (Item item : items) {
            if (item.getType() == type) {
                result.add(item);
            }
        }

        return result;
    }

    public List<Item> getItemsBySeller(Seller seller) {
        List<Item> result = new ArrayList<>();

        if (seller == null) {
            throw new RuntimeException("Seller cannot be null");
        }

        for (Item item : items) {
            if (item.getOwner().equals(seller)) {
                result.add(item);
            }
        }

        return result;
    }

    public boolean removeItemById(String id) {
        Item item = findItemById(id);

        if (item != null) {
            items.remove(item);
            return true;
        }

        return false;
    }
}
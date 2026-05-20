package auction_system.server.exception.serviceException.Item;

public class ItemNotFoundException extends RuntimeException {
    public ItemNotFoundException(int itemId) {
        super("Item not found: " + itemId);
    }
}
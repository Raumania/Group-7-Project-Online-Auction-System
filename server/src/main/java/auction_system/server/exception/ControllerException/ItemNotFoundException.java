package auction_system.server.exception.ControllerException;

public class ItemNotFoundException extends RuntimeException {
    public ItemNotFoundException(int itemId) {
        super("Item not found: " + itemId);
    }
}
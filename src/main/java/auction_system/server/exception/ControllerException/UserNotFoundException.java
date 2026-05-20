package auction_system.server.exception.ControllerException;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String username) {
        super("User not found: " + username);
    }
    public UserNotFoundException(int userId) {
        super("User not found with id: " + userId);
    }
}
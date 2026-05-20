package auction_system.server.exception.serviceException.User;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(String username) {
        super("User not found: " + username);
    }
    public UserNotFoundException(int userId) {
        super("User not found with id: " + userId);
    }
}
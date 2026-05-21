package auction_system.server.exception.daoException;

public class deletingException extends RuntimeException {
    public deletingException(String message) {
        super(message);
    }
    public deletingException(String message, Throwable cause) {
        super(message, cause);
    }
}

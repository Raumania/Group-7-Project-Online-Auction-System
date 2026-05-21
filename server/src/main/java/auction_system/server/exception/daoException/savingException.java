package auction_system.server.exception.daoException;

public class savingException extends RuntimeException {
    public savingException(String message) {
        super(message);
    }
    public savingException(String message, Throwable cause) {
        super(message, cause);
    }
}

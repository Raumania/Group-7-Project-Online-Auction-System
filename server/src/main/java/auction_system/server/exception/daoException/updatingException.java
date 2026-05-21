package auction_system.server.exception.daoException;

public class updatingException extends RuntimeException {
    
    /**
     * Constructor for creating an exception with a custom message.
     * @param message The detail message.
     */
    public updatingException(String message) {
        super(message);
    }

    /**
     * Constructor for creating an exception that wraps another exception.
     * This is useful for preserving the original stack trace.
     * @param message The detail message.
     * @param cause The cause (which is saved for later retrieval by the getCause() method).
     */
    public updatingException(String message, Throwable cause) {
        super(message, cause);
    }
}

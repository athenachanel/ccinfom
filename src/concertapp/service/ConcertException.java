package concertapp.service;

/** Thrown with a message that is safe to show the user (bad input, not found, database trouble). */
public class ConcertException extends Exception {

    private static final long serialVersionUID = 1L;

    public ConcertException(String message) {
        super(message);
    }

    public ConcertException(String message, Throwable cause) {
        super(message, cause);
    }
}

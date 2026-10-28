/**
 * [CO3]: User-defined exception thrown when registration or profile data is invalid.
 */
public class InvalidProfileException extends Exception {
    public InvalidProfileException(String message) {
        super(message);
    }
}

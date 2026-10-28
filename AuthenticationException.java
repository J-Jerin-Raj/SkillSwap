/**
 * [CO3]: User-defined exception thrown when user credentials fail verification.
 */
public class AuthenticationException extends Exception {
    public AuthenticationException(String message) {
        super(message);
    }
}

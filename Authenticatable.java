/**
 * [CO2]: Interface defining credential authentication operations.
 */
public interface Authenticatable {
    boolean verifyPassword(String rawPassword);
}

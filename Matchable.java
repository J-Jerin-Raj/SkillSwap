import java.util.Set;

/**
 * [CO2]: Interface defining the contract for entities capable of skill matching.
 */
public interface Matchable {
    Set<String> getOffered();
    Set<String> getWanted();
}

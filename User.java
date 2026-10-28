import java.util.*;

/**
 * [CO2]: User entity inheriting AbstractAccount and implementing Matchable.
 * Demonstrates Inheritance, Polymorphism, and Defensive Encapsulation.
 */
public class User extends AbstractAccount implements Matchable {
    private final Set<String> offered = new LinkedHashSet<>();
    private final Set<String> wanted  = new LinkedHashSet<>();

    public User(String name, String password, String contact,
                Collection<String> offered, Collection<String> wanted) {
        super(name, password, contact);
        if (offered != null) {
            for (String s : offered) {
                String t = s.trim().toLowerCase();
                if (!t.isEmpty()) this.offered.add(t);
            }
        }
        if (wanted != null) {
            for (String s : wanted) {
                String t = s.trim().toLowerCase();
                if (!t.isEmpty()) this.wanted.add(t);
            }
        }
    }

    /** Overloaded constructor defaulting password for backwards compatibility */
    public User(String name, String contact, Collection<String> offered, Collection<String> wanted) {
        this(name, "password123", contact, offered, wanted);
    }

    @Override
    public Set<String> getOffered() {
        return Collections.unmodifiableSet(offered);
    }

    @Override
    public Set<String> getWanted() {
        return Collections.unmodifiableSet(wanted);
    }

    @Override
    public String getAccountSummary() {
        return "User: " + name + " | Contact: " + contact +
               " | Offers: " + offered.size() + " skills | Wants: " + wanted.size() + " skills";
    }
}

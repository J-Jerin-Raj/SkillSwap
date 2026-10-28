import java.util.*;

/** OOP: encapsulated user profile with Offered and Requested skills. */
public class User {
    private final String name;
    private final String contact;
    private final Set<String> offered = new LinkedHashSet<>();
    private final Set<String> wanted  = new LinkedHashSet<>();

    public User(String name, String contact, Collection<String> offered, Collection<String> wanted) {
        this.name = name == null ? "" : name.trim();
        this.contact = contact == null ? "" : contact.trim();
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

    public String getName()          { return name; }
    public String getContact()       { return contact; }
    public Set<String> getOffered()  { return Collections.unmodifiableSet(offered); }
    public Set<String> getWanted()   { return Collections.unmodifiableSet(wanted); }
}

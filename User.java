import java.util.*;

/** OOP: encapsulated user profile with Offered and Requested skills. */
public class User {
    private final String name;
    private final String contact;
    private final Set<String> offered = new HashSet<>();
    private final Set<String> wanted  = new HashSet<>();

    public User(String name, String contact, Collection<String> offered, Collection<String> wanted) {
        this.name = name;
        this.contact = contact;
        for (String s : offered) this.offered.add(s.trim().toLowerCase());
        for (String s : wanted)  this.wanted.add(s.trim().toLowerCase());
    }

    public String getName()          { return name; }
    public String getContact()       { return contact; }
    public Set<String> getOffered()  { return offered; }
    public Set<String> getWanted()   { return wanted; }
}

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Bidirectional matching engine using in-memory Java Collections. */
public class MatchEngine {
    private final Map<String, User> users = new ConcurrentHashMap<>();

    public void addUser(User u)       { users.put(u.getName().toLowerCase(), u); }
    public User getUser(String name)  { return users.get(name.trim().toLowerCase()); }

    /** Stages 2-4: cross-comparison, reciprocal check, score & rank. */
    public List<MatchResult> findMatches(User me) {
        List<MatchResult> results = new ArrayList<>();

        for (User other : users.values()) {
            if (other == me) continue;

            // Stage 2: what they offer that I want
            Set<String> theyTeachMe = new HashSet<>(me.getWanted());
            theyTeachMe.retainAll(other.getOffered());

            // Stage 3: what I offer that they want (reciprocal check)
            Set<String> iTeachThem = new HashSet<>(other.getWanted());
            iTeachThem.retainAll(me.getOffered());

            if (theyTeachMe.isEmpty() || iTeachThem.isEmpty()) continue; // must work both ways

            // Stage 4: score = matched skills / total skills wanted by both
            int totalWanted = me.getWanted().size() + other.getWanted().size();
            int score = (theyTeachMe.size() + iTeachThem.size()) * 100 / totalWanted;
            results.add(new MatchResult(other, theyTeachMe, iTeachThem, score));
        }
        Collections.sort(results);
        return results;
    }
}

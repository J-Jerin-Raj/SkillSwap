import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** Bidirectional matching engine and message router using in-memory Java Collections. */
public class MatchEngine {
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<String, List<String>> inboxes = new ConcurrentHashMap<>();

    public void addUser(User u) {
        if (u != null && u.getName() != null && !u.getName().trim().isEmpty()) {
            users.put(u.getName().trim().toLowerCase(), u);
        }
    }

    public User getUser(String name) {
        if (name == null) return null;
        return users.get(name.trim().toLowerCase());
    }

    public Collection<User> getAllUsers() {
        return users.values();
    }

    /** Stages 2-4: cross-comparison, reciprocal check, score & rank. */
    public List<MatchResult> findMatches(User me) {
        List<MatchResult> results = new ArrayList<>();

        for (User other : users.values()) {
            if (other.getName().equalsIgnoreCase(me.getName())) continue;

            // Stage 2: what they offer that I want
            Set<String> theyTeachMe = new HashSet<>(me.getWanted());
            theyTeachMe.retainAll(other.getOffered());

            // Stage 3: what I offer that they want (reciprocal check)
            Set<String> iTeachThem = new HashSet<>(other.getWanted());
            iTeachThem.retainAll(me.getOffered());

            if (theyTeachMe.isEmpty() || iTeachThem.isEmpty()) continue; // must work both ways

            // Stage 4: score = matched skills / total skills wanted by both
            int totalWanted = me.getWanted().size() + other.getWanted().size();
            int score = totalWanted == 0 ? 0 : (theyTeachMe.size() + iTeachThem.size()) * 100 / totalWanted;
            results.add(new MatchResult(other, theyTeachMe, iTeachThem, score));
        }
        Collections.sort(results);
        return results;
    }

    /** Find people who offer skills that I want to learn (Mentors / Recommendations). */
    public Map<User, Set<String>> findUsersWhoTeachWhatIWant(User me) {
        Map<User, Set<String>> map = new LinkedHashMap<>();
        for (User other : users.values()) {
            if (other.getName().equalsIgnoreCase(me.getName())) continue;
            Set<String> overlap = new HashSet<>(me.getWanted());
            overlap.retainAll(other.getOffered());
            if (!overlap.isEmpty()) {
                map.put(other, overlap);
            }
        }
        return map;
    }

    /** Find people who want the skills I offer (Learners / Demand). */
    public Map<User, Set<String>> findUsersWhoWantMySkills(User me) {
        Map<User, Set<String>> map = new LinkedHashMap<>();
        for (User other : users.values()) {
            if (other.getName().equalsIgnoreCase(me.getName())) continue;
            Set<String> overlap = new HashSet<>(other.getWanted());
            overlap.retainAll(me.getOffered());
            if (!overlap.isEmpty()) {
                map.put(other, overlap);
            }
        }
        return map;
    }

    /** Store a direct message between users. */
    public boolean addMessage(String from, String to, String content) {
        if (to == null || from == null || content == null) return false;
        User recipient = getUser(to);
        if (recipient == null) return false;
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        inboxes.computeIfAbsent(to.trim().toLowerCase(), k -> new CopyOnWriteArrayList<>())
               .add("[" + time + "] From " + from.trim() + ": " + content.trim());
        return true;
    }

    /** Retrieve all messages for a user. */
    public List<String> getMessages(String user) {
        if (user == null) return Collections.emptyList();
        return inboxes.getOrDefault(user.trim().toLowerCase(), Collections.emptyList());
    }
}

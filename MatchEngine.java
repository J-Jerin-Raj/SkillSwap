import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * [CO3 / CO4]: Thread-safe matching engine and message router.
 * Demonstrates Concurrent Collections, Lambda/Stream transformations,
 * and User-defined Exception handling.
 */
public class MatchEngine {
    // [CO4]: Concurrent Collections Framework
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

    // [CO3]: User-defined exception handling for authentication
    public User authenticate(String name, String password) throws AuthenticationException {
        if (name == null || name.trim().isEmpty()) {
            throw new AuthenticationException("Username cannot be empty.");
        }
        User u = getUser(name);
        if (u == null) {
            throw new AuthenticationException("User '" + name.trim() + "' not found. Please Sign Up first.");
        }
        if (!u.verifyPassword(password)) {
            throw new AuthenticationException("Incorrect password for user '" + name.trim() + "'.");
        }
        return u;
    }

    // [CO3]: User-defined exception handling for profile registration
    public User registerUser(String name, String password, String contact,
                             Collection<String> offered, Collection<String> wanted)
            throws InvalidProfileException {
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidProfileException("Username cannot be empty.");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new InvalidProfileException("Password cannot be empty.");
        }
        User existing = getUser(name);
        if (existing != null && !existing.verifyPassword(password)) {
            throw new InvalidProfileException("User '" + name.trim() + "' already exists. Incorrect password to update profile.");
        }
        User u = new User(name, password, contact, offered, wanted);
        addUser(u);
        return u;
    }

    /**
     * [CO4]: Computes all compatible matches ranked by affinity score using Java Collections.
     */
    public List<MatchResult> computeAllMatches(User me) {
        List<MatchResult> results = new ArrayList<>();
        if (me == null) return results;

        for (User other : users.values()) {
            if (other.getName().equalsIgnoreCase(me.getName())) continue;

            Set<String> theyTeachMe = new HashSet<>(me.getWanted());
            theyTeachMe.retainAll(other.getOffered());

            Set<String> iTeachThem = new HashSet<>(other.getWanted());
            iTeachThem.retainAll(me.getOffered());

            Set<String> sharedInterests = new HashSet<>(me.getWanted());
            sharedInterests.retainAll(other.getWanted());

            boolean hasReciprocal = !theyTeachMe.isEmpty() && !iTeachThem.isEmpty();
            boolean hasMentor = !theyTeachMe.isEmpty();
            boolean hasLearner = !iTeachThem.isEmpty();
            boolean hasShared = !sharedInterests.isEmpty();

            if (!hasReciprocal && !hasMentor && !hasLearner && !hasShared) {
                continue;
            }

            int score;
            String type;

            if (hasReciprocal) {
                type = "RECIPROCAL";
                double myCov = (double) theyTeachMe.size() / Math.max(1, me.getWanted().size());
                double theirCov = (double) iTeachThem.size() / Math.max(1, other.getWanted().size());
                int base = 60;
                int coveragePoints = (int) Math.round(((myCov + theirCov) / 2.0) * 32.0);
                int sharedBonus = Math.min(8, sharedInterests.size() * 4);
                score = Math.min(100, base + coveragePoints + sharedBonus);
            } else if (hasMentor) {
                type = "MENTOR";
                double myCov = (double) theyTeachMe.size() / Math.max(1, me.getWanted().size());
                int base = 40;
                int covPoints = (int) Math.round(myCov * 38.0);
                int sharedBonus = Math.min(7, sharedInterests.size() * 3);
                score = Math.min(85, base + covPoints + sharedBonus);
            } else if (hasLearner) {
                type = "LEARNER";
                double theirCov = (double) iTeachThem.size() / Math.max(1, other.getWanted().size());
                int base = 35;
                int covPoints = (int) Math.round(theirCov * 35.0);
                int sharedBonus = Math.min(5, sharedInterests.size() * 2);
                score = Math.min(75, base + covPoints + sharedBonus);
            } else {
                type = "PEER";
                double jaccard = (double) sharedInterests.size() /
                        Math.max(1, me.getWanted().size() + other.getWanted().size() - sharedInterests.size());
                score = Math.min(60, (int) Math.round(25 + jaccard * 35.0));
            }

            results.add(new MatchResult(other, theyTeachMe, iTeachThem, sharedInterests, score, type));
        }

        Collections.sort(results);
        return results;
    }

    /** [CO4]: Lambda & Stream filter for specific skill search */
    public List<User> searchUsersBySkill(String skill) {
        if (skill == null || skill.trim().isEmpty()) return Collections.emptyList();
        String query = skill.trim().toLowerCase();
        return users.values().stream()
                .filter(u -> u.getOffered().contains(query) || u.getWanted().contains(query))
                .collect(Collectors.toList());
    }

    public boolean addMessage(String from, String to, String content) {
        if (to == null || from == null || content == null) return false;
        User recipient = getUser(to);
        if (recipient == null) return false;
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        String cleanFrom = from.trim();
        String cleanTo = to.trim();
        String cleanContent = content.trim();

        // Recipient log: Received Message
        String recvEntry = "[" + time + "] From " + cleanFrom + ": " + cleanContent + " (Received Message)";
        inboxes.computeIfAbsent(cleanTo.toLowerCase(), k -> new CopyOnWriteArrayList<>())
               .add(recvEntry);

        // Sender log: Sent Message
        String sentEntry = "[" + time + "] To " + cleanTo + ": " + cleanContent + " (Sent Message)";
        inboxes.computeIfAbsent(cleanFrom.toLowerCase(), k -> new CopyOnWriteArrayList<>())
               .add(sentEntry);

        return true;
    }

    public List<String> getMessages(String user) {
        if (user == null) return Collections.emptyList();
        return inboxes.getOrDefault(user.trim().toLowerCase(), Collections.emptyList());
    }
}

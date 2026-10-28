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

    /**
     * Computes all compatible matches ranked by calculated affinity score.
     * Evaluates reciprocal matches, mentors, learners, and shared learning interests.
     */
    public List<MatchResult> computeAllMatches(User me) {
        List<MatchResult> results = new ArrayList<>();
        if (me == null) return results;

        for (User other : users.values()) {
            if (other.getName().equalsIgnoreCase(me.getName())) continue;

            // What they can teach me (my wanted ∩ their offered)
            Set<String> theyTeachMe = new HashSet<>(me.getWanted());
            theyTeachMe.retainAll(other.getOffered());

            // What I can teach them (their wanted ∩ my offered)
            Set<String> iTeachThem = new HashSet<>(other.getWanted());
            iTeachThem.retainAll(me.getOffered());

            // Shared learning interests (skills both want to learn)
            Set<String> sharedInterests = new HashSet<>(me.getWanted());
            sharedInterests.retainAll(other.getWanted());

            boolean hasReciprocal = !theyTeachMe.isEmpty() && !iTeachThem.isEmpty();
            boolean hasMentor = !theyTeachMe.isEmpty();
            boolean hasLearner = !iTeachThem.isEmpty();
            boolean hasShared = !sharedInterests.isEmpty();

            if (!hasReciprocal && !hasMentor && !hasLearner && !hasShared) {
                continue; // No compatibility overlap
            }

            int score;
            String type;

            if (hasReciprocal) {
                type = "RECIPROCAL";
                // Base reciprocal score + coverage ratio + shared interest bonus
                double myCov = (double) theyTeachMe.size() / Math.max(1, me.getWanted().size());
                double theirCov = (double) iTeachThem.size() / Math.max(1, other.getWanted().size());
                int base = 60;
                int coveragePoints = (int) Math.round(((myCov + theirCov) / 2.0) * 32.0);
                int sharedBonus = Math.min(8, sharedInterests.size() * 4);
                score = Math.min(100, base + coveragePoints + sharedBonus);
            } else if (hasMentor) {
                type = "MENTOR";
                // Mentor can teach me
                double myCov = (double) theyTeachMe.size() / Math.max(1, me.getWanted().size());
                int base = 40;
                int covPoints = (int) Math.round(myCov * 38.0);
                int sharedBonus = Math.min(7, sharedInterests.size() * 3);
                score = Math.min(85, base + covPoints + sharedBonus);
            } else if (hasLearner) {
                type = "LEARNER";
                // Learner wants to learn from me
                double theirCov = (double) iTeachThem.size() / Math.max(1, other.getWanted().size());
                int base = 35;
                int covPoints = (int) Math.round(theirCov * 35.0);
                int sharedBonus = Math.min(5, sharedInterests.size() * 2);
                score = Math.min(75, base + covPoints + sharedBonus);
            } else {
                type = "PEER";
                // Study buddies with shared interests
                double jaccard = (double) sharedInterests.size() /
                        Math.max(1, me.getWanted().size() + other.getWanted().size() - sharedInterests.size());
                score = Math.min(60, (int) Math.round(25 + jaccard * 35.0));
            }

            results.add(new MatchResult(other, theyTeachMe, iTeachThem, sharedInterests, score, type));
        }

        Collections.sort(results);
        return results;
    }

    /** Legacy helper: returns 100% two-way reciprocal matches. */
    public List<MatchResult> findMatches(User me) {
        List<MatchResult> all = computeAllMatches(me);
        List<MatchResult> reciprocal = new ArrayList<>();
        for (MatchResult r : all) {
            if ("RECIPROCAL".equals(r.matchType)) {
                reciprocal.add(r);
            }
        }
        return reciprocal;
    }

    /** Find people who offer skills that I want to learn (Mentors / Recommendations). */
    public Map<User, Set<String>> findUsersWhoTeachWhatIWant(User me) {
        Map<User, Set<String>> map = new LinkedHashMap<>();
        if (me == null) return map;
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
        if (me == null) return map;
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

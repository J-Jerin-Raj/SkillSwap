import java.util.Collections;
import java.util.Set;

/**
 * Ranked match card representing compatibility between two users.
 * Contains reciprocal overlap, mentorship overlap, shared interests, and affinity percentage.
 */
public class MatchResult implements Comparable<MatchResult> {
    public final User peer;
    public final Set<String> theyTeachMe;     // my wanted  ∩ their offered
    public final Set<String> iTeachThem;      // their wanted ∩ my offered
    public final Set<String> sharedInterests; // my wanted ∩ their wanted
    public final int score;                   // affinity score (0 - 100%)
    public final String matchType;            // "RECIPROCAL", "MENTOR", "LEARNER", "PEER"

    public MatchResult(User peer, Set<String> theyTeachMe, Set<String> iTeachThem, int score) {
        this(peer, theyTeachMe, iTeachThem, Collections.emptySet(), score, "RECIPROCAL");
    }

    public MatchResult(User peer, Set<String> theyTeachMe, Set<String> iTeachThem,
                       Set<String> sharedInterests, int score, String matchType) {
        this.peer = peer;
        this.theyTeachMe = theyTeachMe != null ? theyTeachMe : Collections.emptySet();
        this.iTeachThem = iTeachThem != null ? iTeachThem : Collections.emptySet();
        this.sharedInterests = sharedInterests != null ? sharedInterests : Collections.emptySet();
        this.score = Math.max(0, Math.min(100, score));
        this.matchType = matchType != null ? matchType : "RECIPROCAL";
    }

    public String getCompatibilityLabel() {
        if (score >= 90) return "Exceptional Match (" + score + "%)";
        if (score >= 75) return "Strong Match (" + score + "%)";
        if (score >= 50) return "Moderate Compatibility (" + score + "%)";
        return "Potential Connect (" + score + "%)";
    }

    @Override
    public int compareTo(MatchResult o) {
        return Integer.compare(o.score, score); // descending order
    }
}

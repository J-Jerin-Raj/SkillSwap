import java.util.Set;

/** One ranked match card: who, which skills each side teaches, and the score. */
public class MatchResult implements Comparable<MatchResult> {
    public final User peer;
    public final Set<String> theyTeachMe;   // my wanted  ∩ their offered
    public final Set<String> iTeachThem;    // their wanted ∩ my offered
    public final int score;                 // exchange-affinity %

    public MatchResult(User peer, Set<String> theyTeachMe, Set<String> iTeachThem, int score) {
        this.peer = peer;
        this.theyTeachMe = theyTeachMe;
        this.iTeachThem = iTeachThem;
        this.score = score;
    }

    @Override public int compareTo(MatchResult o) { return Integer.compare(o.score, score); } // high first
}

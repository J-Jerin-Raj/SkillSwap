import java.io.Serializable;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * [CO2 / CO4]: Encapsulates a multi-user group chat room identified by a unique 5-letter code.
 * Demonstrates Collections, Encapsulation, and Thread-Safe Message Routing.
 */
public class GroupChat implements Serializable {
    private final String code;
    private final Set<String> members = new LinkedHashSet<>();
    private final List<String> messageHistory = new CopyOnWriteArrayList<>();

    public GroupChat(String code, Collection<String> initialMembers) {
        this.code = (code == null || code.trim().isEmpty()) ? generateCode() : code.trim().toUpperCase();
        if (initialMembers != null) {
            for (String m : initialMembers) {
                if (m != null && !m.trim().isEmpty()) {
                    members.add(m.trim().toLowerCase());
                }
            }
        }
    }

    /** Generates a 5-character uppercase alphanumeric code (e.g. A8K2P) */
    public static String generateCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        Random r = new Random();
        StringBuilder sb = new StringBuilder(5);
        for (int i = 0; i < 5; i++) {
            sb.append(chars.charAt(r.nextInt(chars.length())));
        }
        return sb.toString();
    }

    public String getCode() {
        return code;
    }

    public synchronized boolean addMember(String username) {
        if (username == null || username.trim().isEmpty()) return false;
        return members.add(username.trim().toLowerCase());
    }

    public synchronized boolean hasMember(String username) {
        if (username == null) return false;
        return members.contains(username.trim().toLowerCase());
    }

    public synchronized Set<String> getMembers() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(members));
    }

    public void addMessage(String entry) {
        if (entry != null && !entry.trim().isEmpty()) {
            messageHistory.add(entry.trim());
        }
    }

    public List<String> getMessageHistory() {
        return Collections.unmodifiableList(messageHistory);
    }

    /** Returns formatted list of all other members except 'me' */
    public String getOtherMembersDisplay(String me) {
        List<String> others = new ArrayList<>();
        String myName = (me == null) ? "" : me.trim().toLowerCase();
        for (String m : members) {
            if (!m.equalsIgnoreCase(myName)) {
                String capitalized = Character.toUpperCase(m.charAt(0)) + (m.length() > 1 ? m.substring(1) : "");
                others.add(capitalized);
            }
        }
        if (others.isEmpty()) return "Peers";
        return String.join(", ", others);
    }
}

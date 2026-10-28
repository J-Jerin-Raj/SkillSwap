import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * [CO3 / CO4]: Thread-safe matching engine, message router, and group chat manager.
 * Demonstrates Concurrent Collections, Lambda/Stream transformations,
 * and User-defined Exception handling.
 */
public class MatchEngine {
    // [CO4]: Concurrent Collections Framework
    private final Map<String, User> users = new ConcurrentHashMap<>();
    private final Map<String, List<String>> inboxes = new ConcurrentHashMap<>();

    // Multi-user Group Chats indexed by 5-letter unique code
    private final Map<String, GroupChat> groupChats = new ConcurrentHashMap<>();
    // Tracks the latest active chat partner or room code per user
    private final Map<String, String> userActiveChatPartner = new ConcurrentHashMap<>();

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

    // =========================================================================
    // GROUP CHAT & 5-LETTER CODE MANAGEMENT
    // =========================================================================

    /**
     * Finds or creates a group chat for the given participants, assigning a unique 5-letter code.
     */
    public synchronized GroupChat getOrCreateChatRoom(String user1, Collection<String> otherUsers) {
        Set<String> memberSet = new LinkedHashSet<>();
        if (user1 != null && !user1.trim().isEmpty()) {
            memberSet.add(user1.trim().toLowerCase());
        }
        if (otherUsers != null) {
            for (String u : otherUsers) {
                if (u != null && !u.trim().isEmpty()) {
                    memberSet.add(u.trim().toLowerCase());
                }
            }
        }

        // Check if an existing room already has these exact members
        for (GroupChat gc : groupChats.values()) {
            if (gc.getMembers().equals(memberSet)) {
                return gc;
            }
        }

        // Generate unique 5-letter code
        String code;
        do {
            code = GroupChat.generateCode();
        } while (groupChats.containsKey(code));

        GroupChat chat = new GroupChat(code, memberSet);
        groupChats.put(code, chat);
        return chat;
    }

    public GroupChat getChatByCode(String code) {
        if (code == null) return null;
        return groupChats.get(code.trim().toUpperCase());
    }

    public synchronized GroupChat joinGroupChat(String username, String code) {
        GroupChat chat = getChatByCode(code);
        if (chat == null || username == null || username.trim().isEmpty()) return null;
        chat.addMember(username);
        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String joinMsg = "[" + time + "] " + username.trim() + " joined the chat [" + chat.getCode() + "]";
        chat.addMessage(joinMsg);

        // Store notification for all members
        for (String m : chat.getMembers()) {
            inboxes.computeIfAbsent(m.toLowerCase(), k -> new CopyOnWriteArrayList<>()).add(joinMsg);
        }
        userActiveChatPartner.put(username.trim().toLowerCase(), chat.getCode());
        return chat;
    }

    public List<GroupChat> getUserGroupChats(String username) {
        if (username == null || username.trim().isEmpty()) return Collections.emptyList();
        String u = username.trim().toLowerCase();
        return groupChats.values().stream()
                .filter(gc -> gc.hasMember(u))
                .collect(Collectors.toList());
    }

    /**
     * Sends a direct or multi-user message.
     * Supports:
     * - 'to' as a 5-letter room code (e.g. A8K2P)
     * - 'to' as multiple comma-separated users (e.g. "Cara, Nate")
     * - 'to' as a single user (e.g. "Alice")
     */
    public boolean addMessage(String from, String to, String content) {
        if (to == null || from == null || content == null) return false;
        String cleanFrom = from.trim();
        String cleanTo = to.trim();
        String cleanContent = content.trim();
        if (cleanFrom.isEmpty() || cleanTo.isEmpty() || cleanContent.isEmpty()) return false;

        String time = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        // Case 1: 'to' is an existing 5-letter room code
        GroupChat codeRoom = getChatByCode(cleanTo);
        if (codeRoom != null) {
            codeRoom.addMember(cleanFrom);
            String otherMembers = codeRoom.getOtherMembersDisplay(cleanFrom);
            String sentEntry = "[" + time + "] To " + otherMembers + ": " + cleanContent + " (Sent Message)";
            String recvEntry = "[" + time + "] From " + cleanFrom + ": " + cleanContent + " (Received Message)";

            codeRoom.addMessage(sentEntry);
            inboxes.computeIfAbsent(cleanFrom.toLowerCase(), k -> new CopyOnWriteArrayList<>()).add(sentEntry);

            for (String member : codeRoom.getMembers()) {
                if (!member.equalsIgnoreCase(cleanFrom)) {
                    inboxes.computeIfAbsent(member.toLowerCase(), k -> new CopyOnWriteArrayList<>()).add(recvEntry);
                    userActiveChatPartner.put(member.toLowerCase(), codeRoom.getCode());
                }
            }
            userActiveChatPartner.put(cleanFrom.toLowerCase(), codeRoom.getCode());
            return true;
        }

        // Case 2: 'to' is comma-separated users or single user
        List<String> recipients = Arrays.stream(cleanTo.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty() && !s.equalsIgnoreCase(cleanFrom))
                .collect(Collectors.toList());

        if (recipients.isEmpty()) return false;

        // Verify recipients exist (or at least one exists)
        boolean anyExists = recipients.stream().anyMatch(r -> getUser(r) != null);
        if (!anyExists) return false;

        // Get or create GroupChat for this exact conversation group
        GroupChat chat = getOrCreateChatRoom(cleanFrom, recipients);
        String otherMembersDisplay = chat.getOtherMembersDisplay(cleanFrom);

        // Sender log entry
        String sentEntry = "[" + time + "] To " + otherMembersDisplay + ": " + cleanContent + " (Sent Message)";
        inboxes.computeIfAbsent(cleanFrom.toLowerCase(), k -> new CopyOnWriteArrayList<>()).add(sentEntry);
        chat.addMessage(sentEntry);

        // Recipient log entry for each other participant
        String recvEntry = "[" + time + "] From " + cleanFrom + ": " + cleanContent + " (Received Message)";
        for (String rcp : recipients) {
            inboxes.computeIfAbsent(rcp.toLowerCase(), k -> new CopyOnWriteArrayList<>()).add(recvEntry);
            userActiveChatPartner.put(rcp.toLowerCase(), chat.getCode());
        }

        userActiveChatPartner.put(cleanFrom.toLowerCase(), chat.getCode());
        return true;
    }

    /**
     * Generates the header: === CHAT WITH <NAME> [CODE] (<count> message(s)) ===
     */
    public String getChatHeader(String username, String partnerOrCode, int count) {
        if (username == null) return "=== CHAT (0 message(s)) ===";
        String cleanUser = username.trim();

        GroupChat chat = null;
        if (partnerOrCode != null && !partnerOrCode.trim().isEmpty()) {
            chat = getChatByCode(partnerOrCode.trim());
            if (chat == null) {
                List<String> others = Arrays.stream(partnerOrCode.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());
                if (!others.isEmpty()) {
                    chat = getOrCreateChatRoom(cleanUser, others);
                }
            }
        }

        if (chat == null) {
            String lastCode = userActiveChatPartner.get(cleanUser.toLowerCase());
            if (lastCode != null) {
                chat = getChatByCode(lastCode);
            }
        }

        if (chat != null) {
            String others = chat.getOtherMembersDisplay(cleanUser).toUpperCase();
            return "=== CHAT WITH " + others + " [" + chat.getCode() + "] (" + count + " message(s)) ===";
        }

        return "=== CHAT FOR " + cleanUser.toUpperCase() + " (" + count + " message(s)) ===";
    }

    public List<String> getMessages(String user) {
        if (user == null) return Collections.emptyList();
        return inboxes.getOrDefault(user.trim().toLowerCase(), Collections.emptyList());
    }
}

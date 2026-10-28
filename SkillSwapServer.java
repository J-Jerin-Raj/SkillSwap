import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Application tier: multithreaded socket server with a worker pool.
 * Protocol (one line per request):
 * REGISTER name|contact|skill,skill|skill,skill -> OK / ERROR, then END
 * MATCH name                                    -> matches, recommendations & demand, then END
 * USERS                                         -> lists all users and their skills, then END
 * SEND_MSG from|to|message                      -> OK / ERROR, then END
 * GET_MSGS name                                 -> all messages for name, then END
 */
public class SkillSwapServer {
    public static final int DEFAULT_PORT = 5050;
    private static final MatchEngine engine = new MatchEngine();
    private static final Database db = new Database();

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number '" + args[0] + "', defaulting to " + DEFAULT_PORT);
            }
        }

        // Load persisted users (if DB available), else seed initial demo data
        if (db.connect()) {
            db.loadUsers().forEach(engine::addUser);
        }
        if (engine.getUser("Alice") == null) {
            engine.addUser(new User("Alice", "alice@campus.edu", List.of("Godot Engine", "Game Maker"), List.of("Unity")));
            engine.addUser(new User("Bob", "bob@campus.edu", List.of("Unity", "Raylib in C"), List.of("Godot Engine")));
            engine.addUser(new User("Cara", "cara@campus.edu", List.of("Python"), List.of("Unity")));
        }

        List<String> ipList = getNetworkInterfacesInfo();
        System.out.println("==========================================================");
        System.out.println("             SKILLSWAP SERVER STARTED");
        System.out.println("==========================================================");
        System.out.println("Listening on Port: " + port);
        System.out.println("\nYour Laptop IP Address(es) for connecting from other laptops:");
        for (String info : ipList) {
            System.out.println("   -> " + info);
        }
        System.out.println("\n--- Quick Connection Guide for Client ---");
        System.out.println("1. Connect laptops to the same Wi-Fi / Hotspot.");
        System.out.println("2. Run client: java SkillSwapClient");
        System.out.println("3. Set Server IP to your Wi-Fi IP and Port to " + port);
        System.out.println("==========================================================\n");

        ExecutorService pool = Executors.newFixedThreadPool(10);
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("[SERVER READY] Awaiting client connections on port " + port + "...\n");
            while (true) {
                Socket client = server.accept();
                pool.execute(() -> handle(client));
            }
        } catch (BindException e) {
            System.err.println("[ERROR] Port " + port + " is already in use by another application.");
            System.err.println("Try specifying an alternate port: java SkillSwapServer <port_number>");
        } catch (IOException e) {
            System.err.println("[SERVER ERROR] " + e.getMessage());
        }
    }

    private static void handle(Socket socket) {
        String clientIp = socket.getInetAddress().getHostAddress();
        try (socket;
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            String line = in.readLine();
            if (line == null || line.trim().isEmpty()) {
                return;
            }

            line = line.trim();

            if (line.startsWith("REGISTER ")) {
                String payload = line.substring(9).trim();
                String[] p = payload.split("\\|", -1);
                String name = p.length > 0 ? p[0].trim() : "";
                if (name.isEmpty()) {
                    out.println("ERROR Name cannot be empty.");
                    out.println("END");
                    return;
                }
                String contact = p.length > 1 ? p[1].trim() : "";
                List<String> offered = (p.length > 2 && !p[2].trim().isEmpty())
                        ? Arrays.asList(p[2].split(",")) : Collections.emptyList();
                List<String> wanted = (p.length > 3 && !p[3].trim().isEmpty())
                        ? Arrays.asList(p[3].split(",")) : Collections.emptyList();

                User u = new User(name, contact, offered, wanted);
                engine.addUser(u);
                db.saveUser(u);
                System.out.println("[INFO] [REGISTER] User '" + u.getName() + "' registered/updated from " + clientIp);
                out.println("OK Profile for '" + u.getName() + "' saved successfully!");
                out.println("END");

            } else if (line.startsWith("MATCH ")) {
                String name = line.substring(6).trim();
                if (name.isEmpty()) {
                    out.println("ERROR Please specify a user name to find matches.");
                    out.println("END");
                    return;
                }

                User me = engine.getUser(name);
                if (me == null) {
                    out.println("ERROR User '" + name + "' not found. Please click 'Save Profile' first.");
                    out.println("END");
                    return;
                }

                System.out.println("[INFO] [MATCH] Matchmaking query for '" + me.getName() + "' from " + clientIp);

                out.println("==================================================");
                out.println("       MATCH & RECOMMENDATION RESULTS FOR: " + me.getName().toUpperCase());
                out.println("==================================================");

                // 1. Reciprocal Matches (Two-way mutual matches)
                List<MatchResult> matches = engine.findMatches(me);
                out.println("\n--- 1. MUTUAL MATCHES (Reciprocal Skill Swap) ---");
                if (matches.isEmpty()) {
                    out.println("No direct two-way matches found yet.");
                } else {
                    out.println("Found " + matches.size() + " reciprocal match(es):");
                    for (MatchResult r : matches) {
                        out.println("--------------------------------------------------");
                        out.println("★ RECIPROCAL MATCH: " + r.score + "% Affinity with " + r.peer.getName());
                        out.println("Contact: " + r.peer.getContact());
                        out.println("-> They teach you       : " + r.theyTeachMe);
                        out.println("-> You teach them       : " + r.iTeachThem);
                        out.println("-> What they want overall: " + r.peer.getWanted());
                        out.println("-> All skills they offer: " + r.peer.getOffered());
                    }
                }

                // 2. Mentors / Recommendations: Users who have skills you want to learn
                Map<User, Set<String>> teachers = engine.findUsersWhoTeachWhatIWant(me);
                out.println("\n--- 2. RECOMMENDATIONS (Users Who Teach What You Want) ---");
                if (teachers.isEmpty()) {
                    out.println("No users currently teach the skills you want (" + me.getWanted() + ").");
                } else {
                    for (Map.Entry<User, Set<String>> entry : teachers.entrySet()) {
                        User peer = entry.getKey();
                        out.println("--------------------------------------------------");
                        out.println("User: " + peer.getName() + " (" + peer.getContact() + ")");
                        out.println("-> Can teach you: " + entry.getValue());
                        out.println("-> What they are looking to learn: " + peer.getWanted());
                        out.println("-> All skills they offer: " + peer.getOffered());
                    }
                }

                // 3. Learners / Demand: Users who want skills you offer
                Map<User, Set<String>> learners = engine.findUsersWhoWantMySkills(me);
                out.println("\n--- 3. LEARNERS (Users Who Want The Skills You Offer) ---");
                if (learners.isEmpty()) {
                    out.println("No users are currently looking for skills you offer (" + me.getOffered() + ").");
                } else {
                    for (Map.Entry<User, Set<String>> entry : learners.entrySet()) {
                        User peer = entry.getKey();
                        out.println("--------------------------------------------------");
                        out.println("User: " + peer.getName() + " (" + peer.getContact() + ")");
                        out.println("-> Wants to learn from you: " + entry.getValue());
                        out.println("-> Skills they can teach in return: " + peer.getOffered());
                    }
                }
                out.println("END");

            } else if (line.equals("USERS") || line.equals("ALL")) {
                Collection<User> all = engine.getAllUsers();
                System.out.println("[INFO] [USERS] Directory requested from " + clientIp);
                out.println("=== ALL REGISTERED USERS & THEIR SKILLS (" + all.size() + " users) ===");
                if (all.isEmpty()) {
                    out.println("No users registered yet.");
                } else {
                    for (User u : all) {
                        out.println("--------------------------------------------------");
                        out.println("User   : " + u.getName() + " (" + u.getContact() + ")");
                        out.println("OFFERS : " + u.getOffered());
                        out.println("WANTS  : " + u.getWanted());
                    }
                }
                out.println("END");

            } else if (line.startsWith("SEND_MSG ")) {
                String payload = line.substring(9).trim();
                String[] p = payload.split("\\|", 3);
                if (p.length < 3 || p[0].trim().isEmpty() || p[1].trim().isEmpty() || p[2].trim().isEmpty()) {
                    out.println("ERROR Format: SEND_MSG from|to|message (all fields required)");
                } else {
                    String from = p[0].trim();
                    String to = p[1].trim();
                    String msg = p[2].trim();
                    boolean sent = engine.addMessage(from, to, msg);
                    if (sent) {
                        System.out.println("[INFO] [SEND_MSG] Message routed: " + from + " -> " + to);
                        out.println("OK Message sent to " + to);
                    } else {
                        System.out.println("[WARN] [SEND_MSG] Failed: Recipient '" + to + "' not found");
                        out.println("ERROR Recipient '" + to + "' does not exist. They must register first.");
                    }
                }
                out.println("END");

            } else if (line.startsWith("GET_MSGS ")) {
                String user = line.substring(9).trim();
                if (user.isEmpty()) {
                    out.println("ERROR Please specify your user name.");
                    out.println("END");
                    return;
                }

                System.out.println("[INFO] [GET_MSGS] Inbox checked by '" + user + "' from " + clientIp);
                List<String> msgs = engine.getMessages(user);
                out.println("=== INBOX FOR " + user.toUpperCase() + " (" + msgs.size() + " message(s)) ===");
                if (msgs.isEmpty()) {
                    out.println("No messages received yet.");
                } else {
                    for (String m : msgs) {
                        out.println(m);
                    }
                }
                out.println("END");

            } else {
                System.out.println("[WARN] Unknown command received: " + line);
                out.println("ERROR Unknown command: " + line);
                out.println("END");
            }
        } catch (IOException e) {
            System.out.println("[CLIENT DISCONNECT] " + clientIp + " (" + e.getMessage() + ")");
        }
    }

    private static List<String> getNetworkInterfacesInfo() {
        List<String> list = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces != null && ifaces.hasMoreElements()) {
                NetworkInterface iface = ifaces.nextElement();
                if (iface.isLoopback() || !iface.isUp())
                    continue;
                String displayName = iface.getDisplayName();
                Enumeration<InetAddress> addrs = iface.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        list.add(addr.getHostAddress() + " (" + displayName + ")");
                    }
                }
            }
        } catch (Exception ignored) {
        }
        if (list.isEmpty()) {
            try {
                list.add(InetAddress.getLocalHost().getHostAddress() + " (Localhost)");
            } catch (Exception ignored) {
                list.add("127.0.0.1 (Localhost)");
            }
        }
        return list;
    }
}

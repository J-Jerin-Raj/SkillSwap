import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * [CO3 / CO5]: Multithreaded TCP socket server with worker thread pool.
 * Protocol (one line per request):
 * LOGIN name|password                           -> OK|name|contact|offered|wanted or ERROR, then END
 * REGISTER name|password|contact|offered|wanted -> OK|name|contact|offered|wanted or ERROR, then END
 * MATCH name                                    -> formatted human-readable match report, then END
 * MATCH_DATA name                               -> structured match cards for GUI rendering, then END
 * USERS                                         -> lists all users and their skills, then END
 * SEND_MSG from|to|message                      -> OK / ERROR, then END
 * GET_MSGS name                                 -> all messages for name, then END
 */
public class SkillSwapServer {
    public static final int DEFAULT_PORT = 5050;
    private static final MatchEngine engine = new MatchEngine();
    private static final Database db = new Database();

    // [CO4]: Queue collection for storing server event audit trail
    private static final Queue<String> auditLog = new ConcurrentLinkedQueue<>();

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
            engine.addUser(new User("Alice", "alice123", "alice@campus.edu",
                    List.of("Godot Engine", "Game Maker", "2D Art"),
                    List.of("Unity", "C#", "Blender")));
            engine.addUser(new User("Bob", "bob123", "bob@campus.edu",
                    List.of("Unity", "Raylib in C", "C#"),
                    List.of("Godot Engine", "Game Design")));
            engine.addUser(new User("Cara", "cara123", "cara@campus.edu",
                    List.of("Python", "Data Science", "Machine Learning"),
                    List.of("Unity", "Web Development")));
            engine.addUser(new User("David", "david123", "david@campus.edu",
                    List.of("Web Development", "React", "JavaScript"),
                    List.of("Python", "Machine Learning")));
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

            if (line.startsWith("LOGIN ")) {
                String payload = line.substring(6).trim();
                String[] parts = payload.split("\\|", 2);
                String name = parts.length > 0 ? parts[0].trim() : "";
                String password = parts.length > 1 ? parts[1].trim() : "";

                try {
                    User u = engine.authenticate(name, password);
                    auditLog.offer("LOGIN: " + u.getName() + " from " + clientIp);
                    System.out.println("[INFO] [LOGIN] User '" + u.getName() + "' authenticated from " + clientIp);
                    out.println("OK|" + u.getName() + "|" + u.getContact() + "|"
                            + String.join(",", u.getOffered()) + "|" + String.join(",", u.getWanted()) + "|" + u.getPassword());
                } catch (AuthenticationException e) {
                    System.out.println("[WARN] [LOGIN] Failed attempt for: " + name + " (" + e.getMessage() + ")");
                    out.println("ERROR " + e.getMessage());
                }
                out.println("END");

            } else if (line.startsWith("REGISTER ")) {
                String payload = line.substring(9).trim();
                String[] p = payload.split("\\|", -1);
                String name = p.length > 0 ? p[0].trim() : "";
                String password = p.length > 1 ? p[1].trim() : "";
                String contact = p.length > 2 ? p[2].trim() : "";
                List<String> offered = (p.length > 3 && !p[3].trim().isEmpty())
                        ? Arrays.asList(p[3].split(",")) : Collections.emptyList();
                List<String> wanted = (p.length > 4 && !p[4].trim().isEmpty())
                        ? Arrays.asList(p[4].split(",")) : Collections.emptyList();

                try {
                    User u = engine.registerUser(name, password, contact, offered, wanted);
                    db.saveUser(u);
                    auditLog.offer("REGISTER: " + u.getName() + " from " + clientIp);
                    System.out.println("[INFO] [REGISTER] User '" + u.getName() + "' registered from " + clientIp);
                    out.println("OK Profile for '" + u.getName() + "' saved successfully!|"
                            + u.getName() + "|" + u.getContact() + "|"
                            + String.join(",", u.getOffered()) + "|" + String.join(",", u.getWanted()) + "|" + u.getPassword());
                } catch (InvalidProfileException e) {
                    System.out.println("[WARN] [REGISTER] Failed registration: " + e.getMessage());
                    out.println("ERROR " + e.getMessage());
                }
                out.println("END");

            } else if (line.startsWith("MATCH_DATA ")) {
                String name = line.substring(11).trim();
                User me = engine.getUser(name);
                if (me == null) {
                    out.println("ERROR User '" + name + "' not found.");
                    out.println("END");
                    return;
                }

                List<MatchResult> list = engine.computeAllMatches(me);
                out.println("CURRENT_USER|" + me.getName() + "|" + me.getContact() + "|"
                        + String.join(",", me.getOffered()) + "|" + String.join(",", me.getWanted()));

                for (MatchResult r : list) {
                    out.println("MATCH_CARD|"
                            + r.peer.getName() + "|"
                            + r.peer.getContact() + "|"
                            + String.join(",", r.peer.getOffered()) + "|"
                            + String.join(",", r.peer.getWanted()) + "|"
                            + String.join(",", r.theyTeachMe) + "|"
                            + String.join(",", r.iTeachThem) + "|"
                            + String.join(",", r.sharedInterests) + "|"
                            + r.score + "|"
                            + r.matchType);
                }
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
                    out.println("ERROR User '" + name + "' not found. Please click 'Sign Up' first.");
                    out.println("END");
                    return;
                }

                System.out.println("[INFO] [MATCH] Matchmaking query for '" + me.getName() + "' from " + clientIp);
                List<MatchResult> all = engine.computeAllMatches(me);

                out.println("==================================================");
                out.println("       MATCH & RECOMMENDATION RESULTS FOR: " + me.getName().toUpperCase());
                out.println("==================================================");

                if (all.isEmpty()) {
                    out.println("No matching peers found based on your skills and wanted interests.");
                } else {
                    out.println("Found " + all.size() + " compatible peer(s) ranked by Affinity Score:\n");
                    for (MatchResult r : all) {
                        out.println("--------------------------------------------------");
                        out.println("[AFFINITY SCORE: " + r.score + "%] | " + r.getCompatibilityLabel());
                        out.println("User: " + r.peer.getName() + " (" + r.peer.getContact() + ")");
                        out.println("Match Type: " + r.matchType);
                        if (!r.theyTeachMe.isEmpty()) {
                            out.println("-> They teach you  : " + r.theyTeachMe);
                        }
                        if (!r.iTeachThem.isEmpty()) {
                            out.println("-> You teach them  : " + r.iTeachThem);
                        }
                        if (!r.sharedInterests.isEmpty()) {
                            out.println("-> Shared Interests: " + r.sharedInterests);
                        }
                        out.println("-> All they OFFER  : " + r.peer.getOffered());
                        out.println("-> What they WANT  : " + r.peer.getWanted());
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
                        auditLog.offer("MSG: " + from + " -> " + to);
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

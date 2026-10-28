import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Application tier: multithreaded socket server with a worker pool.
 * Protocol (one line per request):
 * REGISTER name|contact|skill,skill|skill,skill -> OK
 * MATCH name -> matches and what peers want, then END
 * USERS -> lists all users and their wanted skills, then END
 * SEND_MSG from|to|message -> OK / ERROR
 * GET_MSGS name -> all messages for name, then END
 */
public class SkillSwapServer {
    private static final int PORT = 5000;
    private static final MatchEngine engine = new MatchEngine();
    private static final Database db = new Database();

    public static void main(String[] args) throws IOException {
        // Load persisted users (if DB available), else seed demo data
        if (db.connect())
            db.loadUsers().forEach(engine::addUser);
        if (engine.getUser("Alice") == null) {
            engine.addUser(
                    new User("Alice", "alice@campus.edu", List.of("Godot Engine", "Game Maker"), List.of("Unity")));
            engine.addUser(new User("Bob", "bob@campus.edu", List.of("Unity", "Raylib in C"), List.of("Godot Engine")));
            engine.addUser(new User("Cara", "cara@campus.edu", List.of("Python"), List.of("Unity")));
        }

        List<String> ipList = getLocalIpAddresses();
        System.out.println("==========================================================");
        System.out.println("             SKILLSWAP SERVER STARTED");
        System.out.println("==========================================================");
        System.out.println("Listening on Port: " + PORT);
        System.out.println("\nYour Laptop IP Address(es) for connecting from other laptops:");
        for (String ip : ipList) {
            System.out.println("   -> " + ip);
        }
        System.out.println("\n--- Step-by-Step for Second Laptop ---");
        System.out.println("1. Connect both laptops to the same Wi-Fi / Hotspot.");
        System.out.println("2. On the other laptop, run: java SkillSwapClient");
        System.out.println("3. Enter Server IP: " + (ipList.isEmpty() ? "YOUR_LAPTOP_IP" : ipList.get(0)));
        System.out.println("==========================================================\n");

        ExecutorService pool = Executors.newFixedThreadPool(10); // threaded worker pool
        try (ServerSocket server = new ServerSocket(PORT)) {
            while (true) {
                Socket client = server.accept();
                pool.execute(() -> handle(client));
            }
        }
    }

    private static void handle(Socket socket) {
        try (socket;
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            String line = in.readLine();
            if (line == null)
                return;

            if (line.startsWith("REGISTER ")) {
                String[] p = line.substring(9).split("\\|", -1);
                String name = p.length > 0 ? p[0].trim() : "";
                String contact = p.length > 1 ? p[1].trim() : "";
                List<String> offered = p.length > 2 ? Arrays.asList(p[2].split(",")) : Collections.emptyList();
                List<String> wanted = p.length > 3 ? Arrays.asList(p[3].split(",")) : Collections.emptyList();

                User u = new User(name, contact, offered, wanted);
                engine.addUser(u);
                db.saveUser(u);
                out.println("OK Profile for '" + u.getName() + "' saved successfully!");

            } else if (line.startsWith("MATCH ")) {
                String name = line.substring(6).trim();
                User me = engine.getUser(name);
                if (me == null) {
                    out.println("ERROR User '" + name + "' not found. Please click 'Save Profile' first.");
                    out.println("END");
                    return;
                }

                List<MatchResult> matches = engine.findMatches(me);
                out.println("=== MATCH RESULTS FOR " + me.getName().toUpperCase() + " ===");
                if (matches.isEmpty()) {
                    out.println("No 100% two-way mutual matches found yet.");
                } else {
                    out.println("Found " + matches.size() + " mutual match(es):");
                    for (MatchResult r : matches) {
                        out.println("--------------------------------------------------");
                        out.println("Match Affinity: " + r.score + "% with " + r.peer.getName());
                        out.println("Contact: " + r.peer.getContact());
                        out.println("-> They teach you: " + r.theyTeachMe);
                        out.println("-> You teach them: " + r.iTeachThem);
                        out.println("-> What they WANT to learn: " + r.peer.getWanted());
                        out.println("-> All skills they OFFER  : " + r.peer.getOffered());
                    }
                }

                // Also show who wants the skills I offer (even if no reciprocal match yet)
                Map<User, Set<String>> interested = engine.findUsersWhoWantMySkills(me);
                if (!interested.isEmpty()) {
                    out.println("\n=== OTHER USERS WHO WANT YOUR SKILLS ===");
                    for (Map.Entry<User, Set<String>> entry : interested.entrySet()) {
                        User peer = entry.getKey();
                        out.println("--------------------------------------------------");
                        out.println("User: " + peer.getName() + " (" + peer.getContact() + ")");
                        out.println("-> Wants from you: " + entry.getValue());
                        out.println("-> ALL skills they WANT : " + peer.getWanted());
                        out.println("-> Skills they can OFFER: " + peer.getOffered());
                    }
                }
                out.println("END");

            } else if (line.equals("USERS") || line.equals("ALL")) {
                Collection<User> all = engine.getAllUsers();
                out.println("=== ALL REGISTERED USERS & THEIR SKILLS (" + all.size() + " users) ===");
                if (all.isEmpty()) {
                    out.println("No users registered yet.");
                } else {
                    for (User u : all) {
                        out.println("--------------------------------------------------");
                        out.println("User   : " + u.getName() + " (" + u.getContact() + ")");
                        out.println("WANTS  : " + u.getWanted());
                        out.println("OFFERS : " + u.getOffered());
                    }
                }
                out.println("END");

            } else if (line.startsWith("SEND_MSG ")) {
                String[] p = line.substring(9).split("\\|", 3);
                if (p.length < 3) {
                    out.println("ERROR Format: SEND_MSG from|to|message");
                } else {
                    boolean sent = engine.addMessage(p[0].trim(), p[1].trim(), p[2].trim());
                    if (sent) {
                        out.println("OK Message sent to " + p[1].trim());
                    } else {
                        out.println("ERROR Recipient '" + p[1].trim() + "' does not exist.");
                    }
                }

            } else if (line.startsWith("GET_MSGS ")) {
                String user = line.substring(9).trim();
                List<String> msgs = engine.getMessages(user);
                out.println("=== INBOX FOR " + user.toUpperCase() + " (" + msgs.size() + " messages) ===");
                if (msgs.isEmpty()) {
                    out.println("No messages received yet.");
                } else {
                    for (String m : msgs) {
                        out.println(m);
                    }
                }
                out.println("END");

            } else {
                out.println("ERROR Unknown command.");
            }
        } catch (IOException e) {
            System.out.println("Client error: " + e.getMessage());
        }
    }

    private static List<String> getLocalIpAddresses() {
        List<String> ips = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
            while (ifaces != null && ifaces.hasMoreElements()) {
                NetworkInterface iface = ifaces.nextElement();
                if (iface.isLoopback() || !iface.isUp())
                    continue;
                Enumeration<InetAddress> addrs = iface.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress addr = addrs.nextElement();
                    if (addr instanceof Inet4Address && !addr.isLoopbackAddress()) {
                        ips.add(addr.getHostAddress());
                    }
                }
            }
        } catch (Exception ignored) {
        }
        if (ips.isEmpty()) {
            try {
                ips.add(InetAddress.getLocalHost().getHostAddress());
            } catch (Exception ignored) {
            }
        }
        return ips;
    }
}

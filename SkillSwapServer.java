import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * Application tier: multithreaded socket server with a worker pool.
 * Protocol (one line per request):
 *   REGISTER name|contact|skill,skill|skill,skill   ->  OK
 *   MATCH name                                      ->  one line per match, then END
 */
public class SkillSwapServer {
    private static final int PORT = 5000;
    private static final MatchEngine engine = new MatchEngine();
    private static final Database db = new Database();

    public static void main(String[] args) throws IOException {
        // Load persisted users (if DB available), else seed demo data
        if (db.connect()) db.loadUsers().forEach(engine::addUser);
        if (engine.getUser("Alice") == null) {
            engine.addUser(new User("Alice", "alice@campus.edu", List.of("Godot Engine", "Game Maker"), List.of("Unity")));
            engine.addUser(new User("Bob",   "bob@campus.edu",   List.of("Unity", "Raylib in C"),      List.of("Godot Engine")));
            engine.addUser(new User("Cara",  "cara@campus.edu",  List.of("Python"),                    List.of("Unity")));
        }

        ExecutorService pool = Executors.newFixedThreadPool(10);   // threaded worker pool
        try (ServerSocket server = new ServerSocket(PORT)) {
            System.out.println("SkillSwap server listening on port " + PORT);
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
            if (line == null) return;

            if (line.startsWith("REGISTER ")) {
                String[] p = line.substring(9).split("\\|", -1);
                User u = new User(p[0].trim(), p[1].trim(),
                        Arrays.asList(p[2].split(",")), Arrays.asList(p[3].split(",")));
                engine.addUser(u);
                db.saveUser(u);
                out.println("OK");

            } else if (line.startsWith("MATCH ")) {
                User me = engine.getUser(line.substring(6));
                if (me == null) { out.println("ERROR User not found. Register first."); return; }
                for (MatchResult r : engine.findMatches(me)) {
                    out.println(r.score + "% | " + r.peer.getName() + " (" + r.peer.getContact() + ")"
                            + " | teaches you: " + r.theyTeachMe + " | you teach: " + r.iTeachThem);
                }
                out.println("END");
            }
        } catch (IOException e) {
            System.out.println("Client error: " + e.getMessage());
        }
    }
}

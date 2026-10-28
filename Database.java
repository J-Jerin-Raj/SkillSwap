import java.sql.*;
import java.util.*;

/**
 * [CO5]: JDBC Data Persistence Tier (MySQL).
 * Demonstrates Connection management, PreparedStatements, ResultSets,
 * and graceful fallback to in-memory mode.
 */
public class Database {
    private static final String URL  = "jdbc:mysql://localhost:3306/skillswap";
    private static final String USER = "root";
    private static final String PASS = "1234"; // change to your password

    private Connection con;

    public boolean connect() {
        try {
            con = DriverManager.getConnection(URL, USER, PASS);
            try (Statement st = con.createStatement()) {
                st.executeUpdate("CREATE TABLE IF NOT EXISTS users (" +
                        "name VARCHAR(50) PRIMARY KEY, password VARCHAR(100), contact VARCHAR(100), " +
                        "offered VARCHAR(255), wanted VARCHAR(255))");
                try {
                    st.executeUpdate("ALTER TABLE users ADD COLUMN password VARCHAR(100) AFTER name");
                } catch (SQLException ignored) {
                    // Column already exists
                }
            }
            return true;
        } catch (SQLException e) {
            System.out.println("DB not available (" + e.getMessage() + ") - running in memory only.");
            return false;
        }
    }

    public synchronized void saveUser(User u) {
        if (con == null || u == null) return;
        String sql = "REPLACE INTO users VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, u.getName());
            ps.setString(2, u.getPassword());
            ps.setString(3, u.getContact());
            ps.setString(4, String.join(",", u.getOffered()));
            ps.setString(5, String.join(",", u.getWanted()));
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<User> loadUsers() {
        List<User> list = new ArrayList<>();
        if (con == null) return list;
        try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery("SELECT * FROM users")) {
            while (rs.next()) {
                String pwd = "password123";
                try {
                    pwd = rs.getString("password");
                    if (pwd == null || pwd.isEmpty()) pwd = "password123";
                } catch (SQLException ignored) {}

                list.add(new User(
                        rs.getString("name"),
                        pwd,
                        rs.getString("contact"),
                        Arrays.asList(rs.getString("offered").split(",")),
                        Arrays.asList(rs.getString("wanted").split(","))));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }
}

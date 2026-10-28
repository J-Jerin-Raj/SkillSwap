import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.*;
import java.net.Socket;

/**
 * Presentation tier: Swing GUI that communicates with the SkillSwap server over
 * a socket.
 * Supports cross-laptop networking by allowing custom Server IP and Port.
 */
public class SkillSwapClient extends JFrame {
    private final JTextField hostField = new JTextField("localhost", 12);
    private final JTextField portField = new JTextField("5000", 5);

    private final JTextField nameField = new JTextField(12);
    private final JTextField contactField = new JTextField(12);
    private final JTextField offeredField = new JTextField(15);
    private final JTextField wantedField = new JTextField(15);

    private final JTextField msgToField = new JTextField(10);
    private final JTextField msgTextField = new JTextField(20);

    private final JTextArea resultArea = new JTextArea(14, 50);

    public SkillSwapClient() {
        this("localhost");
    }

    public SkillSwapClient(String defaultHost) {
        super("SkillSwap - Connect & Swap Skills Across Laptops");
        hostField.setText(defaultHost == null || defaultHost.isBlank() ? "localhost" : defaultHost);

        // --- 1. Top Panel: Server Connection Settings ---
        JPanel connPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        connPanel.setBorder(new TitledBorder("1. Network Connection (Laptop-to-Laptop)"));
        connPanel.add(new JLabel("Server IP:"));
        connPanel.add(hostField);
        connPanel.add(new JLabel("Port:"));
        connPanel.add(portField);
        JLabel tipLabel = new JLabel("(Use 'localhost' on Server laptop, or Server's Wi-Fi IP on Laptop 2)");
        tipLabel.setForeground(Color.GRAY);
        connPanel.add(tipLabel);

        // --- 2. Profile Panel ---
        JPanel profilePanel = new JPanel(new GridLayout(4, 2, 6, 6));
        profilePanel.setBorder(new TitledBorder("2. My Profile & Skills"));
        profilePanel.add(new JLabel("My Name:"));
        profilePanel.add(nameField);
        profilePanel.add(new JLabel("My Contact (Email/Phone):"));
        profilePanel.add(contactField);
        profilePanel.add(new JLabel("Skills I Can Teach / Offer (comma separated):"));
        profilePanel.add(offeredField);
        profilePanel.add(new JLabel("Skills I Want To Learn (comma separated):"));
        profilePanel.add(wantedField);

        // --- 3. Buttons Panel ---
        JButton saveBtn = new JButton("Save Profile");
        JButton findBtn = new JButton("Find Matches");
        JButton allBtn = new JButton("View All Users & Wanted Skills");
        JButton inboxBtn = new JButton("Check Inbox");

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        buttons.add(saveBtn);
        buttons.add(findBtn);
        buttons.add(allBtn);
        buttons.add(inboxBtn);

        // --- 4. Direct Messaging Panel ---
        JPanel msgPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        msgPanel.setBorder(new TitledBorder("3. Direct Communication Between Laptops"));
        msgPanel.add(new JLabel("Send Message To:"));
        msgPanel.add(msgToField);
        msgPanel.add(new JLabel("Message:"));
        msgPanel.add(msgTextField);
        JButton sendMsgBtn = new JButton("Send Message");
        msgPanel.add(sendMsgBtn);

        // Assemble Top
        JPanel northPanel = new JPanel();
        northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));
        northPanel.add(connPanel);
        northPanel.add(profilePanel);
        northPanel.add(buttons);
        northPanel.add(msgPanel);

        // Result text area
        resultArea.setEditable(false);
        resultArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        resultArea.setText("Welcome to SkillSwap!\n"
                + "• To see what everyone wants: Click 'View All Users & Wanted Skills'.\n"
                + "• To register: Enter your info above and click 'Save Profile'.\n"
                + "• On another laptop: Enter the Server's IP address in the 'Server IP' box above.\n");

        add(northPanel, BorderLayout.NORTH);
        add(new JScrollPane(resultArea), BorderLayout.CENTER);

        // Button Actions
        saveBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                resultArea.setText("Please enter your name first.");
                return;
            }
            send("REGISTER " + name + "|" + contactField.getText().trim()
                    + "|" + offeredField.getText().trim() + "|" + wantedField.getText().trim());
        });

        findBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                resultArea.setText("Please enter your name in the 'My Name' field to find matches.");
                return;
            }
            send("MATCH " + name);
        });

        allBtn.addActionListener(e -> send("USERS"));

        inboxBtn.addActionListener(e -> {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                resultArea.setText("Please enter your name in the 'My Name' field to check your inbox.");
                return;
            }
            send("GET_MSGS " + name);
        });

        sendMsgBtn.addActionListener(e -> {
            String from = nameField.getText().trim();
            String to = msgToField.getText().trim();
            String msg = msgTextField.getText().trim();
            if (from.isEmpty()) {
                resultArea.setText("Please enter your name in 'My Name' so the recipient knows who sent the message.");
                return;
            }
            if (to.isEmpty() || msg.isEmpty()) {
                resultArea.setText("Please enter both the recipient name and a message.");
                return;
            }
            send("SEND_MSG " + from + "|" + to + "|" + msg);
            msgTextField.setText("");
        });

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
    }

    /**
     * Sends a request to the server on a background thread so the GUI never
     * freezes.
     */
    private void send(String request) {
        String host = hostField.getText().trim();
        int port = 5000;
        try {
            port = Integer.parseInt(portField.getText().trim());
        } catch (NumberFormatException ignored) {
        }

        final String targetHost = host.isEmpty() ? "localhost" : host;
        final int targetPort = port;

        resultArea.setText("Connecting to " + targetHost + ":" + targetPort + "...");

        new Thread(() -> {
            StringBuilder sb = new StringBuilder();
            try (Socket s = new Socket(targetHost, targetPort);
                    PrintWriter out = new PrintWriter(s.getOutputStream(), true);
                    BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {

                out.println(request);
                String line;
                while ((line = in.readLine()) != null && !line.equals("END")) {
                    sb.append(line).append("\n");
                }
            } catch (IOException ex) {
                sb.append("Connection failed: ").append(ex.getMessage()).append("\n\n")
                        .append("Troubleshooting for Two Laptops:\n")
                        .append("1. Is the SkillSwapServer running on ").append(targetHost).append("?\n")
                        .append("2. Are both laptops connected to the same Wi-Fi / Hotspot?\n")
                        .append("3. Verify the IP address matches the IP printed on the server console.\n")
                        .append("4. Make sure Windows Firewall allows Java on port ").append(targetPort).append(".\n");
            }
            SwingUtilities.invokeLater(() -> resultArea.setText(sb.toString()));
        }).start();
    }

    public static void main(String[] args) {
        String host = (args.length > 0) ? args[0] : "localhost";
        SwingUtilities.invokeLater(() -> new SkillSwapClient(host).setVisible(true));
    }
}

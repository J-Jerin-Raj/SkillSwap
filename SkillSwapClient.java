import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;

/** Presentation tier: Swing GUI that talks to the server over a socket. */
public class SkillSwapClient extends JFrame {
    private final JTextField nameField = new JTextField(15);
    private final JTextField contactField = new JTextField(15);
    private final JTextField offeredField = new JTextField(15);
    private final JTextField wantedField = new JTextField(15);
    private final JTextArea resultArea = new JTextArea(10, 40);

    public SkillSwapClient() {
        super("SkillSwap - Learn a Skill. Share a Skill. Find Your Match.");

        JPanel form = new JPanel(new GridLayout(4, 2, 5, 5));
        form.add(new JLabel("Name:"));                          form.add(nameField);
        form.add(new JLabel("Contact:"));                       form.add(contactField);
        form.add(new JLabel("Skills I offer (comma sep):"));    form.add(offeredField);
        form.add(new JLabel("Skills I want (comma sep):"));     form.add(wantedField);

        JButton saveBtn = new JButton("Save Profile");
        JButton findBtn = new JButton("Find Matches");
        JPanel buttons = new JPanel();
        buttons.add(saveBtn);
        buttons.add(findBtn);

        JPanel top = new JPanel(new BorderLayout());
        top.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        top.add(form, BorderLayout.CENTER);
        top.add(buttons, BorderLayout.SOUTH);

        resultArea.setEditable(false);
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(resultArea), BorderLayout.CENTER);

        saveBtn.addActionListener(e -> send("REGISTER " + nameField.getText() + "|" + contactField.getText()
                + "|" + offeredField.getText() + "|" + wantedField.getText()));
        findBtn.addActionListener(e -> send("MATCH " + nameField.getText()));

        setDefaultCloseOperation(EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);
    }

    /** Sends a request on a background thread so the GUI never freezes. */
    private void send(String request) {
        resultArea.setText("Contacting server...");
        new Thread(() -> {
            StringBuilder sb = new StringBuilder();
            try (Socket s = new Socket("localhost", 5000);
                 PrintWriter out = new PrintWriter(s.getOutputStream(), true);
                 BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()))) {
                out.println(request);
                String line;
                while ((line = in.readLine()) != null && !line.equals("END")) sb.append(line).append("\n");
                if (request.startsWith("MATCH") && sb.length() == 0) sb.append("No matches found yet.");
            } catch (IOException ex) {
                sb.append("Cannot reach server: ").append(ex.getMessage());
            }
            SwingUtilities.invokeLater(() -> resultArea.setText(sb.toString()));
        }).start();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new SkillSwapClient().setVisible(true));
    }
}

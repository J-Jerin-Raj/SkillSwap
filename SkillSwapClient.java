import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.List;

/**
 * [CO5]: Graphical User Interface Design using Swing API, Event Handling, and Socket Networking.
 * Features:
 * - Clean modern presentation tier without emojis.
 * - Credential authentication (Username + Password) on Sign In & Sign Up.
 * - Dynamic 1-second background inbox polling using Swing Timer.
 * - Multi-skill & multi-interest management with live badge previews.
 * - Matchmaking feed with Affinity Scores and direct messaging integration.
 * - Mirrored Profile editor with locked username field.
 * - File I/O operations for exporting chat logs ([CO4]).
 */
public class SkillSwapClient extends JFrame {

    // --- Modern Theme Palette ---
    public static class Theme {
        public static final Color BG_DARK       = new Color(22, 24, 34);     // Window background
        public static final Color CARD_BG       = new Color(30, 33, 48);     // Card panels
        public static final Color CARD_HOVER    = new Color(38, 42, 60);     // Hover state
        public static final Color INPUT_BG      = new Color(40, 44, 64);     // Text fields
        public static final Color INPUT_BORDER  = new Color(60, 66, 95);     // Field borders
        public static final Color PRIMARY       = new Color(99, 102, 241);   // Indigo primary
        public static final Color PRIMARY_HOVER = new Color(129, 140, 248);  // Primary hover
        public static final Color SUCCESS       = new Color(34, 197, 94);    // Green
        public static final Color CYAN          = new Color(56, 189, 248);   // Blue/Cyan
        public static final Color AMBER         = new Color(245, 158, 11);   // Amber
        public static final Color PURPLE        = new Color(168, 85, 247);   // Purple
        public static final Color TEXT_MAIN     = new Color(243, 244, 246);  // Text primary
        public static final Color TEXT_MUTED    = new Color(156, 163, 175);  // Text subtle
        public static final Color BORDER_SUBTLE = new Color(49, 54, 77);

        public static final Font TITLE_FONT   = new Font("SansSerif", Font.BOLD, 22);
        public static final Font SUBTITLE_FONT= new Font("SansSerif", Font.BOLD, 15);
        public static final Font BOLD_FONT    = new Font("SansSerif", Font.BOLD, 13);
        public static final Font REGULAR_FONT = new Font("SansSerif", Font.PLAIN, 13);
        public static final Font SMALL_FONT   = new Font("SansSerif", Font.PLAIN, 11);
        public static final Font MONO_FONT    = new Font("Monospaced", Font.PLAIN, 12);
    }

    // --- Network Settings ---
    private final JTextField hostField = createStyledTextField("localhost", 10);
    private final JTextField portField = createStyledTextField("5050", 5);

    // --- Active Session ---
    private String currentUserName = "";
    private String currentUserPassword = "";
    private String currentUserContact = "";
    private String currentUserOffered = "";
    private String currentUserWanted = "";

    // --- Navigation & Core Views ---
    private final CardLayout rootCardLayout = new CardLayout();
    private final JPanel rootPanel = new JPanel(rootCardLayout);
    private JTabbedPane mainTabbedPane; // Direct reference to avoid brittle getParent() chains

    // --- Auth View Components ---
    private final CardLayout authCardLayout = new CardLayout();
    private final JPanel authCardContainer = new JPanel(authCardLayout);
    private final JTextField loginNameField = createStyledTextField("", 16);
    private final JPasswordField loginPasswordField = createStyledPasswordField("", 16);

    // Sign Up Fields
    private final JTextField suNameField = createStyledTextField("", 16);
    private final JPasswordField suPasswordField = createStyledPasswordField("", 16);
    private final JTextField suContactField = createStyledTextField("", 16);
    private final JTextField suOfferedField = createStyledTextField("", 20);
    private final JTextField suWantedField = createStyledTextField("", 20);
    private final JPanel suOfferedPills = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
    private final JPanel suWantedPills = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
    private final JLabel authStatusLabel = new JLabel(" ", SwingConstants.CENTER);

    // --- Dashboard Components ---
    private final JLabel userSessionLabel = new JLabel();
    private final JPanel matchesCardsContainer = new JPanel();
    private final JTextArea matchesRawTextArea = new JTextArea();
    private final JCheckBox toggleRawReportCheck = new JCheckBox("Show Raw Terminal Report", false);

    private final JPanel usersContainer = new JPanel();
    private final JTextField msgToField = createStyledTextField("", 12);
    private final JTextField msgTextField = createStyledTextField("", 24);
    private final JTextArea inboxTextArea = new JTextArea();
    private final JLabel dashboardStatusLabel = new JLabel("Ready", SwingConstants.LEFT);

    // --- Profile Editor Components (Mirrors Sign Up) ---
    private final JTextField profNameField = createStyledTextField("", 16);
    private final JPasswordField profPasswordField = createStyledPasswordField("", 16);
    private final JTextField profContactField = createStyledTextField("", 16);
    private final JTextField profOfferedField = createStyledTextField("", 20);
    private final JTextField profWantedField = createStyledTextField("", 20);
    private final JPanel profOfferedPills = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
    private final JPanel profWantedPills = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));

    // [CO3]: Background Timer checking inbox every 1000ms (1 second)
    private final javax.swing.Timer inboxAutoTimer;
    private String lastInboxContent = "";

    public SkillSwapClient() {
        this("localhost");
    }

    public SkillSwapClient(String defaultHost) {
        super("SkillSwap - Peer-to-Peer Skill Matching & Communication Platform");
        hostField.setText(defaultHost == null || defaultHost.isBlank() ? "localhost" : defaultHost.trim());

        setupWindow();
        initAuthScreen();
        initDashboardScreen();

        // [CO3]: 1-second auto inbox poller
        inboxAutoTimer = new javax.swing.Timer(1000, e -> autoPollInbox());

        rootCardLayout.show(rootPanel, "AUTH");
        add(rootPanel);

        // Preload default demo user
        loginNameField.setText("Alice");
        loginPasswordField.setText("alice123");
    }

    private void setupWindow() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1040, 780);
        setMinimumSize(new Dimension(880, 660));
        setLocationRelativeTo(null);
        getContentPane().setBackground(Theme.BG_DARK);
    }

    // =========================================================================
    // 1. AUTHENTICATION VIEW (SIGN IN & SIGN UP)
    // =========================================================================
    private void initAuthScreen() {
        JPanel authView = new JPanel(new BorderLayout());
        authView.setBackground(Theme.BG_DARK);

        // Top Network Settings Strip
        JPanel topNetworkBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        topNetworkBar.setBackground(Theme.BG_DARK);
        topNetworkBar.setBorder(new EmptyBorder(4, 12, 4, 12));

        JLabel netLabel = new JLabel("Server IP:");
        netLabel.setFont(Theme.SMALL_FONT);
        netLabel.setForeground(Theme.TEXT_MUTED);

        JLabel portLabel = new JLabel("Port:");
        portLabel.setFont(Theme.SMALL_FONT);
        portLabel.setForeground(Theme.TEXT_MUTED);

        topNetworkBar.add(netLabel);
        topNetworkBar.add(hostField);
        topNetworkBar.add(portLabel);
        topNetworkBar.add(portField);

        // Center Box
        JPanel centerWrap = new JPanel(new GridBagLayout());
        centerWrap.setBackground(Theme.BG_DARK);

        RoundedPanel authBox = new RoundedPanel(18, Theme.CARD_BG, Theme.BORDER_SUBTLE);
        authBox.setPreferredSize(new Dimension(600, 630));
        authBox.setLayout(new BorderLayout());
        authBox.setBorder(new EmptyBorder(22, 26, 22, 26));

        // Header Panel
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setOpaque(false);

        JLabel logoLabel = new JLabel("SkillSwap");
        logoLabel.setFont(Theme.TITLE_FONT);
        logoLabel.setForeground(Theme.TEXT_MAIN);
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Peer-to-Peer Collaborative Skill Exchange");
        subtitleLabel.setFont(Theme.REGULAR_FONT);
        subtitleLabel.setForeground(Theme.TEXT_MUTED);
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        headerPanel.add(logoLabel);
        headerPanel.add(Box.createVerticalStrut(4));
        headerPanel.add(subtitleLabel);
        headerPanel.add(Box.createVerticalStrut(14));

        // Segmented Switcher Bar (Sign In / Sign Up)
        JPanel switcherPanel = new JPanel(new GridLayout(1, 2, 8, 0));
        switcherPanel.setOpaque(false);
        switcherPanel.setPreferredSize(new Dimension(360, 38));
        switcherPanel.setMaximumSize(new Dimension(360, 38));

        ModernButton tabSignInBtn = new ModernButton("Sign In", Theme.PRIMARY, Theme.PRIMARY_HOVER);
        ModernButton tabSignUpBtn = new ModernButton("Create Profile (Sign Up)", Theme.CARD_HOVER, Theme.PRIMARY);

        tabSignInBtn.addActionListener(e -> {
            tabSignInBtn.setBackgroundColor(Theme.PRIMARY);
            tabSignUpBtn.setBackgroundColor(Theme.CARD_HOVER);
            authCardLayout.show(authCardContainer, "LOGIN");
            authStatusLabel.setText(" ");
        });

        tabSignUpBtn.addActionListener(e -> {
            tabSignUpBtn.setBackgroundColor(Theme.PRIMARY);
            tabSignInBtn.setBackgroundColor(Theme.CARD_HOVER);
            authCardLayout.show(authCardContainer, "SIGNUP");
            authStatusLabel.setText(" ");
        });

        switcherPanel.add(tabSignInBtn);
        switcherPanel.add(tabSignUpBtn);

        headerPanel.add(switcherPanel);
        headerPanel.add(Box.createVerticalStrut(10));

        // Form Cards Container
        authCardContainer.setOpaque(false);
        authCardContainer.add(buildLoginForm(tabSignUpBtn), "LOGIN");
        authCardContainer.add(buildSignUpForm(tabSignInBtn), "SIGNUP");

        authStatusLabel.setFont(Theme.SMALL_FONT);
        authStatusLabel.setForeground(Theme.CYAN);

        authBox.add(headerPanel, BorderLayout.NORTH);
        authBox.add(authCardContainer, BorderLayout.CENTER);
        authBox.add(authStatusLabel, BorderLayout.SOUTH);

        centerWrap.add(authBox);

        authView.add(topNetworkBar, BorderLayout.NORTH);
        authView.add(centerWrap, BorderLayout.CENTER);

        rootPanel.add(authView, "AUTH");
    }

    private JPanel buildLoginForm(ModernButton switchToSignUpBtn) {
        JPanel formWrap = new JPanel();
        formWrap.setLayout(new BoxLayout(formWrap, BoxLayout.Y_AXIS));
        formWrap.setOpaque(false);
        formWrap.setBorder(new EmptyBorder(16, 20, 10, 20));

        JLabel info = new JLabel("Enter your registered credentials to sign in:");
        info.setFont(Theme.REGULAR_FONT);
        info.setForeground(Theme.TEXT_MUTED);
        info.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel userLbl = new JLabel("Username / Name:");
        userLbl.setFont(Theme.BOLD_FONT);
        userLbl.setForeground(Theme.TEXT_MAIN);
        userLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel passLbl = new JLabel("Password:");
        passLbl.setFont(Theme.BOLD_FONT);
        passLbl.setForeground(Theme.TEXT_MAIN);
        passLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        ModernButton loginBtn = new ModernButton("Sign In to SkillSwap", Theme.PRIMARY, Theme.PRIMARY_HOVER);
        loginBtn.setMaximumSize(new Dimension(Short.MAX_VALUE, 40));
        loginBtn.setAlignmentX(Component.LEFT_ALIGNMENT);

        loginBtn.addActionListener(e -> handleLogin());
        loginNameField.addActionListener(e -> loginPasswordField.requestFocusInWindow());
        loginPasswordField.addActionListener(e -> handleLogin());

        formWrap.add(info);
        formWrap.add(Box.createVerticalStrut(14));
        formWrap.add(userLbl);
        formWrap.add(Box.createVerticalStrut(4));
        formWrap.add(loginNameField);
        formWrap.add(Box.createVerticalStrut(10));
        formWrap.add(passLbl);
        formWrap.add(Box.createVerticalStrut(4));
        formWrap.add(loginPasswordField);
        formWrap.add(Box.createVerticalStrut(16));
        formWrap.add(loginBtn);
        formWrap.add(Box.createVerticalStrut(14));

        // Quick demo user buttons
        JLabel quickLbl = new JLabel("Quick Demo Profiles:");
        quickLbl.setFont(Theme.SMALL_FONT);
        quickLbl.setForeground(Theme.TEXT_MUTED);
        quickLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        formWrap.add(quickLbl);
        formWrap.add(Box.createVerticalStrut(6));

        JPanel demoBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        demoBar.setOpaque(false);
        demoBar.setAlignmentX(Component.LEFT_ALIGNMENT);

        String[][] demoUsers = {
            {"Alice", "alice123"},
            {"Bob", "bob123"},
            {"Cara", "cara123"},
            {"David", "david123"}
        };

        for (String[] demo : demoUsers) {
            JButton b = createChipButton(demo[0], Theme.CARD_HOVER, Theme.TEXT_MAIN);
            b.addActionListener(ev -> {
                loginNameField.setText(demo[0]);
                loginPasswordField.setText(demo[1]);
                handleLogin();
            });
            demoBar.add(b);
        }
        formWrap.add(demoBar);

        formWrap.add(Box.createVerticalGlue());

        JButton linkBtn = createTextLinkButton("Don't have a profile yet? Click here to Sign Up ->");
        linkBtn.addActionListener(e -> switchToSignUpBtn.doClick());
        linkBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        formWrap.add(linkBtn);

        return formWrap;
    }

    private JPanel buildSignUpForm(ModernButton switchToSignInBtn) {
        JPanel formWrap = new JPanel();
        formWrap.setLayout(new BoxLayout(formWrap, BoxLayout.Y_AXIS));
        formWrap.setOpaque(false);
        formWrap.setBorder(new EmptyBorder(8, 10, 4, 10));

        // Username
        JLabel nameLbl = new JLabel("Username / Name *:");
        nameLbl.setFont(Theme.BOLD_FONT);
        nameLbl.setForeground(Theme.TEXT_MAIN);
        nameLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Password
        JLabel passLbl = new JLabel("Password *:");
        passLbl.setFont(Theme.BOLD_FONT);
        passLbl.setForeground(Theme.TEXT_MAIN);
        passLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Contact
        JLabel contactLbl = new JLabel("Contact (Email / Discord / Phone) *:");
        contactLbl.setFont(Theme.BOLD_FONT);
        contactLbl.setForeground(Theme.TEXT_MAIN);
        contactLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Skills Offered
        JLabel offeredLbl = new JLabel("Skills I Can Teach / Offer (Multiple, comma-separated) *:");
        offeredLbl.setFont(Theme.BOLD_FONT);
        offeredLbl.setForeground(Theme.SUCCESS);
        offeredLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel quickOffered = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        quickOffered.setOpaque(false);
        quickOffered.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (String skill : new String[]{"Java", "Python", "React", "C++", "DSA", "Unity", "UI/UX"}) {
            JButton chip = createChipButton("+ " + skill, Theme.INPUT_BG, Theme.TEXT_MUTED);
            chip.addActionListener(e -> appendSkill(suOfferedField, skill));
            quickOffered.add(chip);
        }

        suOfferedPills.setOpaque(false);
        suOfferedPills.setAlignmentX(Component.LEFT_ALIGNMENT);
        attachPillUpdater(suOfferedField, suOfferedPills, Theme.SUCCESS);

        // Skills Wanted
        JLabel wantedLbl = new JLabel("Interests / Skills I Want To Learn (Multiple, comma-separated) *:");
        wantedLbl.setFont(Theme.BOLD_FONT);
        wantedLbl.setForeground(Theme.CYAN);
        wantedLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel quickWanted = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        quickWanted.setOpaque(false);
        quickWanted.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (String skill : new String[]{"Machine Learning", "Godot", "Flutter", "Spring Boot", "Docker"}) {
            JButton chip = createChipButton("+ " + skill, Theme.INPUT_BG, Theme.TEXT_MUTED);
            chip.addActionListener(e -> appendSkill(suWantedField, skill));
            quickWanted.add(chip);
        }

        suWantedPills.setOpaque(false);
        suWantedPills.setAlignmentX(Component.LEFT_ALIGNMENT);
        attachPillUpdater(suWantedField, suWantedPills, Theme.CYAN);

        ModernButton signUpBtn = new ModernButton("Complete Sign Up & Enter Dashboard", Theme.PRIMARY, Theme.PRIMARY_HOVER);
        signUpBtn.setMaximumSize(new Dimension(Short.MAX_VALUE, 38));
        signUpBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        signUpBtn.addActionListener(e -> handleSignUp());

        formWrap.add(nameLbl);
        formWrap.add(Box.createVerticalStrut(2));
        formWrap.add(suNameField);
        formWrap.add(Box.createVerticalStrut(4));

        formWrap.add(passLbl);
        formWrap.add(Box.createVerticalStrut(2));
        formWrap.add(suPasswordField);
        formWrap.add(Box.createVerticalStrut(4));

        formWrap.add(contactLbl);
        formWrap.add(Box.createVerticalStrut(2));
        formWrap.add(suContactField);
        formWrap.add(Box.createVerticalStrut(6));

        formWrap.add(offeredLbl);
        formWrap.add(Box.createVerticalStrut(2));
        formWrap.add(suOfferedField);
        formWrap.add(quickOffered);
        formWrap.add(suOfferedPills);
        formWrap.add(Box.createVerticalStrut(4));

        formWrap.add(wantedLbl);
        formWrap.add(Box.createVerticalStrut(2));
        formWrap.add(suWantedField);
        formWrap.add(quickWanted);
        formWrap.add(suWantedPills);
        formWrap.add(Box.createVerticalStrut(8));

        formWrap.add(signUpBtn);

        JButton link = createTextLinkButton("Already registered? Sign In instead ->");
        link.addActionListener(e -> switchToSignInBtn.doClick());
        link.setAlignmentX(Component.CENTER_ALIGNMENT);
        formWrap.add(Box.createVerticalStrut(4));
        formWrap.add(link);

        return formWrap;
    }

    private void appendSkill(JTextField field, String skill) {
        String current = field.getText().trim();
        if (current.isEmpty()) {
            field.setText(skill);
        } else {
            List<String> list = Arrays.asList(current.split("\\s*,\\s*"));
            if (!list.contains(skill)) {
                field.setText(current + ", " + skill);
            }
        }
        field.requestFocus();
    }

    private void attachPillUpdater(JTextField field, JPanel pillsPanel, Color tagColor) {
        field.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { update(); }
            public void removeUpdate(DocumentEvent e) { update(); }
            public void changedUpdate(DocumentEvent e) { update(); }
            private void update() {
                pillsPanel.removeAll();
                String text = field.getText().trim();
                if (!text.isEmpty()) {
                    String[] items = text.split(",");
                    for (String item : items) {
                        String clean = item.trim();
                        if (!clean.isEmpty()) {
                            pillsPanel.add(new PillBadge(clean, tagColor));
                        }
                    }
                }
                pillsPanel.revalidate();
                pillsPanel.repaint();
            }
        });
    }

    // =========================================================================
    // 2. MAIN DASHBOARD VIEW
    // =========================================================================
    private void initDashboardScreen() {
        JPanel dashView = new JPanel(new BorderLayout());
        dashView.setBackground(Theme.BG_DARK);

        // Top Navigation Bar
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(Theme.CARD_BG);
        topBar.setBorder(new EmptyBorder(10, 16, 10, 16));

        JLabel brandLogo = new JLabel("SkillSwap");
        brandLogo.setFont(Theme.SUBTITLE_FONT);
        brandLogo.setForeground(Theme.TEXT_MAIN);

        JPanel rightUserPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightUserPanel.setOpaque(false);

        userSessionLabel.setFont(Theme.REGULAR_FONT);
        userSessionLabel.setForeground(Theme.CYAN);

        ModernButton refreshBtn = new ModernButton("Find Matches", Theme.PRIMARY, Theme.PRIMARY_HOVER);
        refreshBtn.addActionListener(e -> loadMatches());

        ModernButton signOutBtn = new ModernButton("Sign Out", Theme.INPUT_BG, Theme.CARD_HOVER);
        signOutBtn.addActionListener(e -> handleSignOut());

        rightUserPanel.add(userSessionLabel);
        rightUserPanel.add(refreshBtn);
        rightUserPanel.add(signOutBtn);

        topBar.add(brandLogo, BorderLayout.WEST);
        topBar.add(rightUserPanel, BorderLayout.EAST);

        // Center Content Tabs
        mainTabbedPane = new JTabbedPane();
        mainTabbedPane.setBackground(Theme.BG_DARK);
        mainTabbedPane.setForeground(Theme.TEXT_MAIN);
        mainTabbedPane.setFont(Theme.BOLD_FONT);

        mainTabbedPane.addTab("Matchmaking Feed", buildMatchesTab());
        mainTabbedPane.addTab("Community Directory", buildDirectoryTab());
        mainTabbedPane.addTab("Direct Messaging & Inbox", buildMessagingTab());
        mainTabbedPane.addTab("My Profile", buildProfileTab());

        // Bottom Status Bar
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setBackground(Theme.CARD_BG);
        bottomBar.setBorder(new EmptyBorder(6, 14, 6, 14));
        dashboardStatusLabel.setFont(Theme.SMALL_FONT);
        dashboardStatusLabel.setForeground(Theme.TEXT_MUTED);
        bottomBar.add(dashboardStatusLabel, BorderLayout.WEST);

        dashView.add(topBar, BorderLayout.NORTH);
        dashView.add(mainTabbedPane, BorderLayout.CENTER);
        dashView.add(bottomBar, BorderLayout.SOUTH);

        rootPanel.add(dashView, "DASHBOARD");
    }

    private JPanel buildMatchesTab() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBackground(Theme.BG_DARK);
        p.setBorder(new EmptyBorder(12, 14, 12, 14));

        JPanel controlBar = new JPanel(new BorderLayout());
        controlBar.setBackground(Theme.BG_DARK);

        JLabel intro = new JLabel("Compatible Peers ranked by calculated Affinity Score:");
        intro.setFont(Theme.SUBTITLE_FONT);
        intro.setForeground(Theme.TEXT_MAIN);

        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightControls.setOpaque(false);

        toggleRawReportCheck.setFont(Theme.SMALL_FONT);
        toggleRawReportCheck.setForeground(Theme.TEXT_MUTED);
        toggleRawReportCheck.setOpaque(false);

        ModernButton matchBtn = new ModernButton("Refresh Matches", Theme.PRIMARY, Theme.PRIMARY_HOVER);
        matchBtn.addActionListener(e -> loadMatches());

        rightControls.add(toggleRawReportCheck);
        rightControls.add(matchBtn);

        controlBar.add(intro, BorderLayout.WEST);
        controlBar.add(rightControls, BorderLayout.EAST);

        JPanel feedCardPanel = new JPanel(new CardLayout());
        feedCardPanel.setBackground(Theme.BG_DARK);

        matchesCardsContainer.setLayout(new BoxLayout(matchesCardsContainer, BoxLayout.Y_AXIS));
        matchesCardsContainer.setBackground(Theme.BG_DARK);
        JScrollPane cardScroll = new JScrollPane(matchesCardsContainer);
        cardScroll.setBorder(null);
        cardScroll.getVerticalScrollBar().setUnitIncrement(16);

        matchesRawTextArea.setEditable(false);
        matchesRawTextArea.setFont(Theme.MONO_FONT);
        matchesRawTextArea.setBackground(Theme.CARD_BG);
        matchesRawTextArea.setForeground(Theme.TEXT_MAIN);
        matchesRawTextArea.setBorder(new EmptyBorder(10, 10, 10, 10));
        JScrollPane textScroll = new JScrollPane(matchesRawTextArea);
        textScroll.setBorder(null);

        feedCardPanel.add(cardScroll, "CARDS");
        feedCardPanel.add(textScroll, "TEXT");

        CardLayout feedLayout = (CardLayout) feedCardPanel.getLayout();
        toggleRawReportCheck.addActionListener(e -> {
            if (toggleRawReportCheck.isSelected()) {
                feedLayout.show(feedCardPanel, "TEXT");
            } else {
                feedLayout.show(feedCardPanel, "CARDS");
            }
        });

        p.add(controlBar, BorderLayout.NORTH);
        p.add(feedCardPanel, BorderLayout.CENTER);

        return p;
    }

    private JPanel buildDirectoryTab() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBackground(Theme.BG_DARK);
        p.setBorder(new EmptyBorder(12, 14, 12, 14));

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Theme.BG_DARK);
        JLabel title = new JLabel("All Registered Users Across The Network:");
        title.setFont(Theme.SUBTITLE_FONT);
        title.setForeground(Theme.TEXT_MAIN);

        ModernButton reloadUsersBtn = new ModernButton("Refresh Users", Theme.PRIMARY, Theme.PRIMARY_HOVER);
        reloadUsersBtn.addActionListener(e -> loadUsers());

        top.add(title, BorderLayout.WEST);
        top.add(reloadUsersBtn, BorderLayout.EAST);

        usersContainer.setLayout(new BoxLayout(usersContainer, BoxLayout.Y_AXIS));
        usersContainer.setBackground(Theme.BG_DARK);
        JScrollPane scroll = new JScrollPane(usersContainer);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);

        p.add(top, BorderLayout.NORTH);
        p.add(scroll, BorderLayout.CENTER);

        return p;
    }

    private JPanel buildMessagingTab() {
        JPanel p = new JPanel(new BorderLayout(10, 10));
        p.setBackground(Theme.BG_DARK);
        p.setBorder(new EmptyBorder(12, 14, 12, 14));

        RoundedPanel sendCard = new RoundedPanel(12, Theme.CARD_BG, Theme.BORDER_SUBTLE);
        sendCard.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 8));

        JLabel toLbl = new JLabel("To User:");
        toLbl.setFont(Theme.BOLD_FONT);
        toLbl.setForeground(Theme.TEXT_MAIN);

        JLabel msgLbl = new JLabel("Message:");
        msgLbl.setFont(Theme.BOLD_FONT);
        msgLbl.setForeground(Theme.TEXT_MAIN);

        ModernButton sendBtn = new ModernButton("Send Message", Theme.PRIMARY, Theme.PRIMARY_HOVER);
        ModernButton exportLogBtn = new ModernButton("Export Chat Log", Theme.INPUT_BG, Theme.CARD_HOVER);

        sendBtn.addActionListener(e -> handleSendMessage());
        exportLogBtn.addActionListener(e -> exportChatLogToFile());
        msgTextField.addActionListener(e -> handleSendMessage());

        sendCard.add(toLbl);
        sendCard.add(msgToField);
        sendCard.add(msgLbl);
        sendCard.add(msgTextField);
        sendCard.add(sendBtn);
        sendCard.add(exportLogBtn);

        inboxTextArea.setEditable(false);
        inboxTextArea.setFont(Theme.MONO_FONT);
        inboxTextArea.setBackground(Theme.CARD_BG);
        inboxTextArea.setForeground(Theme.TEXT_MAIN);
        inboxTextArea.setBorder(new EmptyBorder(12, 12, 12, 12));
        JScrollPane inboxScroll = new JScrollPane(inboxTextArea);
        inboxScroll.setBorder(null);

        p.add(sendCard, BorderLayout.NORTH);
        p.add(inboxScroll, BorderLayout.CENTER);

        return p;
    }

    // =========================================================================
    // 3. FIXED MY PROFILE VIEW (MIRRORS SIGN UP WITH GREYED-OUT USERNAME)
    // =========================================================================
    private JPanel buildProfileTab() {
        JPanel wrap = new JPanel(new GridBagLayout());
        wrap.setBackground(Theme.BG_DARK);

        RoundedPanel card = new RoundedPanel(18, Theme.CARD_BG, Theme.BORDER_SUBTLE);
        card.setPreferredSize(new Dimension(620, 600));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(20, 26, 20, 26));

        JLabel title = new JLabel("My Profile Settings");
        title.setFont(Theme.TITLE_FONT);
        title.setForeground(Theme.TEXT_MAIN);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Greyed out Username
        JLabel nameLbl = new JLabel("Username (Locked / Permanent):");
        nameLbl.setFont(Theme.BOLD_FONT);
        nameLbl.setForeground(Theme.TEXT_MUTED);
        nameLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        profNameField.setEnabled(false);
        profNameField.setEditable(false);
        profNameField.setBackground(Theme.CARD_HOVER);
        profNameField.setForeground(Theme.TEXT_MUTED);

        // Password
        JLabel passLbl = new JLabel("Account Password:");
        passLbl.setFont(Theme.BOLD_FONT);
        passLbl.setForeground(Theme.TEXT_MAIN);
        passLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Contact
        JLabel contactLbl = new JLabel("Contact (Email / Discord / Phone):");
        contactLbl.setFont(Theme.BOLD_FONT);
        contactLbl.setForeground(Theme.TEXT_MAIN);
        contactLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Skills Offered
        JLabel offeredLbl = new JLabel("Skills I Can Teach / Offer (Multiple, comma-separated):");
        offeredLbl.setFont(Theme.BOLD_FONT);
        offeredLbl.setForeground(Theme.SUCCESS);
        offeredLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel quickOffered = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        quickOffered.setOpaque(false);
        quickOffered.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (String skill : new String[]{"Java", "Python", "React", "C++", "DSA", "Unity", "UI/UX"}) {
            JButton chip = createChipButton("+ " + skill, Theme.INPUT_BG, Theme.TEXT_MUTED);
            chip.addActionListener(e -> appendSkill(profOfferedField, skill));
            quickOffered.add(chip);
        }

        profOfferedPills.setOpaque(false);
        profOfferedPills.setAlignmentX(Component.LEFT_ALIGNMENT);
        attachPillUpdater(profOfferedField, profOfferedPills, Theme.SUCCESS);

        // Skills Wanted
        JLabel wantedLbl = new JLabel("Interests / Skills I Want To Learn (Multiple, comma-separated):");
        wantedLbl.setFont(Theme.BOLD_FONT);
        wantedLbl.setForeground(Theme.CYAN);
        wantedLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel quickWanted = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        quickWanted.setOpaque(false);
        quickWanted.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (String skill : new String[]{"Machine Learning", "Godot", "Flutter", "Spring Boot", "Docker"}) {
            JButton chip = createChipButton("+ " + skill, Theme.INPUT_BG, Theme.TEXT_MUTED);
            chip.addActionListener(e -> appendSkill(profWantedField, skill));
            quickWanted.add(chip);
        }

        profWantedPills.setOpaque(false);
        profWantedPills.setAlignmentX(Component.LEFT_ALIGNMENT);
        attachPillUpdater(profWantedField, profWantedPills, Theme.CYAN);

        ModernButton saveProfileBtn = new ModernButton("Save Profile Changes", Theme.PRIMARY, Theme.PRIMARY_HOVER);
        saveProfileBtn.setMaximumSize(new Dimension(Short.MAX_VALUE, 40));
        saveProfileBtn.setAlignmentX(Component.LEFT_ALIGNMENT);

        saveProfileBtn.addActionListener(e -> {
            String name = profNameField.getText().trim();
            String pwd = new String(profPasswordField.getPassword()).trim();
            String contact = profContactField.getText().trim();
            String offered = profOfferedField.getText().trim();
            String wanted = profWantedField.getText().trim();

            if (name.isEmpty() || pwd.isEmpty()) {
                dashboardStatusLabel.setText("Username and password cannot be empty.");
                return;
            }

            dashboardStatusLabel.setText("Updating profile on server...");
            executeNetwork("REGISTER " + name + "|" + pwd + "|" + contact + "|" + offered + "|" + wanted, resp -> {
                if (resp.startsWith("OK")) {
                    currentUserName = name;
                    currentUserPassword = pwd;
                    currentUserContact = contact;
                    currentUserOffered = offered;
                    currentUserWanted = wanted;
                    userSessionLabel.setText("User: " + currentUserName + (contact.isEmpty() ? "" : " (" + contact + ")"));
                    dashboardStatusLabel.setText("Profile updated successfully on server!");
                    loadMatches();
                } else {
                    dashboardStatusLabel.setText(resp);
                }
            });
        });

        card.add(title);
        card.add(Box.createVerticalStrut(10));
        card.add(nameLbl);
        card.add(Box.createVerticalStrut(2));
        card.add(profNameField);
        card.add(Box.createVerticalStrut(6));

        card.add(passLbl);
        card.add(Box.createVerticalStrut(2));
        card.add(profPasswordField);
        card.add(Box.createVerticalStrut(6));

        card.add(contactLbl);
        card.add(Box.createVerticalStrut(2));
        card.add(profContactField);
        card.add(Box.createVerticalStrut(6));

        card.add(offeredLbl);
        card.add(Box.createVerticalStrut(2));
        card.add(profOfferedField);
        card.add(quickOffered);
        card.add(profOfferedPills);
        card.add(Box.createVerticalStrut(4));

        card.add(wantedLbl);
        card.add(Box.createVerticalStrut(2));
        card.add(profWantedField);
        card.add(quickWanted);
        card.add(profWantedPills);
        card.add(Box.createVerticalStrut(10));

        card.add(saveProfileBtn);

        wrap.add(card);
        return wrap;
    }

    private void syncProfileFields() {
        profNameField.setText(currentUserName);
        profPasswordField.setText(currentUserPassword);
        profContactField.setText(currentUserContact);
        profOfferedField.setText(currentUserOffered);
        profWantedField.setText(currentUserWanted);
    }

    // =========================================================================
    // 4. HANDLERS, MATCHMAKING & RELIABLE TAB SWITCHING
    // =========================================================================
    private void handleLogin() {
        String name = loginNameField.getText().trim();
        String password = new String(loginPasswordField.getPassword()).trim();

        if (name.isEmpty() || password.isEmpty()) {
            authStatusLabel.setText("Please enter both username and password.");
            return;
        }

        authStatusLabel.setText("Authenticating credentials for '" + name + "'...");
        executeNetwork("LOGIN " + name + "|" + password, response -> {
            if (response.startsWith("OK|")) {
                String[] parts = response.split("\\|", -1);
                currentUserName = parts.length > 1 ? parts[1].trim() : name;
                currentUserContact = parts.length > 2 ? parts[2].trim() : "";
                currentUserOffered = parts.length > 3 ? parts[3].trim() : "";
                currentUserWanted = parts.length > 4 ? parts[4].trim() : "";
                currentUserPassword = parts.length > 5 ? parts[5].trim() : password;

                userSessionLabel.setText("User: " + currentUserName + (currentUserContact.isEmpty() ? "" : " (" + currentUserContact + ")"));
                syncProfileFields();

                rootCardLayout.show(rootPanel, "DASHBOARD");
                loadMatches();
                autoPollInbox();
                inboxAutoTimer.start(); // Start 1-sec auto polling
            } else {
                authStatusLabel.setText(response.replace("ERROR ", ""));
            }
        });
    }

    private void handleSignUp() {
        String name = suNameField.getText().trim();
        String password = new String(suPasswordField.getPassword()).trim();
        String contact = suContactField.getText().trim();
        String offered = suOfferedField.getText().trim();
        String wanted = suWantedField.getText().trim();

        if (name.isEmpty() || password.isEmpty()) {
            authStatusLabel.setText("Username and password are required.");
            return;
        }

        authStatusLabel.setText("Registering profile for '" + name + "'...");
        executeNetwork("REGISTER " + name + "|" + password + "|" + contact + "|" + offered + "|" + wanted, response -> {
            if (response.startsWith("OK")) {
                currentUserName = name;
                currentUserPassword = password;
                currentUserContact = contact;
                currentUserOffered = offered;
                currentUserWanted = wanted;

                userSessionLabel.setText("User: " + currentUserName + (currentUserContact.isEmpty() ? "" : " (" + currentUserContact + ")"));
                syncProfileFields();

                rootCardLayout.show(rootPanel, "DASHBOARD");
                loadMatches();
                autoPollInbox();
                inboxAutoTimer.start(); // Start 1-sec auto polling
                dashboardStatusLabel.setText("Account created successfully. Welcome to SkillSwap!");
            } else {
                authStatusLabel.setText(response.replace("ERROR ", ""));
            }
        });
    }

    private void handleSignOut() {
        inboxAutoTimer.stop(); // Stop background polling
        currentUserName = "";
        currentUserPassword = "";
        lastInboxContent = "";
        inboxTextArea.setText("");
        rootCardLayout.show(rootPanel, "AUTH");
    }

    /** Direct method taking user to Chat tab and populating recipient */
    private void openDirectMessageWith(String recipientName) {
        msgToField.setText(recipientName);
        if (mainTabbedPane != null) {
            mainTabbedPane.setSelectedIndex(2); // Direct Messaging tab
        }
        SwingUtilities.invokeLater(() -> msgTextField.requestFocusInWindow());
    }

    private void loadMatches() {
        if (currentUserName.isEmpty()) return;
        dashboardStatusLabel.setText("Computing matches and affinity scores...");

        executeNetwork("MATCH_DATA " + currentUserName, dataResponse -> {
            matchesCardsContainer.removeAll();
            String[] lines = dataResponse.split("\n");
            int cardCount = 0;

            for (String line : lines) {
                if (line.startsWith("MATCH_CARD|")) {
                    cardCount++;
                    String[] p = line.substring(11).split("\\|", -1);
                    String peerName = p.length > 0 ? p[0] : "";
                    String peerContact = p.length > 1 ? p[1] : "";
                    String peerOffered = p.length > 2 ? p[2] : "";
                    String peerWanted = p.length > 3 ? p[3] : "";
                    String theyTeachMe = p.length > 4 ? p[4] : "";
                    String iTeachThem = p.length > 5 ? p[5] : "";
                    String shared = p.length > 6 ? p[6] : "";
                    int score = p.length > 7 ? parseInt(p[7], 0) : 0;
                    String matchType = p.length > 8 ? p[8] : "RECIPROCAL";

                    matchesCardsContainer.add(createMatchCard(
                            peerName, peerContact, peerOffered, peerWanted,
                            theyTeachMe, iTeachThem, shared, score, matchType
                    ));
                    matchesCardsContainer.add(Box.createVerticalStrut(10));
                }
            }

            if (cardCount == 0) {
                JPanel emptyPanel = new JPanel();
                emptyPanel.setOpaque(false);
                emptyPanel.setBorder(new EmptyBorder(30, 20, 30, 20));
                JLabel emptyLbl = new JLabel("No matching peers found yet. Check back soon or add more skills/interests!");
                emptyLbl.setFont(Theme.SUBTITLE_FONT);
                emptyLbl.setForeground(Theme.TEXT_MUTED);
                emptyPanel.add(emptyLbl);
                matchesCardsContainer.add(emptyPanel);
            }

            matchesCardsContainer.revalidate();
            matchesCardsContainer.repaint();
            dashboardStatusLabel.setText("Found " + cardCount + " compatible match(es) for " + currentUserName);
        });

        // Also update text view
        executeNetwork("MATCH " + currentUserName, reportResponse -> {
            matchesRawTextArea.setText(reportResponse);
            matchesRawTextArea.setCaretPosition(0);
        });
    }

    private JPanel createMatchCard(String name, String contact, String peerOffered, String peerWanted,
                                   String theyTeachMe, String iTeachThem, String shared, int score, String matchType) {
        RoundedPanel card = new RoundedPanel(14, Theme.CARD_BG, Theme.BORDER_SUBTLE);
        card.setLayout(new BorderLayout(12, 10));
        card.setBorder(new EmptyBorder(14, 16, 14, 16));
        card.setMaximumSize(new Dimension(Short.MAX_VALUE, 155));

        // Top Row
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);

        JPanel userMeta = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        userMeta.setOpaque(false);

        JLabel avatar = new AvatarBadge(name);
        JLabel nameLbl = new JLabel(name);
        nameLbl.setFont(Theme.SUBTITLE_FONT);
        nameLbl.setForeground(Theme.TEXT_MAIN);

        JLabel contactLbl = new JLabel(contact.isEmpty() ? "" : "(" + contact + ")");
        contactLbl.setFont(Theme.SMALL_FONT);
        contactLbl.setForeground(Theme.TEXT_MUTED);

        userMeta.add(avatar);
        userMeta.add(nameLbl);
        userMeta.add(contactLbl);

        JPanel affinityBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        affinityBox.setOpaque(false);

        Color affinityColor = score >= 80 ? Theme.SUCCESS : (score >= 50 ? Theme.CYAN : Theme.AMBER);
        String typeBadgeText = matchType.equals("RECIPROCAL") ? "Reciprocal Swap" :
                              (matchType.equals("MENTOR") ? "Can Teach You" :
                              (matchType.equals("LEARNER") ? "Wants Your Skills" : "Study Buddy"));

        JLabel typeBadge = new PillBadge(typeBadgeText, affinityColor);
        JLabel scoreLabel = new JLabel(score + "% Affinity");
        scoreLabel.setFont(Theme.SUBTITLE_FONT);
        scoreLabel.setForeground(affinityColor);

        affinityBox.add(typeBadge);
        affinityBox.add(scoreLabel);

        topRow.add(userMeta, BorderLayout.WEST);
        topRow.add(affinityBox, BorderLayout.EAST);

        // Center Breakdowns
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);

        if (!theyTeachMe.isEmpty()) {
            JPanel r1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            r1.setOpaque(false);
            JLabel lbl = new JLabel("They Can Teach You: ");
            lbl.setFont(Theme.BOLD_FONT);
            lbl.setForeground(Theme.CYAN);
            r1.add(lbl);
            for (String s : theyTeachMe.split(",")) {
                if (!s.trim().isEmpty()) r1.add(new PillBadge(s.trim(), Theme.CYAN));
            }
            center.add(r1);
            center.add(Box.createVerticalStrut(4));
        }

        if (!iTeachThem.isEmpty()) {
            JPanel r2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            r2.setOpaque(false);
            JLabel lbl = new JLabel("You Can Teach Them: ");
            lbl.setFont(Theme.BOLD_FONT);
            lbl.setForeground(Theme.SUCCESS);
            r2.add(lbl);
            for (String s : iTeachThem.split(",")) {
                if (!s.trim().isEmpty()) r2.add(new PillBadge(s.trim(), Theme.SUCCESS));
            }
            center.add(r2);
            center.add(Box.createVerticalStrut(4));
        }

        if (!shared.isEmpty()) {
            JPanel r3 = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            r3.setOpaque(false);
            JLabel lbl = new JLabel("Shared Learning Interests: ");
            lbl.setFont(Theme.BOLD_FONT);
            lbl.setForeground(Theme.PURPLE);
            r3.add(lbl);
            for (String s : shared.split(",")) {
                if (!s.trim().isEmpty()) r3.add(new PillBadge(s.trim(), Theme.PURPLE));
            }
            center.add(r3);
        }

        // Bottom Row
        JPanel bottomRow = new JPanel(new BorderLayout());
        bottomRow.setOpaque(false);

        JLabel allSkillsLbl = new JLabel("All offered: [" + peerOffered + "] | All wanted: [" + peerWanted + "]");
        allSkillsLbl.setFont(Theme.SMALL_FONT);
        allSkillsLbl.setForeground(Theme.TEXT_MUTED);

        ModernButton chatBtn = new ModernButton("Message " + name, Theme.PRIMARY, Theme.PRIMARY_HOVER);
        // Direct reliable call to open chat
        chatBtn.addActionListener(e -> openDirectMessageWith(name));

        bottomRow.add(allSkillsLbl, BorderLayout.WEST);
        bottomRow.add(chatBtn, BorderLayout.EAST);

        card.add(topRow, BorderLayout.NORTH);
        card.add(center, BorderLayout.CENTER);
        card.add(bottomRow, BorderLayout.SOUTH);

        return card;
    }

    private void loadUsers() {
        dashboardStatusLabel.setText("Loading user directory...");
        executeNetwork("USERS", response -> {
            usersContainer.removeAll();
            String[] lines = response.split("\n");
            String curName = "";
            String curContact = "";
            String curOffers = "";
            String curWants = "";

            for (String line : lines) {
                if (line.startsWith("User   : ")) {
                    curName = line.substring(9).trim();
                    int paren = curName.indexOf('(');
                    if (paren > 0) {
                        curContact = curName.substring(paren + 1).replace(")", "").trim();
                        curName = curName.substring(0, paren).trim();
                    }
                } else if (line.startsWith("OFFERS : ")) {
                    curOffers = line.substring(9).trim();
                } else if (line.startsWith("WANTS  : ")) {
                    curWants = line.substring(9).trim();
                    if (!curName.isEmpty()) {
                        usersContainer.add(createDirectoryCard(curName, curContact, curOffers, curWants));
                        usersContainer.add(Box.createVerticalStrut(8));
                        curName = "";
                    }
                }
            }
            usersContainer.revalidate();
            usersContainer.repaint();
            dashboardStatusLabel.setText("Directory loaded.");
        });
    }

    private JPanel createDirectoryCard(String name, String contact, String offers, String wants) {
        RoundedPanel card = new RoundedPanel(12, Theme.CARD_BG, Theme.BORDER_SUBTLE);
        card.setLayout(new BorderLayout(8, 6));
        card.setBorder(new EmptyBorder(10, 14, 10, 14));
        card.setMaximumSize(new Dimension(Short.MAX_VALUE, 90));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        left.setOpaque(false);
        left.add(new AvatarBadge(name));
        JPanel info = new JPanel();
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.setOpaque(false);
        JLabel n = new JLabel(name + (contact.isEmpty() ? "" : "  •  " + contact));
        n.setFont(Theme.SUBTITLE_FONT);
        n.setForeground(Theme.TEXT_MAIN);

        JLabel off = new JLabel("Can Teach: " + offers);
        off.setFont(Theme.SMALL_FONT);
        off.setForeground(Theme.SUCCESS);

        JLabel wnt = new JLabel("Wants To Learn: " + wants);
        wnt.setFont(Theme.SMALL_FONT);
        wnt.setForeground(Theme.CYAN);

        info.add(n);
        info.add(off);
        info.add(wnt);
        left.add(info);

        ModernButton msgBtn = new ModernButton("Send Message", Theme.INPUT_BG, Theme.PRIMARY);
        msgBtn.addActionListener(e -> openDirectMessageWith(name));

        card.add(left, BorderLayout.WEST);
        card.add(msgBtn, BorderLayout.EAST);
        return card;
    }

    private void handleSendMessage() {
        String from = currentUserName;
        String to = msgToField.getText().trim();
        String msg = msgTextField.getText().trim();

        if (from.isEmpty()) {
            dashboardStatusLabel.setText("Please log in first.");
            return;
        }
        if (to.isEmpty() || msg.isEmpty()) {
            dashboardStatusLabel.setText("Please enter both recipient and message text.");
            return;
        }

        dashboardStatusLabel.setText("Sending message to " + to + "...");
        executeNetwork("SEND_MSG " + from + "|" + to + "|" + msg, resp -> {
            dashboardStatusLabel.setText(resp);
            msgTextField.setText("");
            autoPollInbox(); // Immediately refresh inbox
        });
    }

    // 1-second auto inbox poll without freezing GUI
    private void autoPollInbox() {
        if (currentUserName == null || currentUserName.trim().isEmpty()) return;
        executeNetworkSilent("GET_MSGS " + currentUserName, resp -> {
            if (!resp.equals(lastInboxContent)) {
                lastInboxContent = resp;
                inboxTextArea.setText(resp);
                inboxTextArea.setCaretPosition(inboxTextArea.getDocument().getLength());
            }
        });
    }

    // [CO4]: File Operations - Exporting chat history to a local text file
    private void exportChatLogToFile() {
        String text = inboxTextArea.getText().trim();
        if (text.isEmpty()) {
            dashboardStatusLabel.setText("No messages to export.");
            return;
        }

        String fileName = "chat_log_" + currentUserName.toLowerCase() + ".txt";
        File file = new File(fileName);
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            bw.write("SkillSwap Chat Log for: " + currentUserName + "\n");
            bw.write("Exported at: " + new Date() + "\n");
            bw.write("==================================================\n\n");
            bw.write(text);
            bw.write("\n\n==================================================\n");
            dashboardStatusLabel.setText("Chat log successfully saved to: " + file.getAbsolutePath());
        } catch (IOException ex) {
            dashboardStatusLabel.setText("Failed to export chat: " + ex.getMessage());
        }
    }

    // =========================================================================
    // 5. NETWORKING UTILITIES
    // =========================================================================
    private void executeNetwork(String request, java.util.function.Consumer<String> callback) {
        String host = hostField.getText().trim();
        if (host.isEmpty()) host = "localhost";
        int port = parseInt(portField.getText().trim(), 5050);

        final String targetHost = host;
        final int targetPort = port;

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
                sb.append("ERROR Connection failed to ").append(targetHost).append(":").append(targetPort)
                  .append(" (").append(ex.getMessage()).append(")");
            }
            String result = sb.toString().trim();
            SwingUtilities.invokeLater(() -> callback.accept(result));
        }).start();
    }

    private void executeNetworkSilent(String request, java.util.function.Consumer<String> callback) {
        String host = hostField.getText().trim();
        final String targetHost = host.isEmpty() ? "localhost" : host;
        final int targetPort = parseInt(portField.getText().trim(), 5050);

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
                String result = sb.toString().trim();
                SwingUtilities.invokeLater(() -> callback.accept(result));
            } catch (Exception ignored) {
                // Silent fail for polling background thread
            }
        }).start();
    }

    private static int parseInt(String val, int def) {
        try { return Integer.parseInt(val.trim()); } catch (Exception e) { return def; }
    }

    // =========================================================================
    // 6. CUSTOM MODERN SWING CONTROLS (BOUNDED DIMENSIONS)
    // =========================================================================
    private static JTextField createStyledTextField(String text, int cols) {
        JTextField tf = new JTextField(text, cols) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                super.paintComponent(g);
                g2.dispose();
            }
        };
        tf.setOpaque(false);
        tf.setBackground(Theme.INPUT_BG);
        tf.setForeground(Theme.TEXT_MAIN);
        tf.setCaretColor(Theme.TEXT_MAIN);
        tf.setFont(Theme.REGULAR_FONT);
        tf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.INPUT_BORDER, 1, true),
                new EmptyBorder(6, 10, 6, 10)
        ));
        // Crucial fix: bounds maximumSize so BoxLayout never stretches vertically
        tf.setMaximumSize(new Dimension(Short.MAX_VALUE, 36));
        tf.setPreferredSize(new Dimension(280, 36));
        tf.setAlignmentX(Component.LEFT_ALIGNMENT);
        return tf;
    }

    private static JPasswordField createStyledPasswordField(String text, int cols) {
        JPasswordField pf = new JPasswordField(text, cols) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                super.paintComponent(g);
                g2.dispose();
            }
        };
        pf.setOpaque(false);
        pf.setBackground(Theme.INPUT_BG);
        pf.setForeground(Theme.TEXT_MAIN);
        pf.setCaretColor(Theme.TEXT_MAIN);
        pf.setFont(Theme.REGULAR_FONT);
        pf.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.INPUT_BORDER, 1, true),
                new EmptyBorder(6, 10, 6, 10)
        ));
        // Crucial fix: bounds maximumSize so BoxLayout never stretches vertically
        pf.setMaximumSize(new Dimension(Short.MAX_VALUE, 36));
        pf.setPreferredSize(new Dimension(280, 36));
        pf.setAlignmentX(Component.LEFT_ALIGNMENT);
        return pf;
    }

    private static JButton createChipButton(String text, Color bg, Color fg) {
        JButton b = new JButton(text);
        b.setFont(Theme.SMALL_FONT);
        b.setForeground(fg);
        b.setBackground(bg);
        b.setFocusPainted(false);
        b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Theme.BORDER_SUBTLE, 1, true),
                new EmptyBorder(3, 8, 3, 8)
        ));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    private static JButton createTextLinkButton(String text) {
        JButton b = new JButton(text);
        b.setFont(Theme.SMALL_FONT);
        b.setForeground(Theme.PRIMARY_HOVER);
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    public static class ModernButton extends JButton {
        private Color bgColor;
        private final Color hoverColor;

        public ModernButton(String text, Color bg, Color hover) {
            super(text);
            this.bgColor = bg;
            this.hoverColor = hover;
            setFont(Theme.BOLD_FONT);
            setForeground(Theme.TEXT_MAIN);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(8, 14, 8, 14));

            addMouseListener(new MouseAdapter() {
                public void mouseEntered(MouseEvent e) { setBackground(hoverColor); repaint(); }
                public void mouseExited(MouseEvent e) { setBackground(bgColor); repaint(); }
            });
            setBackground(bg);
        }

        public void setBackgroundColor(Color c) {
            this.bgColor = c;
            setBackground(c);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
            super.paintComponent(g);
            g2.dispose();
        }
    }

    public static class RoundedPanel extends JPanel {
        private final int radius;
        private final Color bgColor;
        private final Color borderColor;

        public RoundedPanel(int radius, Color bg, Color border) {
            this.radius = radius;
            this.bgColor = bg;
            this.borderColor = border;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bgColor);
            g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            if (borderColor != null) {
                g2.setColor(borderColor);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radius, radius);
            }
            super.paintComponent(g);
            g2.dispose();
        }
    }

    public static class PillBadge extends JLabel {
        private final Color color;

        public PillBadge(String text, Color c) {
            super(" " + text + " ");
            this.color = c;
            setFont(Theme.SMALL_FONT);
            setForeground(Color.WHITE);
            setOpaque(false);
            setBorder(new EmptyBorder(2, 6, 2, 6));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 60));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
            g2.setColor(color);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
            super.paintComponent(g);
            g2.dispose();
        }
    }

    public static class AvatarBadge extends JLabel {
        public AvatarBadge(String name) {
            String initials = name == null || name.isEmpty() ? "?" : name.substring(0, Math.min(2, name.length())).toUpperCase();
            setText(initials);
            setFont(Theme.BOLD_FONT);
            setForeground(Color.WHITE);
            setHorizontalAlignment(SwingConstants.CENTER);
            setPreferredSize(new Dimension(36, 36));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(Theme.PRIMARY);
            g2.fillOval(0, 0, getWidth(), getHeight());
            super.paintComponent(g);
            g2.dispose();
        }
    }

    public static void main(String[] args) {
        String host = (args.length > 0) ? args[0] : "localhost";
        SwingUtilities.invokeLater(() -> new SkillSwapClient(host).setVisible(true));
    }
}

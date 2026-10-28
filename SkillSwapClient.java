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
 * Modern Presentation Tier for SkillSwap.
 * Features:
 * - Authentication flow on startup (Sign In vs Sign Up / Add Profile).
 * - Multi-skill and multi-interest management with quick-add chips and live pill preview.
 * - Dynamic Matchmaking feed displaying calculated Affinity Scores, Compatibility badges,
 *   and reciprocal skill breakdown cards.
 * - Direct peer-to-peer messaging and community exploration.
 */
public class SkillSwapClient extends JFrame {

    // --- UI Theme Palette ---
    public static class Theme {
        public static final Color BG_DARK       = new Color(22, 24, 34);     // Main window background
        public static final Color CARD_BG       = new Color(30, 33, 48);     // Cards & containers
        public static final Color CARD_HOVER    = new Color(38, 42, 60);     // Hover state
        public static final Color INPUT_BG      = new Color(40, 44, 64);     // Text fields
        public static final Color INPUT_BORDER  = new Color(60, 66, 95);     // Field borders
        public static final Color PRIMARY       = new Color(99, 102, 241);   // Indigo primary button
        public static final Color PRIMARY_HOVER = new Color(129, 140, 248);  // Primary hover
        public static final Color SUCCESS       = new Color(34, 197, 94);    // Green for 80%+ Affinity
        public static final Color CYAN          = new Color(56, 189, 248);   // Blue/Cyan for Mentors
        public static final Color AMBER         = new Color(245, 158, 11);   // Amber for Learners
        public static final Color PURPLE        = new Color(168, 85, 247);   // Purple for Shared Interests
        public static final Color TEXT_MAIN     = new Color(243, 244, 246);  // Bright white-grey
        public static final Color TEXT_MUTED    = new Color(156, 163, 175);  // Subtle grey
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
    private String currentUserContact = "";
    private String currentUserOffered = "";
    private String currentUserWanted = "";

    // --- CardLayout Root ---
    private final CardLayout rootCardLayout = new CardLayout();
    private final JPanel rootPanel = new JPanel(rootCardLayout);

    // --- Auth Screen Components ---
    private final CardLayout authCardLayout = new CardLayout();
    private final JPanel authCardContainer = new JPanel(authCardLayout);
    private final JTextField loginNameField = createStyledTextField("", 16);

    // Sign Up (Add Profile) Fields
    private final JTextField suNameField = createStyledTextField("", 16);
    private final JTextField suContactField = createStyledTextField("", 16);
    private final JTextField suOfferedField = createStyledTextField("", 20);
    private final JTextField suWantedField = createStyledTextField("", 20);
    private final JPanel offeredPillsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
    private final JPanel wantedPillsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 4));
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

    public SkillSwapClient() {
        this("localhost");
    }

    public SkillSwapClient(String defaultHost) {
        super("SkillSwap — Peer-to-Peer Skill Matching & Communication Platform");
        hostField.setText(defaultHost == null || defaultHost.isBlank() ? "localhost" : defaultHost.trim());

        setupWindow();
        initAuthScreen();
        initDashboardScreen();

        rootCardLayout.show(rootPanel, "AUTH");
        add(rootPanel);

        // Preload default demo user if desired
        loginNameField.setText("Alice");
    }

    private void setupWindow() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1020, 760);
        setMinimumSize(new Dimension(860, 640));
        setLocationRelativeTo(null);
        getContentPane().setBackground(Theme.BG_DARK);
    }

    // =========================================================================
    // 1. AUTHENTICATION SCREEN (LOGIN & SIGN UP / ADD PROFILE)
    // =========================================================================
    private void initAuthScreen() {
        JPanel authView = new JPanel(new BorderLayout());
        authView.setBackground(Theme.BG_DARK);

        // Top Network Strip
        JPanel topNetworkBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 8));
        topNetworkBar.setBackground(Theme.BG_DARK);
        topNetworkBar.setBorder(new EmptyBorder(4, 12, 4, 12));

        JLabel netDot = new JLabel("●");
        netDot.setForeground(Theme.SUCCESS);
        JLabel netLabel = new JLabel("Server IP:");
        netLabel.setFont(Theme.SMALL_FONT);
        netLabel.setForeground(Theme.TEXT_MUTED);
        JLabel portLabel = new JLabel("Port:");
        portLabel.setFont(Theme.SMALL_FONT);
        portLabel.setForeground(Theme.TEXT_MUTED);

        topNetworkBar.add(netDot);
        topNetworkBar.add(netLabel);
        topNetworkBar.add(hostField);
        topNetworkBar.add(portLabel);
        topNetworkBar.add(portField);

        // Center Box
        JPanel centerWrap = new JPanel(new GridBagLayout());
        centerWrap.setBackground(Theme.BG_DARK);

        RoundedPanel authBox = new RoundedPanel(18, Theme.CARD_BG, Theme.BORDER_SUBTLE);
        authBox.setPreferredSize(new Dimension(580, 610));
        authBox.setLayout(new BorderLayout());
        authBox.setBorder(new EmptyBorder(20, 24, 20, 24));

        // Header
        JPanel headerPanel = new JPanel();
        headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
        headerPanel.setOpaque(false);

        JLabel logoLabel = new JLabel("🔄 SkillSwap");
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
        switcherPanel.setPreferredSize(new Dimension(320, 36));

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
        headerPanel.add(Box.createVerticalStrut(12));

        // Forms Container
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
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(16, 10, 10, 10));

        JLabel info = new JLabel("Enter your registered username to continue:");
        info.setFont(Theme.REGULAR_FONT);
        info.setForeground(Theme.TEXT_MUTED);

        JLabel userLbl = new JLabel("Username / Name:");
        userLbl.setFont(Theme.BOLD_FONT);
        userLbl.setForeground(Theme.TEXT_MAIN);

        ModernButton loginBtn = new ModernButton("Sign In to SkillSwap", Theme.PRIMARY, Theme.PRIMARY_HOVER);
        loginBtn.setMaximumSize(new Dimension(Short.MAX_VALUE, 42));

        loginBtn.addActionListener(e -> handleLogin());
        loginNameField.addActionListener(e -> handleLogin());

        p.add(info);
        p.add(Box.createVerticalStrut(14));
        p.add(userLbl);
        p.add(Box.createVerticalStrut(6));
        p.add(loginNameField);
        p.add(Box.createVerticalStrut(18));
        p.add(loginBtn);
        p.add(Box.createVerticalStrut(16));

        // Quick demo user buttons
        JLabel quickLbl = new JLabel("Quick Demo Profiles:");
        quickLbl.setFont(Theme.SMALL_FONT);
        quickLbl.setForeground(Theme.TEXT_MUTED);
        p.add(quickLbl);
        p.add(Box.createVerticalStrut(6));

        JPanel demoBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        demoBar.setOpaque(false);
        for (String demo : new String[]{"Alice", "Bob", "Cara", "David"}) {
            JButton b = createChipButton(demo, Theme.CARD_HOVER, Theme.TEXT_MAIN);
            b.addActionListener(ev -> {
                loginNameField.setText(demo);
                handleLogin();
            });
            demoBar.add(b);
        }
        p.add(demoBar);

        p.add(Box.createVerticalGlue());

        JButton linkBtn = createTextLinkButton("Don't have a profile yet? Click here to Sign Up →");
        linkBtn.addActionListener(e -> switchToSignUpBtn.doClick());
        linkBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(linkBtn);

        return p;
    }

    private JPanel buildSignUpForm(ModernButton switchToSignInBtn) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(6, 4, 4, 4));

        // Name
        JLabel nameLbl = new JLabel("Full Name / Username *:");
        nameLbl.setFont(Theme.BOLD_FONT);
        nameLbl.setForeground(Theme.TEXT_MAIN);

        // Contact
        JLabel contactLbl = new JLabel("Contact (Email / Discord / Phone) *:");
        contactLbl.setFont(Theme.BOLD_FONT);
        contactLbl.setForeground(Theme.TEXT_MAIN);

        // Skills Offered
        JLabel offeredLbl = new JLabel("Skills I Can Teach / Offer (Multiple, comma-separated) *:");
        offeredLbl.setFont(Theme.BOLD_FONT);
        offeredLbl.setForeground(Theme.SUCCESS);

        JPanel quickOffered = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        quickOffered.setOpaque(false);
        for (String skill : new String[]{"Java", "Python", "React", "C++", "DSA", "Unity", "UI/UX"}) {
            JButton chip = createChipButton("+ " + skill, Theme.INPUT_BG, Theme.TEXT_MUTED);
            chip.addActionListener(e -> appendSkill(suOfferedField, skill));
            quickOffered.add(chip);
        }

        offeredPillsPanel.setOpaque(false);
        attachPillUpdater(suOfferedField, offeredPillsPanel, Theme.SUCCESS);

        // Skills Wanted (Interests)
        JLabel wantedLbl = new JLabel("Interests / Skills I Want To Learn (Multiple, comma-separated) *:");
        wantedLbl.setFont(Theme.BOLD_FONT);
        wantedLbl.setForeground(Theme.CYAN);

        JPanel quickWanted = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        quickWanted.setOpaque(false);
        for (String skill : new String[]{"Machine Learning", "Godot", "Flutter", "Spring Boot", "Docker"}) {
            JButton chip = createChipButton("+ " + skill, Theme.INPUT_BG, Theme.TEXT_MUTED);
            chip.addActionListener(e -> appendSkill(suWantedField, skill));
            quickWanted.add(chip);
        }

        wantedPillsPanel.setOpaque(false);
        attachPillUpdater(suWantedField, wantedPillsPanel, Theme.CYAN);

        ModernButton signUpBtn = new ModernButton("Complete Sign Up & Enter Dashboard", Theme.PRIMARY, Theme.PRIMARY_HOVER);
        signUpBtn.setMaximumSize(new Dimension(Short.MAX_VALUE, 40));
        signUpBtn.addActionListener(e -> handleSignUp());

        p.add(nameLbl);
        p.add(Box.createVerticalStrut(3));
        p.add(suNameField);
        p.add(Box.createVerticalStrut(6));

        p.add(contactLbl);
        p.add(Box.createVerticalStrut(3));
        p.add(suContactField);
        p.add(Box.createVerticalStrut(8));

        p.add(offeredLbl);
        p.add(Box.createVerticalStrut(3));
        p.add(suOfferedField);
        p.add(Box.createVerticalStrut(2));
        p.add(quickOffered);
        p.add(offeredPillsPanel);
        p.add(Box.createVerticalStrut(6));

        p.add(wantedLbl);
        p.add(Box.createVerticalStrut(3));
        p.add(suWantedField);
        p.add(Box.createVerticalStrut(2));
        p.add(quickWanted);
        p.add(wantedPillsPanel);
        p.add(Box.createVerticalStrut(10));

        p.add(signUpBtn);

        JButton link = createTextLinkButton("Already registered? Sign In instead");
        link.addActionListener(e -> switchToSignInBtn.doClick());
        link.setAlignmentX(Component.CENTER_ALIGNMENT);
        p.add(Box.createVerticalStrut(4));
        p.add(link);

        return p;
    }

    private void appendSkill(JTextField field, String skill) {
        String current = field.getText().trim();
        if (current.isEmpty()) {
            field.setText(skill);
        } else {
            // check if not already present
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
    // 2. MAIN DASHBOARD SCREEN (MATCHMAKING, DIRECTORY, CHAT)
    // =========================================================================
    private void initDashboardScreen() {
        JPanel dashView = new JPanel(new BorderLayout());
        dashView.setBackground(Theme.BG_DARK);

        // Top Navigation Bar
        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(Theme.CARD_BG);
        topBar.setBorder(new EmptyBorder(10, 16, 10, 16));

        JPanel brandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        brandPanel.setOpaque(false);
        JLabel logo = new JLabel("🔄 SkillSwap");
        logo.setFont(Theme.SUBTITLE_FONT);
        logo.setForeground(Theme.TEXT_MAIN);
        brandPanel.add(logo);

        JPanel rightUserPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightUserPanel.setOpaque(false);

        userSessionLabel.setFont(Theme.REGULAR_FONT);
        userSessionLabel.setForeground(Theme.CYAN);

        ModernButton refreshBtn = new ModernButton("⚡ Find Matches", Theme.PRIMARY, Theme.PRIMARY_HOVER);
        refreshBtn.addActionListener(e -> loadMatches());

        ModernButton signOutBtn = new ModernButton("Sign Out", Theme.INPUT_BG, Theme.CARD_HOVER);
        signOutBtn.addActionListener(e -> {
            currentUserName = "";
            rootCardLayout.show(rootPanel, "AUTH");
        });

        rightUserPanel.add(userSessionLabel);
        rightUserPanel.add(refreshBtn);
        rightUserPanel.add(signOutBtn);

        topBar.add(brandPanel, BorderLayout.WEST);
        topBar.add(rightUserPanel, BorderLayout.EAST);

        // Center Content Tabs
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setBackground(Theme.BG_DARK);
        tabbedPane.setForeground(Theme.TEXT_MAIN);
        tabbedPane.setFont(Theme.BOLD_FONT);

        tabbedPane.addTab("🎯 Matchmaking & Affinity Feed", buildMatchesTab());
        tabbedPane.addTab("👥 Community Directory", buildDirectoryTab());
        tabbedPane.addTab("💬 Direct Messaging & Inbox", buildMessagingTab());
        tabbedPane.addTab("⚙️ My Profile", buildProfileTab());

        // Bottom Status Bar
        JPanel bottomBar = new JPanel(new BorderLayout());
        bottomBar.setBackground(Theme.CARD_BG);
        bottomBar.setBorder(new EmptyBorder(6, 14, 6, 14));
        dashboardStatusLabel.setFont(Theme.SMALL_FONT);
        dashboardStatusLabel.setForeground(Theme.TEXT_MUTED);
        bottomBar.add(dashboardStatusLabel, BorderLayout.WEST);

        dashView.add(topBar, BorderLayout.NORTH);
        dashView.add(tabbedPane, BorderLayout.CENTER);
        dashView.add(bottomBar, BorderLayout.SOUTH);

        rootPanel.add(dashView, "DASHBOARD");
    }

    private JPanel buildMatchesTab() {
        JPanel p = new JPanel(new BorderLayout(8, 8));
        p.setBackground(Theme.BG_DARK);
        p.setBorder(new EmptyBorder(12, 14, 12, 14));

        // Sub-header controls
        JPanel controlBar = new JPanel(new BorderLayout());
        controlBar.setBackground(Theme.BG_DARK);

        JLabel intro = new JLabel("Compatible Peers & Mentors ranked by calculated Affinity Score:");
        intro.setFont(Theme.SUBTITLE_FONT);
        intro.setForeground(Theme.TEXT_MAIN);

        JPanel rightControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightControls.setOpaque(false);

        toggleRawReportCheck.setFont(Theme.SMALL_FONT);
        toggleRawReportCheck.setForeground(Theme.TEXT_MUTED);
        toggleRawReportCheck.setOpaque(false);

        ModernButton matchBtn = new ModernButton("🔄 Refresh Matches", Theme.PRIMARY, Theme.PRIMARY_HOVER);
        matchBtn.addActionListener(e -> loadMatches());

        rightControls.add(toggleRawReportCheck);
        rightControls.add(matchBtn);

        controlBar.add(intro, BorderLayout.WEST);
        controlBar.add(rightControls, BorderLayout.EAST);

        // Feed Container (Card View vs Raw Text View)
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

        // Send Form
        RoundedPanel sendCard = new RoundedPanel(12, Theme.CARD_BG, Theme.BORDER_SUBTLE);
        sendCard.setLayout(new FlowLayout(FlowLayout.LEFT, 10, 8));

        JLabel toLbl = new JLabel("To User:");
        toLbl.setFont(Theme.BOLD_FONT);
        toLbl.setForeground(Theme.TEXT_MAIN);

        JLabel msgLbl = new JLabel("Message:");
        msgLbl.setFont(Theme.BOLD_FONT);
        msgLbl.setForeground(Theme.TEXT_MAIN);

        ModernButton sendBtn = new ModernButton("Send Message", Theme.PRIMARY, Theme.PRIMARY_HOVER);
        ModernButton checkInboxBtn = new ModernButton("Check Inbox", Theme.INPUT_BG, Theme.CARD_HOVER);

        sendBtn.addActionListener(e -> handleSendMessage());
        checkInboxBtn.addActionListener(e -> handleCheckInbox());
        msgTextField.addActionListener(e -> handleSendMessage());

        sendCard.add(toLbl);
        sendCard.add(msgToField);
        sendCard.add(msgLbl);
        sendCard.add(msgTextField);
        sendCard.add(sendBtn);
        sendCard.add(checkInboxBtn);

        // Inbox View
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

    private JPanel buildProfileTab() {
        JPanel wrap = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
        wrap.setBackground(Theme.BG_DARK);

        RoundedPanel card = new RoundedPanel(16, Theme.CARD_BG, Theme.BORDER_SUBTLE);
        card.setPreferredSize(new Dimension(540, 480));
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel title = new JLabel("Active User Profile Settings");
        title.setFont(Theme.TITLE_FONT);
        title.setForeground(Theme.TEXT_MAIN);

        JTextField editName = createStyledTextField("", 16);
        JTextField editContact = createStyledTextField("", 16);
        JTextField editOffered = createStyledTextField("", 20);
        JTextField editWanted = createStyledTextField("", 20);

        ModernButton saveEditBtn = new ModernButton("Update Profile on Server", Theme.PRIMARY, Theme.PRIMARY_HOVER);
        saveEditBtn.addActionListener(e -> {
            String name = editName.getText().trim();
            if (name.isEmpty()) return;
            executeNetwork("REGISTER " + name + "|" + editContact.getText().trim()
                    + "|" + editOffered.getText().trim() + "|" + editWanted.getText().trim(), resp -> {
                currentUserName = name;
                currentUserContact = editContact.getText().trim();
                currentUserOffered = editOffered.getText().trim();
                currentUserWanted = editWanted.getText().trim();
                userSessionLabel.setText("👤 " + currentUserName + " (" + currentUserContact + ")");
                dashboardStatusLabel.setText("Profile updated successfully!");
                loadMatches();
            });
        });

        // Populate initially
        editName.setText(currentUserName);
        editContact.setText(currentUserContact);
        editOffered.setText(currentUserOffered);
        editWanted.setText(currentUserWanted);

        card.add(title);
        card.add(Box.createVerticalStrut(14));
        card.add(new JLabel("Username:"));
        card.add(editName);
        card.add(Box.createVerticalStrut(8));
        card.add(new JLabel("Contact Information:"));
        card.add(editContact);
        card.add(Box.createVerticalStrut(8));
        card.add(new JLabel("Skills Offered (Teaching):"));
        card.add(editOffered);
        card.add(Box.createVerticalStrut(8));
        card.add(new JLabel("Interests (Learning Goals):"));
        card.add(editWanted);
        card.add(Box.createVerticalStrut(16));
        card.add(saveEditBtn);

        wrap.add(card);
        return wrap;
    }

    // =========================================================================
    // 3. ACTION HANDLERS & MATCHMAKING LOGIC
    // =========================================================================
    private void handleLogin() {
        String name = loginNameField.getText().trim();
        if (name.isEmpty()) {
            authStatusLabel.setText("Please enter your name.");
            return;
        }

        authStatusLabel.setText("Signing in as '" + name + "'...");
        executeNetwork("LOGIN " + name, response -> {
            if (response.startsWith("OK|")) {
                String[] parts = response.split("\\|", -1);
                currentUserName = parts.length > 1 ? parts[1].trim() : name;
                currentUserContact = parts.length > 2 ? parts[2].trim() : "";
                currentUserOffered = parts.length > 3 ? parts[3].trim() : "";
                currentUserWanted = parts.length > 4 ? parts[4].trim() : "";

                userSessionLabel.setText("👤 " + currentUserName + (currentUserContact.isEmpty() ? "" : " (" + currentUserContact + ")"));
                rootCardLayout.show(rootPanel, "DASHBOARD");
                loadMatches();
                handleCheckInbox();
            } else {
                authStatusLabel.setText(response.replace("ERROR ", ""));
            }
        });
    }

    private void handleSignUp() {
        String name = suNameField.getText().trim();
        String contact = suContactField.getText().trim();
        String offered = suOfferedField.getText().trim();
        String wanted = suWantedField.getText().trim();

        if (name.isEmpty()) {
            authStatusLabel.setText("Please enter a username.");
            return;
        }

        authStatusLabel.setText("Registering profile for '" + name + "'...");
        executeNetwork("REGISTER " + name + "|" + contact + "|" + offered + "|" + wanted, response -> {
            if (response.startsWith("OK")) {
                currentUserName = name;
                currentUserContact = contact;
                currentUserOffered = offered;
                currentUserWanted = wanted;

                userSessionLabel.setText("👤 " + currentUserName + (currentUserContact.isEmpty() ? "" : " (" + currentUserContact + ")"));
                rootCardLayout.show(rootPanel, "DASHBOARD");
                loadMatches();
                dashboardStatusLabel.setText("Account created successfully! Welcome to SkillSwap.");
            } else {
                authStatusLabel.setText(response.replace("ERROR ", ""));
            }
        });
    }

    private void loadMatches() {
        if (currentUserName.isEmpty()) return;
        dashboardStatusLabel.setText("Computing matches and affinity scores...");

        // Fetch structured cards
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

        // Also fetch raw report for text view
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
        card.setMaximumSize(new Dimension(Short.MAX_VALUE, 160));

        // Top Row: Avatar Initials + Name + Contact + Affinity Score Bar
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

        // Affinity Meter
        JPanel affinityBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        affinityBox.setOpaque(false);

        Color affinityColor = score >= 80 ? Theme.SUCCESS : (score >= 50 ? Theme.CYAN : Theme.AMBER);
        String typeBadgeText = matchType.equals("RECIPROCAL") ? "100% Reciprocal Swap" :
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

        // Center: Compatibility Breakdowns
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

        // Bottom Action Row
        JPanel bottomRow = new JPanel(new BorderLayout());
        bottomRow.setOpaque(false);

        JLabel allSkillsLbl = new JLabel("All offered: [" + peerOffered + "] | All wanted: [" + peerWanted + "]");
        allSkillsLbl.setFont(Theme.SMALL_FONT);
        allSkillsLbl.setForeground(Theme.TEXT_MUTED);

        ModernButton chatBtn = new ModernButton("💬 Message " + name, Theme.PRIMARY, Theme.PRIMARY_HOVER);
        chatBtn.addActionListener(e -> {
            msgToField.setText(name);
            msgTextField.requestFocus();
            // Switch to chat tab
            JTabbedPane tabs = (JTabbedPane) card.getParent().getParent().getParent().getParent();
            tabs.setSelectedIndex(2);
        });

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
        msgBtn.addActionListener(e -> {
            msgToField.setText(name);
            JTabbedPane tabs = (JTabbedPane) card.getParent().getParent().getParent().getParent();
            tabs.setSelectedIndex(2);
        });

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
            handleCheckInbox();
        });
    }

    private void handleCheckInbox() {
        if (currentUserName.isEmpty()) return;
        executeNetwork("GET_MSGS " + currentUserName, resp -> {
            inboxTextArea.setText(resp);
            inboxTextArea.setCaretPosition(0);
        });
    }

    // =========================================================================
    // 4. NETWORKING BRIDGE
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

    private static int parseInt(String val, int def) {
        try { return Integer.parseInt(val.trim()); } catch (Exception e) { return def; }
    }

    // =========================================================================
    // 5. CUSTOM MODERN SWING COMPONENTS
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
        return tf;
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
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 60)); // soft tint
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

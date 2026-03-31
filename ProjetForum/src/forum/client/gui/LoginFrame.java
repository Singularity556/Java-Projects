package forum.client.gui;

import forum.client.ForumClient;
import forum.common.Message;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;

/**
 * Fenêtre de connexion : saisie du pseudo, IP serveur et port.
 * Compatible Java 8+.
 */
public class LoginFrame extends JFrame {

    // ─── Palette ──────────────────────────────────────────────────────────────
    static final Color BG_DARK     = new Color(13, 17, 30);
    static final Color BG_PANEL    = new Color(22, 27, 47);
    static final Color ACCENT      = new Color(99, 102, 241);
    static final Color ACCENT2     = new Color(168, 85, 247);
    static final Color TEXT_MAIN   = new Color(240, 240, 255);
    static final Color TEXT_SUB    = new Color(130, 140, 170);
    static final Color INPUT_BG    = new Color(30, 36, 60);
    static final Color INPUT_BORDER = new Color(60, 70, 110);
    static final Color ERROR_COLOR  = new Color(239, 68, 68);
    static final Color SUCCESS_COLOR = new Color(34, 197, 94);

    static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 26);
    static final Font FONT_LABEL = new Font("Segoe UI", Font.PLAIN, 13);
    static final Font FONT_INPUT = new Font("Segoe UI", Font.PLAIN, 14);
    static final Font FONT_BTN   = new Font("Segoe UI", Font.BOLD, 14);
    static final Font FONT_SUB   = new Font("Segoe UI", Font.PLAIN, 11);

    private JTextField pseudoField;
    private JTextField ipField;
    private JTextField portField;
    private JLabel statusLabel;
    private JButton connectBtn;

    // Référence client partagée (null au départ)
    private ForumClient activeClient;

    public LoginFrame() {
        setTitle("Forum — Connexion");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        setBackground(BG_DARK);
        buildUI();
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void buildUI() {
        JPanel root = new JPanel(new GridBagLayout());
        root.setBackground(BG_DARK);
        root.setBorder(new EmptyBorder(40, 50, 40, 50));
        setContentPane(root);

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets   = new Insets(6, 0, 6, 0);
        gbc.fill     = GridBagConstraints.HORIZONTAL;
        gbc.gridx    = 0;
        gbc.weightx  = 1;

        // ── Titre ──
        JPanel titlePanel = new JPanel();
        titlePanel.setBackground(BG_DARK);
        JLabel icon  = new JLabel("💬");
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 40));
        JLabel title = new JLabel("  Forum");
        title.setFont(FONT_TITLE);
        title.setForeground(TEXT_MAIN);
        titlePanel.add(icon);
        titlePanel.add(title);
        gbc.gridy  = 0; gbc.insets = new Insets(0, 0, 6, 0);
        root.add(titlePanel, gbc);

        JLabel subtitle = new JLabel("Rejoignez la conversation", SwingConstants.CENTER);
        subtitle.setFont(FONT_SUB);
        subtitle.setForeground(TEXT_SUB);
        gbc.gridy  = 1; gbc.insets = new Insets(0, 0, 24, 0);
        root.add(subtitle, gbc);

        // ── Card ──
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(BG_PANEL);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(INPUT_BORDER, 1, true),
                new EmptyBorder(28, 32, 28, 32)));
        card.setPreferredSize(new Dimension(360, 280));

        pseudoField = styledField("Votre pseudo");
        ipField     = styledField("127.0.0.1");
        portField   = styledField("5000");

        card.add(makeLabel("Pseudo"));
        card.add(Box.createVerticalStrut(6));
        card.add(pseudoField);
        card.add(Box.createVerticalStrut(14));
        card.add(makeLabel("Adresse IP du serveur"));
        card.add(Box.createVerticalStrut(6));
        card.add(ipField);
        card.add(Box.createVerticalStrut(14));
        card.add(makeLabel("Port"));
        card.add(Box.createVerticalStrut(6));
        card.add(portField);

        gbc.gridy  = 2; gbc.insets = new Insets(0, 0, 14, 0);
        root.add(card, gbc);

        // ── Statut ──
        statusLabel = new JLabel(" ", SwingConstants.CENTER);
        statusLabel.setFont(FONT_SUB);
        statusLabel.setForeground(TEXT_SUB);
        gbc.gridy  = 3;
        root.add(statusLabel, gbc);

        // ── Bouton ──
        connectBtn = new JButton("Se connecter");
        connectBtn.setFont(FONT_BTN);
        connectBtn.setForeground(Color.WHITE);
        connectBtn.setBackground(ACCENT);
        connectBtn.setOpaque(true);
        connectBtn.setBorderPainted(false);
        connectBtn.setFocusPainted(false);
        connectBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        connectBtn.setBorder(new EmptyBorder(12, 0, 12, 0));
        connectBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        connectBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { connectBtn.setBackground(ACCENT2); }
            public void mouseExited(MouseEvent e)  { connectBtn.setBackground(ACCENT); }
        });
        connectBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { attemptConnect(); }
        });

        KeyAdapter enterKey = new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) attemptConnect();
            }
        };
        pseudoField.addKeyListener(enterKey);
        ipField.addKeyListener(enterKey);
        portField.addKeyListener(enterKey);

        gbc.gridy  = 4; gbc.insets = new Insets(4, 0, 0, 0);
        root.add(connectBtn, gbc);
    }

    private void attemptConnect() {
        String pseudo  = pseudoField.getText().trim();
        String ip      = ipField.getText().trim();
        String portTxt = portField.getText().trim();

        if (pseudo.isEmpty()){ showError("Veuillez entrer un pseudo."); return; }
        if (ip.isEmpty())    { showError("Veuillez entrer l'adresse IP."); return; }

        int port;
        try { port = Integer.parseInt(portTxt); }
        catch (NumberFormatException ex) { showError("Port invalide."); return; }

        connectBtn.setEnabled(false);
        showStatus("Connexion en cours...", TEXT_SUB);

        final int finalPort = port;
        final String finalPseudo = pseudo;
        final String finalIp = ip;

        new Thread(new Runnable() {
            public void run() {
                final ForumClient client = new ForumClient();
                try {
                    client.connect(finalIp, finalPort, new java.util.function.Consumer<Message>() {
                        public void accept(Message msg) {
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    handleServerMsg(msg, finalPseudo, client);
                                }
                            });
                        }
                    });

                    Message loginMsg = new Message(Message.Type.LOGIN);
                    loginMsg.setSender(finalPseudo);
                    client.send(loginMsg);

                } catch (IOException ex) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            showError("Impossible de se connecter : " + ex.getMessage());
                            connectBtn.setEnabled(true);
                        }
                    });
                }
            }
        }).start();
    }

    private void handleServerMsg(Message msg, String pseudo, ForumClient client) {
        switch (msg.getType()) {
            case LOGIN_OK:
                showStatus("Connecté !", SUCCESS_COLOR);
                new LobbyFrame(pseudo, client);
                dispose();
                break;
            case ERROR:
            case LOGIN_ERROR:
                showError(msg.getContent());
                connectBtn.setEnabled(true);
                client.disconnect();
                break;
            default:
                break;
        }
    }

    private void showError(String msg) {
        statusLabel.setText(msg);
        statusLabel.setForeground(ERROR_COLOR);
    }

    private void showStatus(String msg, Color color) {
        statusLabel.setText(msg);
        statusLabel.setForeground(color);
    }

    private JTextField styledField(final String placeholder) {
        JTextField field = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty()) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(TEXT_SUB);
                    g2.setFont(getFont().deriveFont(Font.ITALIC));
                    Insets ins = getInsets();
                    g2.drawString(placeholder, ins.left + 2,
                            getHeight() / 2 + g2.getFontMetrics().getAscent() / 2 - 2);
                    g2.dispose();
                }
            }
        };
        field.setFont(FONT_INPUT);
        field.setForeground(TEXT_MAIN);
        field.setBackground(INPUT_BG);
        field.setCaretColor(TEXT_MAIN);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(INPUT_BORDER, 1, true),
                new EmptyBorder(8, 12, 8, 12)));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        return field;
    }

    private JLabel makeLabel(String text) {
        JLabel lbl = new JLabel(text);
        lbl.setFont(FONT_LABEL);
        lbl.setForeground(TEXT_SUB);
        lbl.setAlignmentX(LEFT_ALIGNMENT);
        return lbl;
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() { new LoginFrame(); }
        });
    }
}

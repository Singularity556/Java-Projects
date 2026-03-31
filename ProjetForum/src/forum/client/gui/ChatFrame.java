package forum.client.gui;

import forum.client.ForumClient;
import forum.common.Message;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static forum.client.gui.LoginFrame.*;

/**
 * Fenêtre de chat d'un salon. Affiche les messages, la liste des membres,
 * et permet d'envoyer des messages.
 * Compatible Java 8+.
 */
public class ChatFrame extends JFrame {

    private final String username;
    private final String roomName;
    private final ForumClient client;
    private final LobbyFrame lobby;

    private JTextPane chatArea;
    private JTextField inputField;
    private DefaultListModel<String> memberModel;

    // Couleurs par utilisateur
    private final Map<String, Color> userColors = new HashMap<String, Color>();
    private final Color[] PALETTE = {
        new Color(99, 102, 241),
        new Color(168, 85, 247),
        new Color(16, 185, 129),
        new Color(245, 158, 11),
        new Color(239, 68, 68),
        new Color(59, 130, 246),
        new Color(236, 72, 153),
        new Color(20, 184, 166),
    };
    private int colorIndex = 0;

    public ChatFrame(String username, String roomName, String topic,
                     ForumClient client, LobbyFrame lobby) {
        this.username = username;
        this.roomName = roomName;
        this.client   = client;
        this.lobby    = lobby;

        setTitle("# " + roomName);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { leaveRoom(); }
        });

        buildUI(topic);
        setSize(820, 600);
        setLocationRelativeTo(null);
        setVisible(true);

        appendSystemMessage("Vous avez rejoint le salon # " + roomName
                + (topic != null && !topic.trim().isEmpty() ? " — " + topic : ""));
    }

    // ─── UI ───────────────────────────────────────────────────────────────────

    private void buildUI(String topic) {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_DARK);
        setContentPane(root);

        // ── Header ──
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG_PANEL);
        header.setBorder(new EmptyBorder(12, 18, 12, 18));

        JPanel titleArea = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titleArea.setBackground(BG_PANEL);

        JLabel hashLbl = new JLabel("# ");
        hashLbl.setFont(new Font("Segoe UI", Font.BOLD, 20));
        hashLbl.setForeground(ACCENT);

        JLabel roomLbl = new JLabel(roomName);
        roomLbl.setFont(new Font("Segoe UI", Font.BOLD, 18));
        roomLbl.setForeground(TEXT_MAIN);

        String topicDisplay = (topic != null && !topic.trim().isEmpty()) ? "   " + topic : "";
        JLabel topicLbl = new JLabel(topicDisplay);
        topicLbl.setFont(FONT_LABEL);
        topicLbl.setForeground(TEXT_SUB);

        titleArea.add(hashLbl);
        titleArea.add(roomLbl);
        titleArea.add(topicLbl);

        JButton leaveBtn = new JButton("Quitter le salon");
        leaveBtn.setFont(FONT_SUB);
        leaveBtn.setForeground(ERROR_COLOR);
        leaveBtn.setBackground(BG_PANEL);
        leaveBtn.setFocusPainted(false);
        leaveBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        leaveBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ERROR_COLOR, 1, true),
                new EmptyBorder(4, 12, 4, 12)));
        leaveBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { leaveRoom(); }
        });

        header.add(titleArea, BorderLayout.WEST);
        header.add(leaveBtn, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);

        // ── Zone centrale : messages + membres ──
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setDividerLocation(580);
        split.setDividerSize(4);
        split.setBorder(null);
        split.setBackground(INPUT_BORDER);

        // Zone messages
        chatArea = new JTextPane();
        chatArea.setEditable(false);
        chatArea.setBackground(BG_DARK);
        chatArea.setForeground(TEXT_MAIN);
        chatArea.setFont(FONT_LABEL);
        chatArea.setMargin(new Insets(12, 16, 12, 16));

        JScrollPane chatScroll = new JScrollPane(chatArea);
        chatScroll.setBorder(BorderFactory.createEmptyBorder());
        chatScroll.getViewport().setBackground(BG_DARK);
        split.setLeftComponent(chatScroll);

        // Zone membres
        memberModel = new DefaultListModel<String>();
        JList<String> memberList = new JList<String>(memberModel);
        memberList.setBackground(BG_PANEL);
        memberList.setForeground(TEXT_MAIN);
        memberList.setFont(FONT_LABEL);
        memberList.setFixedCellHeight(36);
        memberList.setCellRenderer(new MemberCellRenderer(username));

        JPanel memberPanel = new JPanel(new BorderLayout());
        memberPanel.setBackground(BG_PANEL);

        JLabel memberTitle = new JLabel("Membres");
        memberTitle.setFont(new Font("Segoe UI", Font.BOLD, 12));
        memberTitle.setForeground(TEXT_SUB);
        memberTitle.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, INPUT_BORDER),
                new EmptyBorder(10, 14, 10, 14)));

        JScrollPane memberScroll = new JScrollPane(memberList);
        memberScroll.setBorder(BorderFactory.createEmptyBorder());
        memberScroll.getViewport().setBackground(BG_PANEL);

        memberPanel.add(memberTitle, BorderLayout.NORTH);
        memberPanel.add(memberScroll, BorderLayout.CENTER);
        split.setRightComponent(memberPanel);

        root.add(split, BorderLayout.CENTER);

        // ── Zone de saisie ──
        JPanel inputArea = new JPanel(new BorderLayout(10, 0));
        inputArea.setBackground(BG_PANEL);
        inputArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, INPUT_BORDER),
                new EmptyBorder(12, 16, 12, 16)));

        inputField = new JTextField();
        inputField.setFont(FONT_INPUT);
        inputField.setForeground(TEXT_MAIN);
        inputField.setBackground(INPUT_BG);
        inputField.setCaretColor(TEXT_MAIN);
        inputField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(INPUT_BORDER, 1, true),
                new EmptyBorder(8, 14, 8, 14)));
        inputField.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) sendMessage();
            }
        });

        //final JButton sendBtn = new JButton("Envoyer \u27A4");
        final JButton sendBtn = new JButton("Envoyer  \u25B6"); // ▶ solid triangle, universally supported
        
        sendBtn.setFont(FONT_BTN);
        sendBtn.setForeground(Color.WHITE);
        sendBtn.setBackground(ACCENT);
        sendBtn.setOpaque(true);
        sendBtn.setBorderPainted(false);
        sendBtn.setFocusPainted(false);
        sendBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        sendBtn.setBorder(new EmptyBorder(8, 18, 8, 18));
        sendBtn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { sendBtn.setBackground(ACCENT2); }
            public void mouseExited(MouseEvent e)  { sendBtn.setBackground(ACCENT); }
        });
        sendBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { sendMessage(); }
        });

        inputArea.add(inputField, BorderLayout.CENTER);
        inputArea.add(sendBtn,    BorderLayout.EAST);
        root.add(inputArea, BorderLayout.SOUTH);

        inputField.requestFocusInWindow();
    }

    // ─── Envoi ────────────────────────────────────────────────────────────────

    private void sendMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) return;

        Message msg = new Message(Message.Type.CHAT);
        msg.setSender(username);
        msg.setRoomName(roomName);
        msg.setContent(text);
        client.send(msg);

        inputField.setText("");
    }

    private void leaveRoom() {
        Message leave = new Message(Message.Type.LEAVE_ROOM);
        leave.setSender(username);
        leave.setRoomName(roomName);
        client.send(leave);
        lobby.chatClosed(roomName);
        dispose();
    }

    // ─── Réception ────────────────────────────────────────────────────────────

    public void handleMessage(final Message msg) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                switch (msg.getType()) {
                    case CHAT_BROADCAST:
                        appendChatMessage(msg.getSender(), msg.getContent(), msg.getTimestamp());
                        break;
                    case USER_JOINED:
                        appendSystemMessage("\u2736 " + msg.getContent());
                        break;
                    case USER_LEFT:
                        appendSystemMessage("\u2737 " + msg.getContent());
                        break;
                    case MEMBER_LIST:
                        updateMembers(msg.getMembers());
                        break;
                    default:
                        break;
                }
            }
        });
    }

    // ─── Affichage ────────────────────────────────────────────────────────────

    private void appendChatMessage(String sender, String content, String time) {
        Color senderColor = getUserColor(sender);
        boolean isMe = sender.equals(username);

        javax.swing.text.StyledDocument doc = chatArea.getStyledDocument();
        try {
            // Timestamp
            javax.swing.text.Style tsStyle = chatArea.addStyle("ts_" + System.nanoTime(), null);
            javax.swing.text.StyleConstants.setForeground(tsStyle, TEXT_SUB);
            javax.swing.text.StyleConstants.setFontSize(tsStyle, 10);
            doc.insertString(doc.getLength(), "[" + time + "] ", tsStyle);

            // Pseudo
            javax.swing.text.Style nameStyle = chatArea.addStyle("name_" + System.nanoTime(), null);
            javax.swing.text.StyleConstants.setForeground(nameStyle, senderColor);
            javax.swing.text.StyleConstants.setBold(nameStyle, true);
            javax.swing.text.StyleConstants.setFontSize(nameStyle, 13);
            doc.insertString(doc.getLength(), isMe ? "Vous" : sender, nameStyle);

            // Espace
            javax.swing.text.Style sepStyle = chatArea.addStyle("sep_" + System.nanoTime(), null);
            javax.swing.text.StyleConstants.setForeground(sepStyle, TEXT_SUB);
            doc.insertString(doc.getLength(), "  ", sepStyle);

            // Contenu
            javax.swing.text.Style contentStyle = chatArea.addStyle("cnt_" + System.nanoTime(), null);
            javax.swing.text.StyleConstants.setForeground(contentStyle,
                    isMe ? new Color(220, 220, 255) : TEXT_MAIN);
            javax.swing.text.StyleConstants.setFontSize(contentStyle, 13);
            doc.insertString(doc.getLength(), content + "\n", contentStyle);

        } catch (javax.swing.text.BadLocationException e) {
            e.printStackTrace();
        }
        scrollToBottom();
    }

    private void appendSystemMessage(String text) {
        javax.swing.text.StyledDocument doc = chatArea.getStyledDocument();
        try {
            javax.swing.text.Style style = chatArea.addStyle("sys_" + System.nanoTime(), null);
            javax.swing.text.StyleConstants.setForeground(style, new Color(100, 116, 139));
            javax.swing.text.StyleConstants.setItalic(style, true);
            javax.swing.text.StyleConstants.setFontSize(style, 11);
            doc.insertString(doc.getLength(), "  " + text + "\n", style);
        } catch (javax.swing.text.BadLocationException e) {
            e.printStackTrace();
        }
        scrollToBottom();
    }

    private void scrollToBottom() {
        chatArea.setCaretPosition(chatArea.getDocument().getLength());
    }

    private void updateMembers(List<String> members) {
        memberModel.clear();
        if (members != null) {
            for (String m : members) memberModel.addElement(m);
        }
    }

    private Color getUserColor(String user) {
        if (!userColors.containsKey(user)) {
            userColors.put(user, PALETTE[colorIndex % PALETTE.length]);
            colorIndex++;
        }
        return userColors.get(user);
    }

    // ─── Renderer membres ─────────────────────────────────────────────────────

    static class MemberCellRenderer extends DefaultListCellRenderer {
        private final String me;
        MemberCellRenderer(String me) { this.me = me; }

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            panel.setBackground(isSelected ? new Color(40, 45, 80) : BG_PANEL);
            panel.setBorder(new EmptyBorder(4, 8, 4, 8));

            String user = (String) value;

            JLabel dot = new JLabel("\u25CF");
            dot.setForeground(SUCCESS_COLOR);
            //dot.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            dot.setFont(new Font("Dialog", Font.PLAIN, 10)); // "Dialog" is Java's logical font, always works
            
            String label = user + (user.equals(me) ? "  (vous)" : "");
            JLabel name = new JLabel(label);
            name.setFont(FONT_LABEL);
            name.setForeground(user.equals(me) ? ACCENT : TEXT_MAIN);

            panel.add(dot);
            panel.add(name);
            return panel;
        }
    }
}

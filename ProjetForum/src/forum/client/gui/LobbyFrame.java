package forum.client.gui;

import forum.client.ForumClient;
import forum.common.ChatRoom;
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
 * Fenêtre principale du lobby : liste les salons, permet d'en créer ou rejoindre.
 * Compatible Java 8+.
 */
public class LobbyFrame extends JFrame {

    private final String username;
    private final ForumClient client;

    private DefaultListModel<ChatRoom> roomListModel;
    private JList<ChatRoom> roomList;

    // Salons ouverts : nom → ChatFrame
    private final Map<String, ChatFrame> openChats = new HashMap<String, ChatFrame>();

    public LobbyFrame(String username, ForumClient client) {
        this.username = username;
        this.client   = client;

        setTitle("Forum — Lobby (" + username + ")");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { logout(); }
        });

        buildUI();
        setSize(640, 520);
        setLocationRelativeTo(null);
        setVisible(true);

        // Rediriger les messages vers ce lobby
        client.connect(new java.util.function.Consumer<Message>() {
            public void accept(Message msg) { handleMessage(msg); }
        });

        refreshRooms();
    }

    // ─── UI ───────────────────────────────────────────────────────────────────

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(BG_DARK);
        setContentPane(root);

        // ── Header ──
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(BG_PANEL);
        header.setBorder(new EmptyBorder(14, 20, 14, 20));

        JPanel titleArea = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        titleArea.setBackground(BG_PANEL);
        JLabel logo = new JLabel("💬  ");
        logo.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 20));
        JLabel titleLbl = new JLabel("Forum — Salons de discussion");
        titleLbl.setFont(new Font("Segoe UI", Font.BOLD, 17));
        titleLbl.setForeground(TEXT_MAIN);
        titleArea.add(logo);
        titleArea.add(titleLbl);

        JLabel userBadge = new JLabel("   " + username + "  ");
        userBadge.setFont(FONT_LABEL);
        userBadge.setForeground(TEXT_MAIN);
        userBadge.setBackground(ACCENT);
        userBadge.setOpaque(true);
        userBadge.setBorder(new EmptyBorder(4, 10, 4, 10));

        header.add(titleArea, BorderLayout.WEST);
        header.add(userBadge, BorderLayout.EAST);
        root.add(header, BorderLayout.NORTH);

        // ── Centre : liste des salons ──
        roomListModel = new DefaultListModel<ChatRoom>();
        roomList = new JList<ChatRoom>(roomListModel);
        roomList.setCellRenderer(new RoomCellRenderer());
        roomList.setBackground(BG_DARK);
        roomList.setSelectionBackground(new Color(40, 45, 80));
        roomList.setFixedCellHeight(70);
        roomList.setFont(FONT_LABEL);
        roomList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) joinSelectedRoom();
            }
        });

        JScrollPane scroll = new JScrollPane(roomList);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setBackground(BG_DARK);
        scroll.getViewport().setBackground(BG_DARK);

        JPanel centerArea = new JPanel(new BorderLayout());
        centerArea.setBackground(BG_DARK);
        centerArea.setBorder(new EmptyBorder(12, 16, 0, 16));

        JPanel listHeader = new JPanel(new BorderLayout());
        listHeader.setBackground(BG_DARK);
        listHeader.setBorder(new EmptyBorder(0, 0, 8, 0));
        JLabel listTitle = new JLabel("Salons disponibles");
        listTitle.setFont(new Font("Segoe UI", Font.BOLD, 13));
        listTitle.setForeground(TEXT_SUB);
        //JButton refreshBtn = new JButton("↻ Actualiser");
        JButton refreshBtn = new JButton("Actualiser"); // or simply "⟳ Actualiser"
        styleSmallBtn(refreshBtn, INPUT_BORDER);
        refreshBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { refreshRooms(); }
        });
        listHeader.add(listTitle, BorderLayout.WEST);
        listHeader.add(refreshBtn, BorderLayout.EAST);

        centerArea.add(listHeader, BorderLayout.NORTH);
        centerArea.add(scroll, BorderLayout.CENTER);
        root.add(centerArea, BorderLayout.CENTER);

        // ── Barre d'action ──
        JPanel actionBar = new JPanel(new BorderLayout());
        actionBar.setBackground(BG_PANEL);
        actionBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, INPUT_BORDER),
                new EmptyBorder(12, 20, 12, 20)));

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        btnPanel.setBackground(BG_PANEL);

        JButton createBtn = new JButton("+ Créer un salon");
        styleBtn(createBtn, ACCENT);
        createBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { showCreateRoomDialog(); }
        });

        JButton joinBtn = new JButton("\u2192 Rejoindre");
        styleBtn(joinBtn, new Color(16, 185, 129));
        joinBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { joinSelectedRoom(); }
        });

        JButton logoutBtn = new JButton("Déconnexion");
        styleSmallBtn(logoutBtn, ERROR_COLOR);
        logoutBtn.setForeground(ERROR_COLOR);
        logoutBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) { logout(); }
        });

        btnPanel.add(createBtn);
        btnPanel.add(joinBtn);
        actionBar.add(btnPanel, BorderLayout.WEST);
        actionBar.add(logoutBtn, BorderLayout.EAST);
        root.add(actionBar, BorderLayout.SOUTH);
    }

    // ─── Actions ──────────────────────────────────────────────────────────────

    private void refreshRooms() {
        client.send(new Message(Message.Type.LIST_ROOMS));
    }

    private void joinSelectedRoom() {
        ChatRoom selected = roomList.getSelectedValue();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "Sélectionnez un salon à rejoindre.",
                    "Aucune sélection", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (openChats.containsKey(selected.getName())) {
            openChats.get(selected.getName()).toFront();
            return;
        }
        Message join = new Message(Message.Type.JOIN_ROOM);
        join.setSender(username);
        join.setRoomName(selected.getName());
        client.send(join);
    }

    private void showCreateRoomDialog() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG_PANEL);
        panel.setBorder(new EmptyBorder(12, 12, 12, 12));

        JTextField nameField  = new JTextField(20);
        nameField.setFont(FONT_INPUT);
        JTextField topicField = new JTextField(20);
        topicField.setFont(FONT_INPUT);

        panel.add(new JLabel("Nom du salon :"));
        panel.add(Box.createVerticalStrut(4));
        panel.add(nameField);
        panel.add(Box.createVerticalStrut(10));
        panel.add(new JLabel("Description / thème :"));
        panel.add(Box.createVerticalStrut(4));
        panel.add(topicField);

        int result = JOptionPane.showConfirmDialog(this, panel, "Créer un salon",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (result == JOptionPane.OK_OPTION) {
            String name  = nameField.getText().trim();
            String topic = topicField.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Le nom du salon ne peut pas être vide.",
                        "Erreur", JOptionPane.ERROR_MESSAGE);
                return;
            }
            Message create = new Message(Message.Type.CREATE_ROOM);
            create.setSender(username);
            create.setRoomName(name);
            create.setContent(topic);
            client.send(create);
        }
    }

    private void logout() {
        client.send(new Message(Message.Type.LOGOUT));
        client.disconnect();
        new LoginFrame();
        dispose();
    }

    // ─── Gestion des messages serveur ─────────────────────────────────────────

    public void handleMessage(final Message msg) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                switch (msg.getType()) {

                    case ROOM_LIST: {
                        List<ChatRoom> rooms = msg.getRooms();
                        roomListModel.clear();
                        if (rooms != null) {
                            for (ChatRoom r : rooms) roomListModel.addElement(r);
                        }
                        break;
                    }

                    case ROOM_CREATED: {
                        refreshRooms();
                        break;
                    }

                    case ROOM_JOINED: {
                        String roomName = msg.getRoomName();
                        String creator = msg.getSender();
                        if (!openChats.containsKey(roomName)) {
                            ChatFrame chat = new ChatFrame(username, roomName, msg.getContent(), creator, client, LobbyFrame.this);
                            openChats.put(roomName, chat);
                        } else {
                            openChats.get(roomName).toFront();
                        }
                        refreshRooms();
                        break;
                    }

                    case ROOM_LEFT: {
                        openChats.remove(msg.getRoomName());
                        refreshRooms();
                        break;
                    }

                    case CHAT_BROADCAST:
                    case USER_JOINED:
                    case USER_LEFT:
                    case MEMBER_LIST:
                    case KICKED:
                    case BANNED: {
                        ChatFrame chat = openChats.get(msg.getRoomName());
                        if (chat != null) chat.handleMessage(msg);
                        break;
                    }

                    case ERROR: {
                        JOptionPane.showMessageDialog(LobbyFrame.this,
                                msg.getContent(), "Erreur", JOptionPane.ERROR_MESSAGE);
                        break;
                    }

                    default:
                        break;
                }
            }
        });
    }

    public void chatClosed(String roomName) {
        openChats.remove(roomName);
    }

    // ─── Helpers UI ───────────────────────────────────────────────────────────

    private void styleBtn(JButton btn, Color bg) {
        btn.setFont(FONT_BTN);
        btn.setForeground(Color.WHITE);
        btn.setBackground(bg);
        btn.setOpaque(true);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 18, 8, 18));
    }

    private void styleSmallBtn(JButton btn, Color border) {
        btn.setFont(FONT_SUB);
        btn.setForeground(TEXT_SUB);
        btn.setBackground(BG_PANEL);
        btn.setOpaque(true);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border, 1, true),
                new EmptyBorder(4, 10, 4, 10)));
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    // ─── Renderer des salons ──────────────────────────────────────────────────

    static class RoomCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {

            JPanel panel = new JPanel(new BorderLayout(12, 0));
            panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, INPUT_BORDER),
                    new EmptyBorder(10, 16, 10, 16)));
            panel.setBackground(isSelected ? new Color(40, 45, 80) : BG_DARK);

            ChatRoom room = (ChatRoom) value;

            JLabel icon = new JLabel("#");
            icon.setFont(new Font("Segoe UI", Font.BOLD, 22));
            icon.setForeground(ACCENT);
            icon.setPreferredSize(new Dimension(28, 28));

            JPanel info = new JPanel(new GridLayout(2, 1, 0, 2));
            info.setOpaque(false);

            JLabel name = new JLabel(room.getName());
            name.setFont(new Font("Segoe UI", Font.BOLD, 14));
            name.setForeground(TEXT_MAIN);

            String topicText = (room.getTopic() == null || room.getTopic().isEmpty())
                    ? "Aucune description" : room.getTopic();
            JLabel topic = new JLabel(topicText);
            topic.setFont(FONT_SUB);
            topic.setForeground(TEXT_SUB);

            info.add(name);
            info.add(topic);

            JLabel members = new JLabel(room.getMemberCount() + " membre(s)");
            members.setFont(FONT_SUB);
            members.setForeground(SUCCESS_COLOR);

            panel.add(icon,    BorderLayout.WEST);
            panel.add(info,    BorderLayout.CENTER);
            panel.add(members, BorderLayout.EAST);

            return panel;
        }
    }
}

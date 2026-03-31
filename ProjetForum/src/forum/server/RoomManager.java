package forum.server;

import forum.common.ChatRoom;
import forum.common.Message;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestionnaire central des salons et des clients connectés.
 */
public class RoomManager {

    // pseudo → ClientHandler
    private final Map<String, ClientHandler> connectedClients = new ConcurrentHashMap<>();
    // nom salon → ChatRoom
    private final Map<String, ChatRoom> rooms = new ConcurrentHashMap<>();

    // ─── Gestion des clients ──────────────────────────────────────────────────

    public synchronized boolean registerClient(String username, ClientHandler handler) {
        if (connectedClients.containsKey(username)) return false;
        connectedClients.put(username, handler);
        System.out.println("[Serveur] Nouveau client : " + username);
        return true;
    }

    public void removeClient(String username) {
        connectedClients.remove(username);
        // Retirer le client de tous les salons
        for (ChatRoom room : rooms.values()) {
            if (room.hasMember(username)) {
                room.removeMember(username);
                broadcastToRoom(room.getName(), buildUserLeftMsg(username, room.getName()));
                broadcastMemberList(room.getName());
            }
        }
        System.out.println("[Serveur] Client déconnecté : " + username);
    }

    // ─── Gestion des salons ───────────────────────────────────────────────────

    public synchronized boolean createRoom(String name, String topic, String creator) {
        if (rooms.containsKey(name)) return false;
        ChatRoom room = new ChatRoom(name, topic, creator);
        rooms.put(name, room);
        System.out.println("[Serveur] Salon créé : " + name + " par " + creator);
        return true;
    }

    public boolean joinRoom(String roomName, String username) {
        ChatRoom room = rooms.get(roomName);
        if (room == null) return false;
        room.addMember(username);
        System.out.println("[Serveur] " + username + " rejoint le salon : " + roomName);
        return true;
    }

    public boolean leaveRoom(String roomName, String username) {
        ChatRoom room = rooms.get(roomName);
        if (room == null) return false;
        room.removeMember(username);
        // Supprimer le salon s'il est vide
        if (room.getMemberCount() == 0) {
            rooms.remove(roomName);
            System.out.println("[Serveur] Salon supprimé (vide) : " + roomName);
        }
        return true;
    }

    public ChatRoom getRoom(String name) {
        return rooms.get(name);
    }

    public List<ChatRoom> listRooms() {
        return new ArrayList<>(rooms.values());
    }

    // ─── Broadcast ────────────────────────────────────────────────────────────

    public void broadcastToRoom(String roomName, Message message) {
        ChatRoom room = rooms.get(roomName);
        if (room == null) return;
        room.incrementMessageCount();
        for (String member : room.getMembers()) {
            ClientHandler handler = connectedClients.get(member);
            if (handler != null) {
                handler.sendMessage(message);
            }
        }
    }

    public void broadcastMemberList(String roomName) {
        ChatRoom room = rooms.get(roomName);
        if (room == null) return;
        Message msg = new Message(Message.Type.MEMBER_LIST);
        msg.setRoomName(roomName);
        msg.setMembers(room.getMembers());
        for (String member : room.getMembers()) {
            ClientHandler handler = connectedClients.get(member);
            if (handler != null) handler.sendMessage(msg);
        }
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private Message buildUserLeftMsg(String username, String roomName) {
        Message msg = new Message(Message.Type.USER_LEFT);
        msg.setSender(username);
        msg.setRoomName(roomName);
        msg.setContent(username + " a quitté le salon.");
        return msg;
    }
}

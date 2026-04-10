package forum.common;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Objet sérialisable représentant tout échange entre client et serveur.
 */
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type {
        // Client → Serveur
        LOGIN,
        CREATE_ROOM,
        JOIN_ROOM,
        LEAVE_ROOM,
        CHAT,
        LIST_ROOMS,
        LOGOUT,

        // Serveur → Client
        LOGIN_OK,
        LOGIN_ERROR,
        ROOM_CREATED,
        ROOM_JOINED,
        ROOM_LEFT,
        ROOM_LIST,
        CHAT_BROADCAST,
        USER_JOINED,
        USER_LEFT,
        MEMBER_LIST,
        ERROR,
        SERVER_INFO,

        // Admin (Client → Serveur)
        KICK_USER,
        BAN_USER,

        // Admin (Serveur → Client)
        KICKED,
        BANNED
    }

    private Type type;
    private String sender;
    private String roomName;
    private String content;
    private String timestamp;
    private List<ChatRoom> rooms;
    private List<String> members;

    public Message(Type type) {
        this.type = type;
        this.timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
    }

    public Message(Type type, String sender, String roomName, String content) {
        this(type);
        this.sender = sender;
        this.roomName = roomName;
        this.content = content;
    }

    // --- Getters & Setters ---

    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getRoomName() { return roomName; }
    public void setRoomName(String roomName) { this.roomName = roomName; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getTimestamp() { return timestamp; }

    public List<ChatRoom> getRooms() { return rooms; }
    public void setRooms(List<ChatRoom> rooms) { this.rooms = rooms; }

    public List<String> getMembers() { return members; }
    public void setMembers(List<String> members) { this.members = members; }

    @Override
    public String toString() {
        return "Message{type=" + type + ", sender=" + sender + ", room=" + roomName + ", content=" + content + "}";
    }
}

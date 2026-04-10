package forum.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Représentation d'un salon de discussion (thread-safe).
 */
public class ChatRoom implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private String topic;
    private String creator;
    private List<String> members;
    private int messageCount;
    private transient Map<String, String> bannedUsers;

    public ChatRoom(String name, String topic, String creator) {
        this.name = name;
        this.topic = topic;
        this.creator = creator;
        this.members = Collections.synchronizedList(new ArrayList<>());
        this.messageCount = 0;
        this.bannedUsers = new HashMap<String, String>();
    }

    public synchronized void addMember(String username) {
        if (!members.contains(username)) {
            members.add(username);
        }
    }

    public synchronized void removeMember(String username) {
        members.remove(username);
    }

    public synchronized void banUser(String username, String reason) {
        if (bannedUsers == null) bannedUsers = new HashMap<String, String>();
        bannedUsers.put(username, reason);
        removeMember(username);
    }

    public synchronized boolean isBanned(String username) {
        return bannedUsers != null && bannedUsers.containsKey(username);
    }

    public synchronized String getBanReason(String username) {
        return bannedUsers != null ? bannedUsers.get(username) : null;
    }

    public synchronized boolean hasMember(String username) {
        return members.contains(username);
    }

    public synchronized List<String> getMembers() {
        return new ArrayList<>(members);
    }

    public synchronized int getMemberCount() {
        return members.size();
    }

    public synchronized void incrementMessageCount() {
        messageCount++;
    }

    // --- Getters ---

    public String getName() { return name; }
    public String getTopic() { return topic; }
    public String getCreator() { return creator; }
    public int getMessageCount() { return messageCount; }

    @Override
    public String toString() {
        return name + " (" + getMemberCount() + " membres) — " + topic;
    }
}

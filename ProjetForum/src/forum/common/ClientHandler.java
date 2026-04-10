package forum.common;

import java.io.*;
import java.net.Socket;
import java.util.List;

import forum.server.RoomManager;

/**
 * Thread dédié à la gestion d'un client connecté.
 * Compatible Java 8+.
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private final RoomManager roomManager;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private String username;

    public ClientHandler(Socket socket, RoomManager roomManager) {
        this.socket = socket;
        this.roomManager = roomManager;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            Message msg;
            while ((msg = (Message) in.readObject()) != null) {
                handleMessage(msg);
            }
        } catch (EOFException e) {
            // Déconnexion normale
        } catch (java.net.SocketException e) {
            // Client déconnecté
        } catch (IOException e) {
            System.err.println("[Handler] IOException avec " + username + " : " + e.getMessage());
        } catch (ClassNotFoundException e) {
            System.err.println("[Handler] ClassNotFound : " + e.getMessage());
        } finally {
            disconnect();
        }
    }

    private void handleMessage(Message msg) {
        System.out.println("[Handler] Reçu : " + msg);

        switch (msg.getType()) {

            case LOGIN: {
                String pseudo = msg.getSender();
                if (pseudo == null || pseudo.trim().isEmpty() || pseudo.length() > 20) {
                    sendError("Pseudo invalide (1-20 caractères).");
                    return;
                }
                if (roomManager.registerClient(pseudo.trim(), this)) {
                    this.username = pseudo.trim();
                    Message ok = new Message(Message.Type.LOGIN_OK);
                    ok.setContent("Bienvenue, " + username + " !");
                    sendMessage(ok);
                    sendRoomList();
                } else {
                    sendError("Pseudo \"" + pseudo + "\" déjà utilisé.");
                }
                break;
            }

            case CREATE_ROOM: {
                String name  = msg.getRoomName();
                String topic = msg.getContent() != null ? msg.getContent() : "";
                if (name == null || name.trim().isEmpty()) {
                    sendError("Nom de salon invalide.");
                    return;
                }
                if (roomManager.createRoom(name.trim(), topic, username)) {
                    Message ok = new Message(Message.Type.ROOM_CREATED);
                    ok.setRoomName(name.trim());
                    ok.setContent(topic);
                    sendMessage(ok);
                    joinRoomInternal(name.trim());
                } else {
                    sendError("Le salon \"" + name + "\" existe déjà.");
                }
                break;
            }

            case JOIN_ROOM: {
                joinRoomInternal(msg.getRoomName());
                break;
            }

            case LEAVE_ROOM: {
                String roomName = msg.getRoomName();
                ChatRoom room = roomManager.getRoom(roomName);
                if (room != null && room.hasMember(username)) {
                    roomManager.leaveRoom(roomName, username);

                    Message left = new Message(Message.Type.USER_LEFT);
                    left.setSender(username);
                    left.setRoomName(roomName);
                    left.setContent(username + " a quitté le salon.");
                    roomManager.broadcastToRoom(roomName, left);
                    roomManager.broadcastMemberList(roomName);

                    Message ok = new Message(Message.Type.ROOM_LEFT);
                    ok.setRoomName(roomName);
                    sendMessage(ok);
                }
                break;
            }

            case CHAT: {
                String roomName = msg.getRoomName();
                ChatRoom room = roomManager.getRoom(roomName);
                if (room == null || !room.hasMember(username)) {
                    sendError("Vous n'êtes pas dans ce salon.");
                    return;
                }
                Message broadcast = new Message(Message.Type.CHAT_BROADCAST);
                broadcast.setSender(username);
                broadcast.setRoomName(roomName);
                broadcast.setContent(msg.getContent());
                roomManager.broadcastToRoom(roomName, broadcast);
                break;
            }

            case LIST_ROOMS: {
                sendRoomList();
                break;
            }

            case LOGOUT: {
                disconnect();
                break;
            }

            case KICK_USER: {
                String roomName = msg.getRoomName();
                String payload = msg.getContent();
                int sep = payload.indexOf('|');
                if (sep < 0) {
                    sendError("Format invalide.");
                    return;
                }
                String target = payload.substring(0, sep);
                String reason = payload.substring(sep + 1);

                ChatRoom room = roomManager.getRoom(roomName);
                if (room == null) {
                    sendError("Le salon n'existe pas.");
                    return;
                }
                if (!room.getCreator().equals(username)) {
                    sendError("Seul l'administrateur peut expulser des utilisateurs.");
                    return;
                }
                if (!room.hasMember(target)) {
                    sendError("L'utilisateur n'est pas dans le salon.");
                    return;
                }
                roomManager.leaveRoom(roomName, target);

                Message kicked = new Message(Message.Type.KICKED);
                kicked.setRoomName(roomName);
                kicked.setContent("Vous avez \u00e9t\u00e9 expuls\u00e9 du salon \"" + roomName + "\" par " + username + ".\nRaison : " + reason);
                roomManager.sendToUser(target, kicked);

                Message left = new Message(Message.Type.USER_LEFT);
                left.setSender(target);
                left.setRoomName(roomName);
                left.setContent(target + " a \u00e9t\u00e9 expuls\u00e9 du salon.");
                roomManager.broadcastToRoom(roomName, left);
                roomManager.broadcastMemberList(roomName);
                break;
            }

            case BAN_USER: {
                String roomName = msg.getRoomName();
                String payload = msg.getContent();
                int sep = payload.indexOf('|');
                if (sep < 0) {
                    sendError("Format invalide.");
                    return;
                }
                String target = payload.substring(0, sep);
                String reason = payload.substring(sep + 1);

                ChatRoom room = roomManager.getRoom(roomName);
                if (room == null) {
                    sendError("Le salon n'existe pas.");
                    return;
                }
                if (!room.getCreator().equals(username)) {
                    sendError("Seul l'administrateur peut bannir des utilisateurs.");
                    return;
                }
                if (!room.hasMember(target)) {
                    sendError("L'utilisateur n'est pas dans le salon.");
                    return;
                }
                room.banUser(target, reason);

                Message banned = new Message(Message.Type.BANNED);
                banned.setRoomName(roomName);
                banned.setContent("Vous avez été banni du salon \"" + roomName + "\" par " + username + ".\nRaison : " + reason);
                roomManager.sendToUser(target, banned);

                Message left = new Message(Message.Type.USER_LEFT);
                left.setSender(target);
                left.setRoomName(roomName);
                left.setContent(target + " a été banni du salon.");
                roomManager.broadcastToRoom(roomName, left);
                roomManager.broadcastMemberList(roomName);
                break;
            }

            default:
                sendError("Type de message inconnu : " + msg.getType());
                break;
        }
    }

    /*private void joinRoomInternal(String roomName) {
        if (roomName == null || roomName.trim().isEmpty()) {
            sendError("Nom de salon invalide.");
            return;
        }
        ChatRoom room = roomManager.getRoom(roomName);
        if (room == null) {
            sendError("Le salon \"" + roomName + "\" n'existe pas.");
            return;
        }
        if (!room.hasMember(username)) {
            roomManager.joinRoom(roomName, username);

            Message joined = new Message(Message.Type.USER_JOINED);
            joined.setSender(username);
            joined.setRoomName(roomName);
            joined.setContent(username + " a rejoint le salon.");
            roomManager.broadcastToRoom(roomName, joined);

            roomManager.broadcastMemberList(roomName);
        }

        Message ok = new Message(Message.Type.ROOM_JOINED);
        ok.setRoomName(roomName);
        ok.setContent(room.getTopic());
        sendMessage(ok);
    }*/
    private void joinRoomInternal(String roomName) {
        if (roomName == null || roomName.trim().isEmpty()) {
            sendError("Nom de salon invalide.");
            return;
        }
        ChatRoom room = roomManager.getRoom(roomName);
        if (room == null) {
            sendError("Le salon \"" + roomName + "\" n'existe pas.");
            return;
        }
        if (room.isBanned(username)) {
            String banReason = room.getBanReason(username);
            sendError("Vous \u00eates banni du salon \"" + roomName + "\"."
                    + (banReason != null ? "\nRaison : " + banReason : ""));
            return;
        }

        boolean wasAlreadyMember = room.hasMember(username);

        if (!wasAlreadyMember) {
            roomManager.joinRoom(roomName, username);
        }

        // Send ROOM_JOINED FIRST so the client opens the room panel
        // before the member list and USER_JOINED notifications arrive.
        Message ok = new Message(Message.Type.ROOM_JOINED);
        ok.setRoomName(roomName);
        ok.setContent(room.getTopic());
        ok.setSender(room.getCreator());
        sendMessage(ok);

        if (!wasAlreadyMember) {
            Message joined = new Message(Message.Type.USER_JOINED);
            joined.setSender(username);
            joined.setRoomName(roomName);
            joined.setContent(username + " a rejoint le salon.");
            roomManager.broadcastToRoom(roomName, joined);

            roomManager.broadcastMemberList(roomName);
        }
    }

    private void sendRoomList() {
        List<ChatRoom> rooms = roomManager.listRooms();
        Message listMsg = new Message(Message.Type.ROOM_LIST);
        listMsg.setRooms(rooms);
        sendMessage(listMsg);
    }

    private void sendError(String errorText) {
        Message err = new Message(Message.Type.ERROR);
        err.setContent(errorText);
        sendMessage(err);
    }

    public synchronized void sendMessage(Message msg) {
        try {
            if (out != null) {
                out.writeObject(msg);
                out.flush();
                out.reset();
            }
        } catch (IOException e) {
            System.err.println("[Handler] Impossible d'envoyer à " + username + " : " + e.getMessage());
        }
    }

    private void disconnect() {
        if (username != null) {
            roomManager.removeClient(username);
        }
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }
}

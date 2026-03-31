package forum.client;

import forum.common.Message;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * Gère la connexion TCP côté client.
 * Envoie des messages et reçoit les réponses dans un thread séparé.
 */
public class ForumClient {

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Consumer<Message> onMessage;
    private volatile boolean running = false;

    /**
     * Connexion au serveur.
     *
     * @param host      IP ou hostname du serveur
     * @param port      Port (5000 par défaut)
     * @param onMessage Callback appelé à chaque message reçu
     */
    public void connect(String host, int port, Consumer<Message> onMessage) throws IOException {
        this.onMessage = onMessage;
        socket = new Socket(host, port);
        out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        in = new ObjectInputStream(socket.getInputStream());
        running = true;

        // Thread de réception
        Thread receiverThread = new Thread(this::receiveLoop, "ClientReceiver");
        receiverThread.setDaemon(true);
        receiverThread.start();
    }

    private void receiveLoop() {
        try {
            while (running) {
                Message msg = (Message) in.readObject();
                if (msg != null && onMessage != null) {
                    onMessage.accept(msg);
                }
            }
        } catch (EOFException | java.net.SocketException e) {
            // Déconnexion normale
        } catch (IOException | ClassNotFoundException e) {
            if (running) {
                Message errMsg = new Message(Message.Type.ERROR);
                errMsg.setContent("Connexion perdue : " + e.getMessage());
                if (onMessage != null) onMessage.accept(errMsg);
            }
        } finally {
            running = false;
        }
    }

    public synchronized void send(Message message) {
        try {
            if (out != null && running) {
                out.writeObject(message);
                out.flush();
                out.reset();
            }
        } catch (IOException e) {
            System.err.println("[Client] Erreur d'envoi : " + e.getMessage());
        }
    }

    public void disconnect() {
        running = false;
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }

    /**
     * Remplace le handler de messages (utilisé par LobbyFrame après le login).
     */
    public void connect(Consumer<Message> newHandler) {
        this.onMessage = newHandler;
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed() && running;
    }
}

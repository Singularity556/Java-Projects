package forum.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import forum.common.ClientHandler;

/**
 * Point d'entrée du serveur de forum.
 * Écoute les connexions entrantes et crée un ClientHandler par client.
 */
public class ForumServer {

    private static final int PORT = 5000;
    private final RoomManager roomManager = new RoomManager();

    public void start() {
        System.out.println("╔══════════════════════════════════════╗");
        System.out.println("║     Forum Serveur — Port " + PORT + "        ║");
        System.out.println("╚══════════════════════════════════════╝");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("[Serveur] En attente de connexions...\n");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[Serveur] Connexion entrante depuis : "
                        + clientSocket.getInetAddress().getHostAddress());

                ClientHandler handler = new ClientHandler(clientSocket, roomManager);
                Thread thread = new Thread(handler);
                thread.setDaemon(true);
                thread.start();
            }
        } catch (IOException e) {
            System.err.println("[Serveur] Erreur fatale : " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        new ForumServer().start();
    }
}

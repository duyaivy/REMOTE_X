package server;

import java.util.concurrent.ConcurrentHashMap;
import io.github.cdimascio.dotenv.Dotenv;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.DatagramSocket;
import java.util.Map;

/**
 * Main Relay Server
 * - TCP ports: Handshake, Control, Chat
 * - UDP port: Screen data
 */
public class ServerRelay {
    private static final Map<String, Session> activeSessions = new ConcurrentHashMap<>();
    private static DatagramSocket udpSocket;

    public static void main(String[] args) {
        // Load config từ .env
        Dotenv dotenv = Dotenv.configure()
                .directory("./")
                .ignoreIfMissing()
                .load();

        System.out.println("Current directory: " + System.getProperty("user.dir"));
        System.out.println("SCREEN_PORT from env: " + dotenv.get("SCREEN_PORT"));
        System.out.println("CONTROL_PORT from env: " + dotenv.get("CONTROL_PORT"));
        System.out.println("CHAT_PORT from env: " + dotenv.get("CHAT_PORT"));
        int SCREEN_PORT_TCP = Integer.parseInt(dotenv.get("SCREEN_PORT", "5000"));
        int SCREEN_PORT_UDP = Integer.parseInt(dotenv.get("SCREEN_PORT_UDP", "5001"));
        int CONTROL_PORT = Integer.parseInt(dotenv.get("CONTROL_PORT", "6000"));
        int CHAT_PORT = Integer.parseInt(dotenv.get("CHAT_PORT", "7000"));

        System.out.println("═══════════════════════════════════════");
        System.out.println("    SCREEN SHARING RELAY SERVER");
        System.out.println("═══════════════════════════════════════");

        // Tạo UDP socket cho screen data
        try {
            udpSocket = new DatagramSocket(SCREEN_PORT_UDP);
            System.out.println("✓ UDP socket created on port " + SCREEN_PORT_UDP);

            // Start UDP relay handler
            new UDPRelayHandler(udpSocket, activeSessions).start();

        } catch (Exception e) {
            System.err.println("✗ Failed to create UDP socket: " + e.getMessage());
            return;
        }

        // Start TCP ports (handshake, control, chat)
        new Thread(() -> startTCPPort(SCREEN_PORT_TCP, "Screen Handshake")).start();
        new Thread(() -> startTCPPort(CONTROL_PORT, "Control")).start();
        new Thread(() -> startTCPPort(CHAT_PORT, "Chat")).start();

        System.out.println("═══════════════════════════════════════");
        System.out.println("✓ All servers started successfully!");
        System.out.println("═══════════════════════════════════════");
    }

    private static void startTCPPort(int port, String name) {
        System.out.println("✓ Listening for TCP connections on port " + port + " (" + name + ")");
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("[TCP-" + port + "] New connection from " +
                        clientSocket.getInetAddress());

                new ClientHandler(clientSocket, activeSessions).start();
            }
        } catch (IOException e) {
            System.err.println("✗ Error on port " + port + ": " + e.getMessage());
        }
    }
}
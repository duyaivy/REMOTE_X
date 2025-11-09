package server;

import java.util.concurrent.ConcurrentHashMap;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;

public class ServerRelay {
    private static final int SCREEN_PORT = 5000;
    private static final int CONTROL_PORT = 6000;
    // 1. Mở thêm PORT 7000 cho Chat
    private static final int CHAT_PORT = 7000;

    private static final Map<String, Session> activeSessions = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        new Thread(() -> startPort(SCREEN_PORT)).start();
        new Thread(() -> startPort(CONTROL_PORT)).start();
        new Thread(() -> startPort(CHAT_PORT)).start();
    }

    private static void startPort(int port) {
        System.out.println("Listening for connections on port " + port);
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New connection from " + clientSocket.getInetAddress() + " on port " + port);

                new ClientHandler(clientSocket, activeSessions).start();
            }
        } catch (IOException e) {
            System.err.println("Error on port " + port + ": " + e.getMessage());
        }
    }

}
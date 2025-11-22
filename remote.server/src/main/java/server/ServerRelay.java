package server;

import java.util.concurrent.ConcurrentHashMap;

import io.github.cdimascio.dotenv.Dotenv;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;

public class ServerRelay {

    private static final Map<String, Session> activeSessions = new ConcurrentHashMap<>();

    public static void main(String[] args) {

        int SCREEN_PORT = 5002;
        int CONTROL_PORT = 6002;
        int MESSAGES_PORT = 7002;
        new Thread(() -> startPort(SCREEN_PORT)).start();
        new Thread(() -> startPort(CONTROL_PORT)).start();
        new Thread(() -> startPort(MESSAGES_PORT)).start();
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
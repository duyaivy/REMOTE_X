package serverRelay;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Map;

public class ClientHandler extends Thread {
    private final Socket clientSocket;
    private final Map<String, Session> activeSessions;
    private final int MAX_CLIENTS = 100;

    public ClientHandler(Socket clientSocket, Map<String, Session> activeSessions) {
        this.clientSocket = clientSocket;
        this.activeSessions = activeSessions;
    }

    @Override
    public void run() {
        try (DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());
                DataInputStream dis = new DataInputStream(clientSocket.getInputStream())) {

            String[] data = dis.readUTF().split(",");
            if (data.length < 3) {
                System.err.println("Invalid message format received.");
                return;
            }
            String username = data[0].trim();
            String password = data[1].trim();
            String type = data[2].trim();

            if ("sharer".equals(type)) {
                handleSharer(username, password, dos);
            } else if ("viewer".equals(type)) { // Đổi tên "controller" thành "viewer" để đồng bộ
                handleViewer(username, password, dos);
            }

        } catch (Exception e) {
            System.err.println("Handler error for " + clientSocket.getRemoteSocketAddress() + ": " + e.getMessage());
        }
    }

    private void handleSharer(String username, String password, DataOutputStream dos) throws IOException {
        synchronized (activeSessions) {
            Session session = activeSessions.get(username);
            if (session == null) {
                if (activeSessions.size() >= MAX_CLIENTS) {
                    dos.writeUTF("false,Server is full");
                    return;
                }
                session = new Session(username, password, activeSessions);
                activeSessions.put(username, session);
            }

            // Dùng getLocalPort() để xác định đúng port server
            session.setSharerSocket(clientSocket);

            // **SỬA LỖI #1: Gửi lại phản hồi cho client**
            if (session.isSharerReady()) {
                System.out.println("Sharer '" + username + "' is now fully connected and ready.");
                dos.writeUTF("true,Sharer is ready");
            } else {
                System.out.println("Sharer '" + username + "' connected one channel. Waiting for the other.");
                dos.writeUTF("true,Channel connected, waiting for the other.");
            }
        }
    }

    private void handleViewer(String username, String password, DataOutputStream dos) throws IOException {
        Session session;
        synchronized (activeSessions) {
            session = activeSessions.get(username);
        }

        if (session == null) {
            dos.writeUTF("false,Session not found");
            return;
        }
        if (!session.isSharerReady()) {
            dos.writeUTF("false,Session is not ready yet");
            return;
        }
        if (!session.checkPassword(password)) {
            dos.writeUTF("false,Invalid password");
            return;
        }

        // Dùng getLocalPort() để xác định đúng port server
        session.setViewerSocketAndAttemptRelay(clientSocket);
        dos.writeUTF("true,Channel connected, attempting to start relay.");
    }
}
package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Map;

/**
 * Xử lý TCP handshake cho từng client connection
 * Screen channel chỉ dùng TCP để handshake, sau đó chuyển sang UDP
 */
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
        DataInputStream dis = null;
        DataOutputStream dos = null;

        try {
            dis = new DataInputStream(clientSocket.getInputStream());
            dos = new DataOutputStream(clientSocket.getOutputStream());

            // Đọc message: "username,password,type,width,height,connectType"
            System.out.println("[ClientHandler] Waiting for client message...");
            String message = dis.readUTF();
            System.out.println("[ClientHandler] Received: " + message);

            String[] data = message.split(",");
            if (data.length < 6) {
                System.err
                        .println("[ClientHandler] Invalid message format (expected 6 parts, got " + data.length + ")");
                dos.writeUTF("false,Invalid message format");
                dos.flush();
                clientSocket.close();
                return;
            }

            String username = data[0].trim();
            String password = data[1].trim();
            String type = data[2].trim(); // "sharer" hoặc "viewer"
            String w = data[3].trim();
            String h = data[4].trim();
            String connectType = data[5].trim(); // "screen", "control", "chat"

            System.out.println("[ClientHandler] " + type + " connecting: " + username +
                    " (" + connectType + ")");

            if ("sharer".equals(type)) {
                handleSharer(username, password, connectType, w, h, dos);
            } else if ("viewer".equals(type)) {
                handleViewer(username, password, connectType, w, h, dos);
            } else {
                dos.writeUTF("false,Invalid client type");
                dos.flush();
                clientSocket.close();
            }

        } catch (Exception e) {
            System.err.println("[ClientHandler] Error: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
            try {
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();
                }
            } catch (IOException ioException) {
                // Ignore
            }
        }
    }

    private void handleSharer(String username, String password, String connectType,
            String w, String h, DataOutputStream dos) throws IOException {

        synchronized (activeSessions) {
            Session session = activeSessions.get(username);

            // Tạo session mới nếu chưa có
            if (session == null) {
                if (activeSessions.size() >= MAX_CLIENTS) {
                    dos.writeUTF("false,Server is full");
                    clientSocket.close();
                    return;
                }
                session = new Session(username, password, w, h, activeSessions);
                activeSessions.put(username, session);
                System.out.println("[ClientHandler] ✓ Created new session: " + username);
            }

            // Kiểm tra session đã active chưa
            if (session.isActive()) {
                System.out.println("[ClientHandler] ✗ Session already active");
                dos.writeUTF("false,Session is already active");
                clientSocket.close();
                return;
            }

            // Kiểm tra password
            if (!session.checkPassword(password)) {
                System.out.println("[ClientHandler] ✗ Password mismatch");
                dos.writeUTF("false,Password mismatch");
                clientSocket.close();
                return;
            }

            // ===== XỬ LÝ SCREEN CHANNEL =====
            if ("screen".equals(connectType)) {
                // SCREEN channel chỉ dùng TCP để handshake
                System.out.println("[ClientHandler] Screen channel - TCP handshake only");
                dos.writeUTF("true,Use UDP on port 5001");
                dos.flush();

                // Đánh dấu sharer screen ready (chờ UDP REGISTER)
                session.setSharerScreenReady(true);

                // Đóng TCP socket ngay
                clientSocket.close();
                System.out.println("[ClientHandler] ✓ Screen TCP handshake done, closed socket");
                return;
            }

            // ===== XỬ LÝ CONTROL + CHAT CHANNELS (TCP) =====
            session.setSharerSocket(clientSocket, connectType);

            if (session.isSharerReady()) {
                dos.writeUTF("true,Sharer is ready");
                System.out.println("[ClientHandler] ✓ Sharer fully ready: " + username);
            } else {
                dos.writeUTF("true,Channel connected, waiting for others");
                System.out.println("[ClientHandler] ⏳ Waiting for other channels");
            }
        }
    }

    private void handleViewer(String username, String password, String connectType,
            String width, String height, DataOutputStream dos) throws IOException {

        Session session;
        synchronized (activeSessions) {
            session = activeSessions.get(username);
        }

        // Kiểm tra session tồn tại
        if (session == null) {
            dos.writeUTF("false,Session not found");
            clientSocket.close();
            return;
        }

        // Kiểm tra sharer ready
        if (!session.isSharerReady()) {
            dos.writeUTF("false,Sharer is not ready yet");
            clientSocket.close();
            return;
        }

        // Kiểm tra password
        if (!session.checkPassword(password)) {
            dos.writeUTF("false,Invalid password");
            clientSocket.close();
            return;
        }

        System.out.println("[ClientHandler] Viewer connecting: " + username +
                " (" + connectType + ")");

        // ===== XỬ LÝ SCREEN CHANNEL =====
        if ("screen".equals(connectType)) {
            // SCREEN channel chỉ dùng TCP để handshake
            System.out.println("[ClientHandler] Viewer screen - TCP handshake only");
            dos.writeUTF("true," + session.getWidth() + "," + session.getHeight() + ",UDP:5001");
            dos.flush();

            session.setViewerScreenReady(true);

            // Đóng TCP socket ngay
            clientSocket.close();
            System.out.println("[ClientHandler] ✓ Viewer screen TCP handshake done, closed socket");
            return;
        }

        session.setViewerSocketAndAttemptRelay(clientSocket, connectType);

        switch (connectType) {
            case "control":
                dos.writeUTF("true,control_ok");
                break;
            case "chat":
                dos.writeUTF("true,chat_ok");
                break;
            default:
                dos.writeUTF("false,Unknown connectType");
                break;
        }
    }
}
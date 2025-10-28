package server;

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
        // KHÔNG DÙNG try-with-resources ở đây nữa
        try {
            DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
            DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());

            String[] data = dis.readUTF().split(",");
            if (data.length < 6) { // Cần 6 phần tử: user, pass, type, connectType, width, height
                System.err.println("Invalid message format received from " + clientSocket.getRemoteSocketAddress());
                dos.writeUTF("false,Invalid message format");
                clientSocket.close(); // Đóng socket nếu handshake lỗi
                return;
            }
            String username = data[0].trim();
            String password = data[1].trim();
            String type = data[2].trim();
            String w = data[3].trim();
            String h = data[4].trim();
            String connectType = data[5].trim();
            System.out.println("New connection: " + username + ", type: " + type + ", connectType: " + connectType
                    + ", from " + clientSocket.getRemoteSocketAddress() + " " + w + "x" + h);
            if ("sharer".equals(type)) {
                handleSharer(username, password, connectType, w, h, dos);
            } else if ("viewer".equals(type)) {
                handleViewer(username, password, connectType, w, h, dos);
            } else {
                dos.writeUTF("false,Invalid client type");
                clientSocket.close(); // Đóng socket nếu handshake lỗi
            }

        } catch (Exception e) {
            System.err.println("Handler error for " + clientSocket.getRemoteSocketAddress() + ": " + e.getMessage());
            // Đảm bảo đóng socket nếu có lỗi xảy ra trong quá trình handshake
            try {
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();
                }
            } catch (IOException ioException) {
                // Ignore
            }
        }
    }

    private void handleSharer(String username, String password, String connectType, String w, String h,
            DataOutputStream dos)
            throws IOException {
        synchronized (activeSessions) {
            Session session = activeSessions.get(username);
            if (session == null) {
                if (activeSessions.size() >= MAX_CLIENTS) {
                    dos.writeUTF("false,Server is full");
                    clientSocket.close(); // Đóng socket vì không thể tạo session
                    return;
                }
                session = new Session(username, password, w, h, activeSessions);
                activeSessions.put(username, session);
            }
            session.setSharerSocket(clientSocket, connectType);

            if (session.isSharerReady()) {
                System.out.println("Sharer '" + username + "' is now fully connected and ready.");
                dos.writeUTF("true,Sharer is ready");
            } else {
                System.out.println("Sharer '" + username + "' connected one channel. Waiting for the other.");
                dos.writeUTF("true,Channel connected, waiting for the other.");
            }
        }
    }

    private void handleViewer(String username, String password, String connectType, String width, String height,
            DataOutputStream dos)
            throws IOException {
        Session session;
        synchronized (activeSessions) {
            session = activeSessions.get(username);
        }

        if (session == null) {
            dos.writeUTF("false,Session not found");
            clientSocket.close(); // Đóng socket
            return;
        }
        if (!session.isSharerReady()) {
            dos.writeUTF("false,Session is not ready yet");
            clientSocket.close(); // Đóng socket
            return;
        }
        if (!session.checkPassword(password)) {
            dos.writeUTF("false,Invalid password");
            clientSocket.close(); // Đóng socket
            return;
        }

        // Giao socket cho session và bắt đầu relay dữ liệu
        // Hàm này giờ đây sẽ chịu trách nhiệm khởi động các thread relay
        session.setViewerSocketAndAttemptRelay(clientSocket, connectType);
        dos.writeUTF("true," + session.getWidth() + "," + session.getHeight());
    }
}

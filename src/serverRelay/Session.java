package serverRelay;

import java.io.IOException;
import java.net.Socket;
import java.util.Map;

public class Session {
    private final String username;
    private final String password;
    private final Map<String, Session> sessionMap;

    private Socket sharerControlSocket;
    private Socket sharerDataSocket;
    private Socket viewerControlSocket;
    private Socket viewerDataSocket;

    public Session(String username, String password, Map<String, Session> sessionMap) {
        this.username = username;
        this.password = password;
        this.sessionMap = sessionMap;
    }

    public boolean checkPassword(String password) {
        return this.password.equals(password);
    }

    public synchronized boolean isSharerReady() {
        // **SỬA LOGIC: Yêu cầu cả hai socket phải được kết nối**
        return sharerControlSocket != null && sharerDataSocket != null;
    }

    public synchronized void setSharerSocket(Socket socket) {
        // **SỬA LỖI #2: Dùng getLocalPort()**
        int serverPort = socket.getLocalPort();

        if (serverPort == 5000) { // Screen sharing port
            this.sharerDataSocket = socket;
            System.out.println("Sharer data socket set for: " + username);
        } else if (serverPort == 6000) { // Control port
            this.sharerControlSocket = socket;
            System.out.println("Sharer control socket set for: " + username);
        }
    }

    public synchronized void setViewerSocketAndAttemptRelay(Socket socket) throws IOException {
        // **SỬA LỖI #2: Dùng getLocalPort()**
        int serverPort = socket.getLocalPort();

        if (serverPort == 6000) { // Control port
            this.viewerControlSocket = socket;
            System.out.println("Viewer control socket set for: " + username);
        } else if (serverPort == 5000) { // Data port
            this.viewerDataSocket = socket;
            System.out.println("Viewer data socket set for: " + username);
        }

        // Chỉ bắt đầu relay khi cả 4 socket đã sẵn sàng
        if (isSharerReady() && viewerControlSocket != null && viewerDataSocket != null) {
            System.out.println("All sockets ready for session '" + username + "'. Starting relay.");

            new RelayThread(sharerDataSocket.getInputStream(), viewerDataSocket.getOutputStream()).start();
            new RelayThread(viewerDataSocket.getInputStream(), sharerDataSocket.getOutputStream()).start();
            new RelayThread(sharerControlSocket.getInputStream(), viewerControlSocket.getOutputStream()).start();
            new RelayThread(viewerControlSocket.getInputStream(), sharerControlSocket.getOutputStream()).start();

            // Xóa phiên khỏi map sau khi đã ghép nối thành công
            sessionMap.remove(this.username);
        }
    }
}
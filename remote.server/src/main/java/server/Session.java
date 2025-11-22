package server;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Map;

/**
 * Quản lý một session screen sharing
 * - TCP sockets: Control + Chat
 * - UDP info: Screen data addresses
 */
public class Session {
    private final String username;
    private final String password;
    private final String width;
    private final String height;
    private final Map<String, Session> sessionMap;

    // TCP sockets (Control + Chat only)
    private Socket sharerControlSocket;
    private Socket sharerChatSocket;
    private Socket viewerControlSocket;
    private Socket viewerChatSocket;

    // UDP info (Screen data)
    private boolean sharerScreenReady = false;
    private boolean viewerScreenReady = false;
    private UDPClientInfo sharerUDPInfo;
    private UDPClientInfo viewerUDPInfo;

    private boolean relayStarted = false;
    private String status;
    private boolean startSessionSent = false;
    private long lastReconnectionTime = 0;

    public Session(String username, String password, String width, String height,
            Map<String, Session> sessionMap) {
        this.username = username;
        this.password = password;
        this.width = width;
        this.height = height;
        this.sessionMap = sessionMap;
        this.status = "waiting";

        System.out.println("[Session] Created: " + username + " (" + width + "x" + height + ")");
    }

    // ===== BASIC GETTERS =====

    public boolean checkPassword(String password) {
        return this.password.equals(password);
    }

    public synchronized boolean isSharerReady() {
        // Sharer ready = Control TCP + Chat TCP + Screen UDP registered
        return sharerControlSocket != null &&
                sharerChatSocket != null &&
                sharerScreenReady;
    }

    public synchronized String getStatus() {
        return this.status;
    }

    public synchronized boolean isActive() {
        return "active".equals(this.status);
    }

    public String getWidth() {
        return width;
    }

    public String getHeight() {
        return height;
    }

    public String getUsername() {
        return username;
    }

    // ===== UDP METHODS =====

    public synchronized void setSharerScreenReady(boolean ready) {
        this.sharerScreenReady = ready;
        System.out.println("[Session] Sharer screen ready: " + ready);
    }

    public synchronized void setViewerScreenReady(boolean ready) {
        this.viewerScreenReady = ready;
        System.out.println("[Session] Viewer screen ready: " + ready);
    }

    /**
     * Đăng ký địa chỉ UDP của sharer
     */
    public synchronized void registerSharerUDP(InetAddress addr, int port, int clientId) {
        this.sharerUDPInfo = new UDPClientInfo(addr, port, clientId);
        System.out.println("[Session] ✓ Sharer UDP registered: " + sharerUDPInfo);
        checkAllConnectionsReady();
    }

    /**
     * Đăng ký địa chỉ UDP của viewer
     */
    public synchronized void registerViewerUDP(InetAddress addr, int port, int clientId) {
        this.viewerUDPInfo = new UDPClientInfo(addr, port, clientId);
        System.out.println("[Session] ✓ Viewer UDP registered: " + viewerUDPInfo);
        checkAllConnectionsReady();
    }

    public synchronized UDPClientInfo getSharerUDPInfo() {
        return sharerUDPInfo;
    }

    public synchronized UDPClientInfo getViewerUDPInfo() {
        return viewerUDPInfo;
    }

    // ===== TCP METHODS =====

    /**
     * Set TCP socket cho sharer (Control hoặc Chat)
     */
    public synchronized void setSharerSocket(Socket socket, String connectType)
            throws IOException {
        if (connectType.equals("control")) {
            this.sharerControlSocket = socket;
            System.out.println("[Session] Sharer control socket set");
        } else if (connectType.equals("chat")) {
            this.sharerChatSocket = socket;
            System.out.println("[Session] Sharer chat socket set");
        }
        checkAllConnectionsReady();
    }

    /**
     * Set TCP socket cho viewer (Control hoặc Chat)
     */
    public synchronized void setViewerSocketAndAttemptRelay(Socket viewerSocket,
            String connectType) {
        if ("control".equals(connectType)) {
            this.viewerControlSocket = viewerSocket;
            System.out.println("[Session] Viewer control socket set");
        } else if ("chat".equals(connectType)) {
            this.viewerChatSocket = viewerSocket;
            System.out.println("[Session] Viewer chat socket set");
        }

        checkAllConnectionsReady();
    }

    /**
     * Kiểm tra tất cả connections đã sẵn sàng chưa
     * Cần: Control TCP (2), Chat TCP (2), Screen UDP (2)
     */
    private void checkAllConnectionsReady() {
        boolean allReady = sharerControlSocket != null && viewerControlSocket != null &&
                sharerChatSocket != null && viewerChatSocket != null &&
                sharerUDPInfo != null && viewerUDPInfo != null;

        if (allReady && !relayStarted) {
            System.out.println("[Session] ✓✓✓ ALL connections ready! Starting relay...");
            startRelay();
        } else if (!allReady) {
            System.out.println("[Session] ⏳ Waiting for connections:");
            System.out.println("  - Sharer Control TCP: " + (sharerControlSocket != null ? "✓" : "✗"));
            System.out.println("  - Viewer Control TCP: " + (viewerControlSocket != null ? "✓" : "✗"));
            System.out.println("  - Sharer Chat TCP:    " + (sharerChatSocket != null ? "✓" : "✗"));
            System.out.println("  - Viewer Chat TCP:    " + (viewerChatSocket != null ? "✓" : "✗"));
            System.out.println("  - Sharer Screen UDP:  " + (sharerUDPInfo != null ? "✓" : "✗"));
            System.out.println("  - Viewer Screen UDP:  " + (viewerUDPInfo != null ? "✓" : "✗"));
        }
    }

    /**
     * Khởi động relay threads cho TCP channels
     */
    private void startRelay() {
        this.relayStarted = true;
        this.status = "active";

        System.out.println("[Session] ═══════════════════════════════════");
        System.out.println("[Session] Status changed to: ACTIVE");
        System.out.println("[Session] ═══════════════════════════════════");

        try {
            // Gửi START_SESSION signal qua Control channel
            if (!startSessionSent) {
                DataOutputStream sharerDos = new DataOutputStream(
                        sharerControlSocket.getOutputStream());
                sharerDos.writeUTF("START_SESSION");
                sharerDos.flush();
                startSessionSent = true;
                System.out.println("[Session] ✓ START_SESSION sent to sharer");
            }

            // Start TCP relay threads (Control + Chat only, Screen qua UDP)
            System.out.println("[Session] Starting TCP relay threads...");

            new RelayThread(sharerControlSocket, viewerControlSocket, this, "sharer-control").start();
            new RelayThread(viewerControlSocket, sharerControlSocket, this, "viewer-control").start();

            new RelayThread(sharerChatSocket, viewerChatSocket, this, "sharer-chat").start();
            new RelayThread(viewerChatSocket, sharerChatSocket, this, "viewer-chat").start();

            System.out.println("[Session] ✓ All TCP relay threads started");
            System.out.println("[Session] ✓ UDP relay handled by UDPRelayHandler");

        } catch (Exception e) {
            System.err.println("[Session] ✗ Failed to start relay: " + e.getMessage());
            e.printStackTrace();
            cleanup();
        }
    }

    // ===== DISCONNECT HANDLING =====

    public synchronized boolean isInReconnectionGracePeriod() {
        long timeSinceReconnection = System.currentTimeMillis() - lastReconnectionTime;
        return timeSinceReconnection < 3000; // 3 seconds grace period
    }

    public synchronized boolean isCurrentSocket(Socket socket) {
        return socket == sharerControlSocket || socket == sharerChatSocket ||
                socket == viewerControlSocket || socket == viewerChatSocket;
    }

    public synchronized void onSharerDisconnect(Socket disconnectedSocket) {
        if (!isCurrentSocket(disconnectedSocket)) {
            System.out.println("[Session] Ignoring disconnect from OLD socket");
            return;
        }

        System.out.println("[Session] ✗ Sharer disconnected, cleaning up session");
        cleanup();
        sessionMap.remove(this.username);
        System.out.println("[Session] Session removed: " + username);
    }

    public synchronized void onViewerDisconnect(Socket disconnectedSocket) {
        if (!isCurrentSocket(disconnectedSocket)) {
            System.out.println("[Session] Ignoring disconnect from OLD socket");
            return;
        }

        System.out.println("[Session] ⚠ Viewer disconnected");

        if (!isInReconnectionGracePeriod()) {
            this.status = "waiting";
            this.relayStarted = false;
            System.out.println("[Session] Status changed to: waiting");
        } else {
            System.out.println("[Session] In grace period - keeping status: " + this.status);
        }

        // Đóng viewer sockets
        try {
            if (viewerControlSocket != null)
                viewerControlSocket.close();
            if (viewerChatSocket != null)
                viewerChatSocket.close();
        } catch (IOException e) {
            // Ignore
        }

        this.viewerControlSocket = null;
        this.viewerChatSocket = null;
        this.viewerUDPInfo = null;
        this.viewerScreenReady = false;

        System.out.println("[Session] Viewer disconnected. Sharer still active.");
    }

    private void cleanup() {
        System.out.println("[Session] Cleanup: Closing all connections...");

        // Đóng tất cả TCP sockets
        try {
            if (sharerControlSocket != null)
                sharerControlSocket.close();
            if (sharerChatSocket != null)
                sharerChatSocket.close();
            if (viewerControlSocket != null)
                viewerControlSocket.close();
            if (viewerChatSocket != null)
                viewerChatSocket.close();
        } catch (IOException e) {
            // Ignore
        }

        // Clear UDP info
        sharerUDPInfo = null;
        viewerUDPInfo = null;
        sharerScreenReady = false;
        viewerScreenReady = false;

        System.out.println("[Session] ✓ Cleanup completed");
    }

    // ===== INNER CLASS =====

    /**
     * Lưu thông tin địa chỉ UDP của client
     */
    public static class UDPClientInfo {
        public final InetAddress address;
        public final int port;
        public final int clientId;

        public UDPClientInfo(InetAddress address, int port, int clientId) {
            this.address = address;
            this.port = port;
            this.clientId = clientId;
        }

        @Override
        public String toString() {
            return address.getHostAddress() + ":" + port + " (clientId=" + clientId + ")";
        }
    }
}

package server;

import java.io.DataOutputStream; 
import java.io.IOException;
import java.net.Socket;
import java.util.Map;

public class Session {
    private final String username;
    private final String password;
    private final String width;
    private final String height;
    private final Map<String, Session> sessionMap;
    
    private Socket sharerControlSocket;
    private Socket sharerScreenSocket;
    private Socket viewerControlSocket;
    private Socket viewerScreenSocket;
    private Socket sharerChatSocket;
    private Socket viewerChatSocket;
    private boolean relayStarted = false;
    
    private String status;
    private boolean startSessionSent = false;
    private boolean sharerDisconnectCalled = false;
    private boolean viewerDisconnectCalled = false;
    private long lastReconnectionTime = 0; // Timestamp của lần reconnect cuối

    public Session(String username, String password, String width, String height, Map<String, Session> sessionMap) {
        this.username = username;
        this.password = password;
        this.width = width;
        this.height = height;
        this.sessionMap = sessionMap;
        this.status = "waiting";
        
        System.out.println("[Session] Created new session: " + username + " with status: " + this.status);
    }

    public boolean checkPassword(String password) {
        return this.password.equals(password);
    }

    public synchronized boolean isSharerReady() {
        return sharerControlSocket != null && sharerScreenSocket != null && sharerChatSocket != null;
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
    
    public synchronized boolean isInReconnectionGracePeriod() {
        long timeSinceReconnection = System.currentTimeMillis() - lastReconnectionTime;
        return timeSinceReconnection < 2000; // 2 giây grace period
    }
    
    
    public synchronized boolean isCurrentSocket(Socket socket) {
        return socket == sharerControlSocket || socket == sharerScreenSocket || socket == sharerChatSocket ||
               socket == viewerControlSocket || socket == viewerScreenSocket || socket == viewerChatSocket;
    }

    public synchronized void setSharerSocket(Socket socket, String connectType) throws IOException {
        if (connectType.equals("screen")) {
            this.sharerScreenSocket = socket;
        } else if (connectType.equals("control")) {
            this.sharerControlSocket = socket;
        } else if (connectType.equals("chat")) {
            this.sharerChatSocket = socket;
        }
    }

    public synchronized void setViewerSocketAndAttemptRelay(Socket viewerSocket, String connectType) {
        

        if (relayStarted && "active".equals(status)) {
            
            handleViewerReconnection(viewerSocket, connectType);
            return;
        }
        
        System.out.println("[Session] Viewer connecting (" + connectType + "). Current status: " + status);

        if ("screen".equals(connectType)) {
            this.viewerScreenSocket = viewerSocket;
        } else if ("control".equals(connectType)) {
            this.viewerControlSocket = viewerSocket;
        } else if ("chat".equals(connectType)) {
            this.viewerChatSocket = viewerSocket;
        }

        // Kiểm tra đủ 6 sockets
        if (sharerScreenSocket != null && viewerScreenSocket != null &&
                sharerControlSocket != null && viewerControlSocket != null &&
                sharerChatSocket != null && viewerChatSocket != null) {

            System.out.println("All 6 sockets connected. Sending START_SESSION signal TO SHARER ONLY...");
            this.relayStarted = true;
            this.status = "active";
            this.viewerDisconnectCalled = false; // Reset flag
            
            System.out.println("[Session] Status changed to: " + this.status + " for user: " + this.username);
            
            try {
                if (!startSessionSent) {
                    DataOutputStream sharerDos = new DataOutputStream(sharerControlSocket.getOutputStream());
                    sharerDos.writeUTF("START_SESSION");
                    sharerDos.flush();
                    startSessionSent = true;
                    System.out.println("[Session] ✅ START_SESSION sent to sharer");
                } else {
                    System.out.println("[Session] ⏭️ START_SESSION already sent, skipping...");
                }
                
                System.out.println("[Session] Starting relay threads with monitoring...");
                
                new RelayThread(sharerScreenSocket, viewerScreenSocket, this, "sharer").start();
                new RelayThread(viewerScreenSocket, sharerScreenSocket, this, "viewer").start();

                new RelayThread(sharerControlSocket, viewerControlSocket, this, "sharer").start();
                new RelayThread(viewerControlSocket, sharerControlSocket, this, "viewer").start();

                new RelayThread(sharerChatSocket, viewerChatSocket, this, "sharer").start();
                new RelayThread(viewerChatSocket, sharerChatSocket, this, "viewer").start();

            } catch (Exception e) {
                System.err.println("Failed to send START_SESSION or start relay: " + e.getMessage());
                cleanup();
            }
        }
    }
    
    private void handleViewerReconnection(Socket newSocket, String connectType) {
        System.out.println("[Session] Handling viewer reconnection for: " + connectType);
        
        // Đánh dấu thời gian reconnection
        this.lastReconnectionTime = System.currentTimeMillis();
        
        try {
            Socket oldSocket = null;
            
            if ("screen".equals(connectType)) {
                oldSocket = this.viewerScreenSocket;
                this.viewerScreenSocket = newSocket;
                System.out.println("[Session] Replaced viewerScreenSocket");
                
            } else if ("control".equals(connectType)) {
                oldSocket = this.viewerControlSocket;
                this.viewerControlSocket = newSocket;
                System.out.println("[Session] Replaced viewerControlSocket");
                
            } else if ("chat".equals(connectType)) {
                oldSocket = this.viewerChatSocket;
                this.viewerChatSocket = newSocket;
                System.out.println("[Session] Replaced viewerChatSocket");
            }

            if (oldSocket != null && !oldSocket.isClosed()) {
                oldSocket.close();
                System.out.println("[Session] Closed old socket");
            }

            // Restart relay threads 
            restartRelayForConnection(connectType);
            
            System.out.println("[Session] ✅ Viewer reconnection successful for: " + connectType);
            
        } catch (Exception e) {
            System.err.println("[Session] ❌ Error during viewer reconnection: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void restartRelayForConnection(String connectType) {
        System.out.println("[Session] Restarting relay threads for: " + connectType);
        
        try {
            if ("screen".equals(connectType)) {
                new RelayThread(sharerScreenSocket, viewerScreenSocket, this, "sharer").start();
                new RelayThread(viewerScreenSocket, sharerScreenSocket, this, "viewer").start();
                System.out.println("[Session] ✅ Restarted screen relay threads");
                
            } else if ("control".equals(connectType)) {
                new RelayThread(sharerControlSocket, viewerControlSocket, this, "sharer").start();
                new RelayThread(viewerControlSocket, sharerControlSocket, this, "viewer").start();
                System.out.println("[Session] ✅ Restarted control relay threads");
                
            } else if ("chat".equals(connectType)) {
                new RelayThread(sharerChatSocket, viewerChatSocket, this, "sharer").start();
                new RelayThread(viewerChatSocket, sharerChatSocket, this, "viewer").start();
                System.out.println("[Session] ✅ Restarted chat relay threads");
            }
            
        } catch (Exception e) {
            System.err.println("[Session] ❌ Error restarting relay: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
  
    public synchronized void onSharerDisconnect(Socket disconnectedSocket) {
        // Kiểm tra xem socket này có còn là socket hiện tại không
        if (!isCurrentSocket(disconnectedSocket)) {
            System.out.println("[Session] Ignoring disconnect from OLD socket (reconnection in progress)");
            return;
        }
        
        if (sharerDisconnectCalled) {
            System.out.println("[Session] onSharerDisconnect() already called, skipping...");
            return;
        }
        sharerDisconnectCalled = true;
        cleanup();
        sessionMap.remove(this.username);
        
        System.out.println("[Session] Session removed from map: " + username);
    }
    
    public synchronized void onViewerDisconnect(Socket disconnectedSocket) {
        // Kiểm tra xem socket này có còn là socket hiện tại không
        if (!isCurrentSocket(disconnectedSocket)) {
            System.out.println("[Session] Ignoring disconnect from OLD socket (reconnection in progress)");
            return;
        }
        
        if (viewerDisconnectCalled) {
            System.out.println("[Session] onViewerDisconnect() already called, skipping...");
            return;
        }
        viewerDisconnectCalled = true;
        this.status = "waiting";
        this.relayStarted = false;
        
        System.out.println("[Session] Status changed to: " + this.status + " for user: " + this.username);
        
        try {
            if (viewerScreenSocket != null) {
                viewerScreenSocket.close();
                System.out.println("[Session] Closed viewerScreenSocket");
            }
        } catch (IOException e) {}
        
        try {
            if (viewerControlSocket != null) {
                viewerControlSocket.close();
                System.out.println("[Session] Closed viewerControlSocket");
            }
        } catch (IOException e) {}
        
        try {
            if (viewerChatSocket != null) {
                viewerChatSocket.close();
                System.out.println("[Session] Closed viewerChatSocket");
            }
        } catch (IOException e) {}
        
        this.viewerScreenSocket = null;
        this.viewerControlSocket = null;
        this.viewerChatSocket = null;
        
        System.out.println("[Session] Viewer sockets reset. Sharer sockets still active. Waiting for viewer reconnection...");
    }
    
    private void cleanup() {
        System.out.println("[Session] Cleanup: Closing all sockets...");
        
        try { 
            if (sharerScreenSocket != null) sharerScreenSocket.close();
        } catch (IOException e) {}
        try { 
            if (sharerControlSocket != null) sharerControlSocket.close();
        } catch (IOException e) {}
        try { 
            if (viewerScreenSocket != null) viewerScreenSocket.close();
        } catch (IOException e) {}
        try { 
            if (viewerControlSocket != null) viewerControlSocket.close();
        } catch (IOException e) {}
        try { 
            if (sharerChatSocket != null) sharerChatSocket.close();
        } catch (IOException e) {}
        try { 
            if (viewerChatSocket != null) viewerChatSocket.close();
        } catch (IOException e) {}
    }
}
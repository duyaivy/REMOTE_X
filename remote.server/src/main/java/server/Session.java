package server; // Hoặc package serverRelay của bạn

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

    public Session(String username, String password, String width, String height, Map<String, Session> sessionMap) {
        this.username = username;
        this.password = password;
        this.width = width;
        this.height = height;
        this.sessionMap = sessionMap;
    }

    public boolean checkPassword(String password) {
        return this.password.equals(password);
    }

    public synchronized boolean isSharerReady() {
        return sharerControlSocket != null && sharerScreenSocket != null && sharerChatSocket != null;
    }

    public String getWidth() { return width; }
    public String getHeight() { return height; }

    public synchronized void setSharerSocket(Socket socket, String connectType) throws IOException {

        if (connectType.equals("screen")) {
            this.sharerScreenSocket = socket;
        } else if (connectType.equals("control")) {
            this.sharerControlSocket = socket;
        }
        else if (connectType.equals("chat")) {
            this.sharerChatSocket = socket;
        }
    }

    public synchronized void setViewerSocketAndAttemptRelay(Socket viewerSocket, String connectType) {
        
        if (relayStarted) {
            try { viewerSocket.close(); } catch (IOException e) {}
            return;
        }

    
        if ("screen".equals(connectType)) {
            this.viewerScreenSocket = viewerSocket;
        } else if ("control".equals(connectType)) {
            this.viewerControlSocket = viewerSocket;
        }
        else if ("chat".equals(connectType)) {
            this.viewerChatSocket = viewerSocket;
        }


        if (sharerScreenSocket != null && viewerScreenSocket != null &&
                sharerControlSocket != null && viewerControlSocket != null &&
                sharerChatSocket != null && viewerChatSocket != null) {

            System.out.println("All 6 sockets connected. Sending START_SESSION signal TO SHARER ONLY...");
            this.relayStarted = true;
            
            try {
           
                DataOutputStream sharerDos = new DataOutputStream(sharerControlSocket.getOutputStream());
                sharerDos.writeUTF("START_SESSION");
                sharerDos.flush();
                
              
                new RelayThread(sharerScreenSocket, viewerScreenSocket).start();
                new RelayThread(viewerScreenSocket, sharerScreenSocket).start();
                new RelayThread(sharerControlSocket, viewerControlSocket).start();
                new RelayThread(viewerControlSocket, sharerControlSocket).start();
                new RelayThread(sharerChatSocket, viewerChatSocket).start();
                new RelayThread(viewerChatSocket, sharerChatSocket).start();

            } catch (Exception e) {
                System.err.println("Failed to send START_SESSION or start relay: " + e.getMessage());
                cleanup();
            }

            sessionMap.remove(this.username);
        }
    }
    
    private void cleanup() {

        try { if (sharerScreenSocket != null) sharerScreenSocket.close(); } catch (IOException e) {}
        try { if (sharerControlSocket != null) sharerControlSocket.close(); } catch (IOException e) {}
        try { if (viewerScreenSocket != null) viewerScreenSocket.close(); } catch (IOException e) {}
        try { if (viewerControlSocket != null) viewerControlSocket.close(); } catch (IOException e) {}
        try { if (sharerChatSocket != null) sharerChatSocket.close(); } catch (IOException e) {}
        try { if (viewerChatSocket != null) viewerChatSocket.close(); } catch (IOException e) {}
        sessionMap.remove(this.username);
    }
}
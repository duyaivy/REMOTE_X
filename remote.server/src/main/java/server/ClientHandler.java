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
        
        try {
            DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
            DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());

            String[] data = dis.readUTF().split(",");
            if (data.length < 6) { 
                System.err.println("Invalid message format received from " + clientSocket.getRemoteSocketAddress());
                dos.writeUTF("false,Invalid message format");
                clientSocket.close(); 
                return;
            }
            String username = data[0].trim();
            String password = data[1].trim();
            String type = data[2].trim();
            String w = data[3].trim();
            String h = data[4].trim();
            String connectType = data[5].trim();
            
           
            // System.out.println("New connection: " + username + ", type: " + type + ", connectType: " + connectType);

            if ("sharer".equals(type)) {
                handleSharer(username, password, connectType, w, h, dos);
            } else if ("viewer".equals(type)) {
                handleViewer(username, password, connectType, w, h, dos);
            } else {
                dos.writeUTF("false,Invalid client type");
                clientSocket.close();// Đóng socket nếu handshake lỗi
            }

        } catch (Exception e) {
            // System.err.println("Handler error for " + clientSocket.getRemoteSocketAddress() + ": " + e.getMessage());
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

        // Nó gửi "true,..." cho cả 3 kết nối, điều này là OK
        // vì MainStart (Sharer) đọc cả 3.
        synchronized (activeSessions) {
            Session session = activeSessions.get(username);
            if (session == null) {

                if (activeSessions.size() >= MAX_CLIENTS) {
                    dos.writeUTF("false,Server is full");
                    clientSocket.close();
                    return;
                }
                session = new Session(username, password, w, h, activeSessions);
                activeSessions.put(username, session);
            }

            if(session.isActive()) {
                System.out.println("[ClientHandler] REJECT: Session is ACTIVE, not allowing duplicate sharer");
                dos.writeUTF("false,Session is already active");
                clientSocket.close();
                return;
            }
            if (!session.checkPassword(password)) {
                    System.out.println("[ClientHandler] REJECT: Password mismatch for session: " + username);
                    dos.writeUTF("false,Username already exists with different password");
                    clientSocket.close();
                    return;
                }
                System.out.println("[ClientHandler] ACCEPT: Session is WAITING, allowing reconnection");

            

            
            session.setSharerSocket(clientSocket, connectType);

            if (session.isSharerReady()) {
                 System.out.println("Sharer '" + username + "' is now fully connected and ready.");
                dos.writeUTF("true,Sharer is ready");
            } else {
                 System.out.println("Sharer '" + username + "' connected one channel. Waiting for the other.");
                dos.writeUTF("true,Channel connected");
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
            clientSocket.close();
            return;
        }
        if (!session.isSharerReady()) {
            dos.writeUTF("false,Session is not ready yet");
            clientSocket.close();
            return;
        }
        if (!session.checkPassword(password)) {
            dos.writeUTF("false,Invalid password");
            clientSocket.close();
            return;
        }

        // Giao socket cho session VÀ để Session tự gửi tín hiệu "START_SESSION"
         System.out.println("[ClientHandler] Viewer connecting to session: " + username + ", status: " + session.getStatus());

        session.setViewerSocketAndAttemptRelay(clientSocket, connectType);
        
        switch (connectType) {
            case "screen":
             
                dos.writeUTF("true," + session.getWidth() + "," + session.getHeight());
                break;
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
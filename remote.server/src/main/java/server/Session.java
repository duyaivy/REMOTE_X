//package serverRelay;
//
//import java.io.IOException;
//import java.net.Socket;
//import java.util.Map;
//
//public class Session {
//    private final String username;
//    private final String password;
//    private final String width;
//    private final String height;
//    private final Map<String, Session> sessionMap;
//
//    // --- SỬA LỖI #1: Đổi tên biến cho nhất quán ---
//    private Socket sharerControlSocket;
//    private Socket sharerScreenSocket; // Đổi từ sharerDataSocket
//    private Socket viewerControlSocket;
//    private Socket viewerScreenSocket; // Đổi từ viewerDataSocket
//
//    // --- SỬA LỖI #3: Thêm biến cờ để quản lý trạng thái relay ---
//    private boolean relayStarted = false;
//
//    public Session(String username, String password, String width, String height, Map<String, Session> sessionMap) {
//        this.username = username;
//        this.password = password;
//        this.width = width;
//        this.height = height;
//        this.sessionMap = sessionMap;
//    }
//
//    public boolean checkPassword(String password) {
//        return this.password.equals(password);
//    }
//
//    public synchronized boolean isSharerReady() {
//        return sharerControlSocket != null && sharerScreenSocket != null;
//    }
//
//    public String getWidth() {
//        return width;
//    }
//
//    public String getHeight() {
//        return height;
//    }
//
//    public synchronized void setSharerSocket(Socket socket, String connectType) throws IOException {
//        if (connectType.equals("screen")) {
//            this.sharerScreenSocket = socket;
//            System.out.println("Sharer screen socket set for: " + username);
//        } else if (connectType.equals("control")) {
//            this.sharerControlSocket = socket;
//            System.out.println("Sharer control socket set for: " + username);
//        }
//    }
//
//    public synchronized void setViewerSocketAndAttemptRelay(Socket viewerSocket, String connectType) {
//        if (relayStarted) {
//
//            try {
//                System.err.println(
//                        "Relay for session '" + username + "' already started. Rejecting new viewer connection.");
//                viewerSocket.close();
//            } catch (IOException e) {
//                // Ignore
//            }
//            return;
//        }
//
//        if ("screen".equals(connectType)) {
//            this.viewerScreenSocket = viewerSocket;
//            System.out.println("Viewer screen socket set for: " + username);
//        } else if ("control".equals(connectType)) {
//            this.viewerControlSocket = viewerSocket;
//            System.out.println("Viewer control socket set for: " + username);
//        }
//
//        if (sharerScreenSocket != null && viewerScreenSocket != null &&
//                sharerControlSocket != null && viewerControlSocket != null) {
//
//            System.out.println("All 4 sockets are connected. Starting relay for session: " + username);
//            this.relayStarted = true;
//            try {
//
//                new RelayThread(sharerScreenSocket, viewerScreenSocket).start(); // sharer -> viewer
//                new RelayThread(viewerScreenSocket, sharerScreenSocket).start(); // viewer -> sharer
//                // control
//                new RelayThread(sharerControlSocket, viewerControlSocket).start(); // sharer -> viewer
//                new RelayThread(viewerControlSocket, sharerControlSocket).start(); // viewer -> sharer
//
//            } catch (Exception e) {
//                System.err.println("Failed to start relay for session '" + username + "': " + e.getMessage());
//                cleanup();
//            }
//
//            // --- SỬA LỖI #4: Dọn dẹp session sau khi đã sử dụng ---
//            // Xóa session khỏi map để không ai có thể kết nối vào nữa
//            sessionMap.remove(this.username);
//            System.out.println("Session '" + username + "' has started relay and been removed from active list.");
//        }
//    }
//
//    /**
//     * Đóng tất cả các socket còn lại của session này.
//     * Được gọi khi có lỗi xảy ra.
//     */
//    private void cleanup() {
//        try {
//            if (sharerScreenSocket != null)
//                sharerScreenSocket.close();
//        } catch (IOException e) {
//        }
//        try {
//            if (sharerControlSocket != null)
//                sharerControlSocket.close();
//        } catch (IOException e) {
//        }
//        try {
//            if (viewerScreenSocket != null)
//                viewerScreenSocket.close();
//        } catch (IOException e) {
//        }
//        try {
//            if (viewerControlSocket != null)
//                viewerControlSocket.close();
//        } catch (IOException e) {
//        }
//        sessionMap.remove(this.username);
//    }
//}

package server;

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

    // --- BƯỚC 1: Thêm socket cho Chat ---
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
        // --- BƯỚC 2: Sharer cần 3 socket để sẵn sàng ---
        return sharerControlSocket != null && sharerScreenSocket != null && sharerChatSocket != null;
    }

    public String getWidth() {
        return width;
    }

    public String getHeight() {
        return height;
    }

    public synchronized void setSharerSocket(Socket socket, String connectType) throws IOException {
        if (connectType.equals("screen")) {
            this.sharerScreenSocket = socket;
            System.out.println("Sharer screen socket set for: " + username);
        } else if (connectType.equals("control")) {
            this.sharerControlSocket = socket;
            System.out.println("Sharer control socket set for: " + username);
        }
        // --- BƯỚC 3: Nhận socket "chat" từ Sharer ---
        else if (connectType.equals("chat")) {
            this.sharerChatSocket = socket;
            System.out.println("Sharer chat socket set for: " + username);
        }
    }

    public synchronized void setViewerSocketAndAttemptRelay(Socket viewerSocket, String connectType) {
        if (relayStarted) {
            try {
                System.err.println(
                        "Relay for session '" + username + "' already started. Rejecting new viewer connection.");
                viewerSocket.close();
            } catch (IOException e) {
                // Ignore
            }
            return;
        }

        if ("screen".equals(connectType)) {
            this.viewerScreenSocket = viewerSocket;
            System.out.println("Viewer screen socket set for: " + username);
        } else if ("control".equals(connectType)) {
            this.viewerControlSocket = viewerSocket;
            System.out.println("Viewer control socket set for: " + username);
        }
        // --- BƯỚC 4: Nhận socket "chat" từ Viewer ---
        else if ("chat".equals(connectType)) {
            this.viewerChatSocket = viewerSocket;
            System.out.println("Viewer chat socket set for: " + username);
        }

        // --- BƯỚC 5: Chờ đủ 6 socket (3 screen, 3 control, 3 chat) ---
        if (sharerScreenSocket != null && viewerScreenSocket != null &&
                sharerControlSocket != null && viewerControlSocket != null &&
                sharerChatSocket != null && viewerChatSocket != null) { // <-- Sửa điều kiện

            System.out.println("All 6 sockets are connected. Starting relay for session: " + username); // <-- Sửa log
            this.relayStarted = true;
            try {
                // screen
                new RelayThread(sharerScreenSocket, viewerScreenSocket).start(); // sharer -> viewer
                new RelayThread(viewerScreenSocket, sharerScreenSocket).start(); // viewer -> sharer
                
                // control
                new RelayThread(sharerControlSocket, viewerControlSocket).start(); // sharer -> viewer
                new RelayThread(viewerControlSocket, sharerControlSocket).start(); // viewer -> sharer

                // --- BƯỚC 6: Bắt đầu chuyển tiếp chat ---
                new RelayThread(sharerChatSocket, viewerChatSocket).start(); // sharer -> viewer
                new RelayThread(viewerChatSocket, sharerChatSocket).start(); // viewer -> sharer

            } catch (Exception e) {
                System.err.println("Failed to start relay for session '" + username + "': " + e.getMessage());
                cleanup();
            }

            // Xóa session khỏi map để không ai có thể kết nối vào nữa
            sessionMap.remove(this.username);
            System.out.println("Session '" + username + "' has started relay and been removed from active list.");
        }
    }

    /**
     * Đóng tất cả các socket còn lại của session này.
     * Được gọi khi có lỗi xảy ra.
     */
    private void cleanup() {
        try {
            if (sharerScreenSocket != null)
                sharerScreenSocket.close();
        } catch (IOException e) {
        }
        try {
            if (sharerControlSocket != null)
                sharerControlSocket.close();
        } catch (IOException e) {
        }
        try {
            if (viewerScreenSocket != null)
                viewerScreenSocket.close();
        } catch (IOException e) {
        }
        try {
            if (viewerControlSocket != null)
                viewerControlSocket.close();
        } catch (IOException e) {
        }

      
        try {
            if (sharerChatSocket != null)
                sharerChatSocket.close();
        } catch (IOException e) {
        }
        try {
            if (viewerChatSocket != null)
                viewerChatSocket.close();
        } catch (IOException e) {
        }
        
        sessionMap.remove(this.username);
    }
}
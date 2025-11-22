package server;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Relay thread cho TCP channels (Control + Chat)
 * Forward data giữa sharer và viewer
 */
public class RelayThread extends Thread {
    private final InputStream inputStream;
    private final OutputStream outputStream;
    private final Socket fromSocket;
    private final Socket toSocket;
    private final Session session;
    private final String senderType; // "sharer-control", "viewer-chat", etc.

    public RelayThread(Socket fromSocket, Socket toSocket, Session session, String senderType)
            throws Exception {
        this.fromSocket = fromSocket;
        this.toSocket = toSocket;
        this.inputStream = fromSocket.getInputStream();
        this.outputStream = toSocket.getOutputStream();
        this.session = session;
        this.senderType = senderType;

        System.out.println("[RelayThread] Created: " + senderType);
    }

    @Override
    public void run() {
        byte[] buffer = new byte[16384]; // 16KB buffer
        int bytesRead;

        String threadName = Thread.currentThread().getName();
        System.out.println("[RelayThread-" + threadName + "] Started: " + senderType);

        try {
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                outputStream.flush();
            }
            System.out.println("[RelayThread-" + threadName + "] Stream ended normally: " + senderType);
        } catch (Exception e) {
            System.out.println("[RelayThread-" + threadName + "] Exception (" + senderType + "): " +
                    e.getMessage());
        } finally {
            System.out.println("[RelayThread-" + threadName + "] Calling handleDisconnection for: " +
                    senderType);
            handleDisconnection();
        }
    }

    private void handleDisconnection() {
        String threadName = Thread.currentThread().getName();

        // Đóng fromSocket
        try {
            if (fromSocket != null && !fromSocket.isClosed()) {
                fromSocket.close();
                System.out.println("[" + threadName + "] Closed fromSocket");
            }
        } catch (Exception e) {
            System.out.println("[" + threadName + "] Error closing fromSocket: " + e.getMessage());
        }

        if (session != null && senderType != null) {
            // Kiểm tra grace period
            boolean inGracePeriod = session.isInReconnectionGracePeriod();

            if (inGracePeriod) {
                System.out.println("[" + threadName + "] In grace period - ignoring disconnect");
                return;
            }

            // Kiểm tra socket còn current không
            boolean fromSocketIsCurrent = session.isCurrentSocket(fromSocket);
            boolean toSocketIsCurrent = session.isCurrentSocket(toSocket);

            if (!toSocketIsCurrent || !fromSocketIsCurrent) {
                System.out.println("[" + threadName + "] Socket is OLD - ignoring (reconnection in progress)");
                return;
            }

            // Xử lý disconnect
            if (senderType.startsWith("sharer")) {
                System.out.println("[" + threadName + "] Detected SHARER disconnect");
                session.onSharerDisconnect(fromSocket);

                // Đóng toSocket (viewer)
                try {
                    if (toSocket != null && !toSocket.isClosed()) {
                        toSocket.close();
                        System.out.println("[" + threadName + "] Closed toSocket (viewer)");
                    }
                } catch (Exception e) {
                    System.out.println("[" + threadName + "] Error closing toSocket: " + e.getMessage());
                }

            } else if (senderType.startsWith("viewer")) {
                System.out.println("[" + threadName + "] Detected VIEWER disconnect");
                session.onViewerDisconnect(fromSocket);

                // KHÔNG đóng toSocket (sharer vẫn online)
                System.out.println("[" + threadName + "] NOT closing toSocket (sharer still online)");
            }
        }

        System.out.println("[" + threadName + "] handleDisconnection() completed");
    }
}
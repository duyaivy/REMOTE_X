package monitor;

import monitor.log.LogHandler;
import monitor.log.NdjsonTailer;
import monitor.ml.AlertService;

import java.net.Socket;

public class MonitoringManager {

    private static MonitoringManager instance;
    private boolean isMonitoring = false;

    private LogHandler logHandler;
    private NdjsonTailer tailer;
    private AlertService alertService;

    private Socket screenSocket;
    private Socket chatSocket;
    private Socket controlSocket;

    private MonitoringManager() {
    }

    public static synchronized MonitoringManager getInstance() {
        if (instance == null) {
            instance = new MonitoringManager();
        }
        return instance;
    }

    /**
     * Bắt đầu monitoring với callback xử lý cảnh báo
     * 
     * @param screenSocket
     * @param chatSocket
     * @param controlSocket
     */
    public synchronized boolean startMonitoring(Socket screenSocket, Socket chatSocket, Socket controlSocket) {
        if (isMonitoring) {
            System.out.println("[MONITOR] Already monitoring!");
            return false;
        }

        try {
            this.screenSocket = screenSocket;
            this.chatSocket = chatSocket;
            this.controlSocket = controlSocket;
            alertService = new AlertService();

            logHandler = new LogHandler(alertService);

            // Start log tailer
            tailer = new NdjsonTailer(logHandler);
            tailer.start();

            isMonitoring = true;
            System.out.println("[MONITOR] ✓ Monitoring started!");
            return true;

        } catch (Exception e) {
            System.err.println("[MONITOR] ✗ Failed to start: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public synchronized void stopMonitoring() {
        if (!isMonitoring) {
            return;
        }

        try {
            System.out.println("[MONITOR] Stopping monitoring...");

            if (tailer != null) {
                tailer.stop();
            }
            if (logHandler != null) {
                logHandler.shutdown();
            }
            closeSockets();

            isMonitoring = false;
            System.out.println("[MONITOR] ✓ Monitoring stopped");

        } catch (Exception e) {
            System.err.println("[MONITOR] Error stopping: " + e.getMessage());
        }
    }

    private void closeSockets() {
        try {
            if (screenSocket != null && !screenSocket.isClosed()) {
                screenSocket.close();
                System.out.println("[MONITOR] Closed screen socket");
            }
        } catch (Exception e) {
            System.err.println("[MONITOR] Error closing screen socket: " + e.getMessage());
        }

        try {
            if (chatSocket != null && !chatSocket.isClosed()) {
                chatSocket.close();
                System.out.println("[MONITOR] Closed chat socket");
            }
        } catch (Exception e) {
            System.err.println("[MONITOR] Error closing chat socket: " + e.getMessage());
        }
        try {
            if (controlSocket != null && !controlSocket.isClosed()) {
                controlSocket.close();
                System.out.println("[MONITOR] Closed control socket");
            }
        } catch (Exception e) {
            System.err.println("[MONITOR] Error closing control socket: " + e.getMessage());
        }
    }

    public boolean isMonitoring() {
        return isMonitoring;
    }
}
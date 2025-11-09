package monitor;

import monitor.log.LogHandler;
import monitor.log.NdjsonTailer;
import monitor.ml.AlertService;
import monitor.ml.Preprocessor;

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
            Preprocessor preprocessor = new Preprocessor();
            alertService = new AlertService(preprocessor);

            logHandler = new LogHandler(alertService, preprocessor);
            tailer = new NdjsonTailer(logHandler);
            tailer.start();

            isMonitoring = true;
            System.out.println("Bắt đầu thu thập dữ liệu");
            return true;

        } catch (Exception e) {
            System.err.println("Lỗi! " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    public synchronized void stopMonitoring() {
        if (!isMonitoring) {
            return;
        }

        try {
            System.out.println("Ngừng thu thập dữ liệu...");

            if (tailer != null) {
                tailer.stop();
            }
            if (logHandler != null) {
                logHandler.shutdown();
            }
            closeSockets();

            isMonitoring = false;
            System.out.println("Ngừng thu thập dữ liệu");

        } catch (Exception e) {
            System.err.println("Lỗi! " + e.getMessage());
        }
    }

    private void closeSockets() {
        try {
            if (screenSocket != null && !screenSocket.isClosed()) {
                screenSocket.close();
                System.out.println("Đã đóng kết nối màn hình");
            }
        } catch (Exception e) {
            System.err.println("Lỗi! " + e.getMessage());
        }

        try {
            if (chatSocket != null && !chatSocket.isClosed()) {
                chatSocket.close();
                System.out.println("Đã đóng kết nối chat");
            }
        } catch (Exception e) {
            System.err.println("Lỗi! " + e.getMessage());
        }
        try {
            if (controlSocket != null && !controlSocket.isClosed()) {
                controlSocket.close();
                System.out.println("Đã đóng kết nối điều khiển");
            }
        } catch (Exception e) {
            System.err.println("Lỗi! " + e.getMessage());
        }
    }

    public boolean isMonitoring() {
        return isMonitoring;
    }
}
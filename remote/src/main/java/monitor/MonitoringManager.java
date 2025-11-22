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
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public synchronized void stopMonitoring() {
        if (!isMonitoring) {
            return;
        }

        try {

            if (tailer != null) {
                tailer.stop();
            }
            if (logHandler != null) {
                logHandler.shutdown();
            }
            closeSockets();

            isMonitoring = false;

        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    private void closeSockets() {
        try {
            if (screenSocket != null && !screenSocket.isClosed()) {
                screenSocket.close();
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }

        try {
            if (chatSocket != null && !chatSocket.isClosed()) {
                chatSocket.close();
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
        try {
            if (controlSocket != null && !controlSocket.isClosed()) {
                controlSocket.close();
            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
        }
    }

    public boolean isMonitoring() {
        return isMonitoring;
    }
}
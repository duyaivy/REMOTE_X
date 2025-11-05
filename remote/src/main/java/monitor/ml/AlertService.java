package monitor.ml;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

public class AlertService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public void showAlert(Map<String, Object> rawFeatures, AnomalyDetector.AnomalyResult result) {
        if (!result.isAnomaly()) {
            return;
        }

        String severity = result.getSeverity();
        printAlertToConsole(rawFeatures, result);
        String msg = buildAlertMessage(rawFeatures, result);
        JFrame frame = new JFrame("Cảnh báo");
        JOptionPane.showMessageDialog(frame,
                msg,
                "Cảnh báo - " + severity,
                JOptionPane.WARNING_MESSAGE);
    }

    private String buildAlertMessage(Map<String, Object> rawFeatures, AnomalyDetector.AnomalyResult result) {
        StringBuilder msg = new StringBuilder();

        msg.append("⚠️ PHÁT HIỆN HOẠT ĐỘNG BẤT THƯỜNG!\n\n");
        msg.append("Mức độ: ").append(result.getSeverity()).append("\n");
        msg.append("Điểm: ").append(String.format("%.4f", result.getScore())).append("\n");
        msg.append("Độ tin cậy: ").append(String.format("%.1f%%", result.getConfidence() * 100)).append("\n");
        msg.append("Mức độ rủi ro: ").append(result.getRiskLevel()).append("/10\n\n");

        // Process info
        String processName = (String) rawFeatures.get("process_name");
        if (processName != null) {
            msg.append("Process: ").append(processName).append("\n");
        }

        String userName = (String) rawFeatures.get("user");
        if (userName != null) {
            msg.append("User: ").append(userName).append("\n");
        }

        // Command line
        String cmdLine = (String) rawFeatures.get("command_line");
        if (cmdLine != null && !cmdLine.isEmpty()) {
            if (cmdLine.length() > 100) {
                cmdLine = cmdLine.substring(0, 97) + "...";
            }
            msg.append("Command: ").append(cmdLine).append("\n");
        }

        msg.append("\nThời gian: ").append(LocalDateTime.now().format(TIME_FORMAT)).append("\n");

        if (isDisconnect(result.getSeverity())) {
            msg.append("\n⚠️ Kết nối sẽ BỊ NGẮT ngay!");
        } else {
            msg.append("\nℹ️ Cảnh báo sẽ tự đóng sau 10 giây");
        }

        return msg.toString();
    }

    private boolean isDisconnect(String severity) {
        return severity.equals("CRITICAL") || severity.equals("HIGH");
    }

    private void printAlertToConsole(Map<String, Object> rawFeatures, AnomalyDetector.AnomalyResult result) {
        String severity = result.getSeverity();

        System.out.println("\n" + "═".repeat(100));
        System.out.println(String.format("🚨 %s SEVERITY ANOMALY DETECTED", severity));
        System.out.println("═".repeat(100));
        System.out.println("⏰ Time:        " + LocalDateTime.now().format(TIME_FORMAT));
        System.out.println(String.format("📊 Score:       %.4f (Risk: %d/10)",
                result.getScore(), result.getRiskLevel()));

        // Process info
        String processName = (String) rawFeatures.get("process_name");
        if (processName != null) {
            System.out.println("  • Process:      " + processName);
        }

        String userName = (String) rawFeatures.get("user");
        if (userName != null) {
            System.out.println("  • User:         " + userName);
        }

        Object eventCodeObj = rawFeatures.get("event_code");
        if (eventCodeObj != null) {
            System.out.println("  • Event Code:   " + eventCodeObj.toString());
        }
        String cmdLine = (String) rawFeatures.get("command_line");
        if (cmdLine != null && !cmdLine.isEmpty()) {
            if (cmdLine.length() > 80) {
                cmdLine = cmdLine.substring(0, 77) + "...";
            }
            System.out.println("  • Command:      " + cmdLine);
        }
        Object destPortObj = rawFeatures.get("dest_port");
        if (destPortObj != null) {
            try {
                int destPort = destPortObj instanceof Integer ? (Integer) destPortObj
                        : Integer.parseInt(destPortObj.toString());
                if (destPort > 0) {
                    System.out.println("  • Dest Port:    " + destPort);
                }
            } catch (Exception e) {

            }
        }
    }

}
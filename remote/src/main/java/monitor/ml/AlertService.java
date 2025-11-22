package monitor.ml;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

public class AlertService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private Preprocessor preprocessor;

    public AlertService(Preprocessor preprocessor) {
        this.preprocessor = preprocessor;
    }

    public void showAlert(Map<String, Object> rawFeatures, AnomalyDetector.AnomalyResult result) {
        if (!result.isAnomaly()) {
            return;
        }

        String processName = (String) rawFeatures.get("process_name");
        String severity = result.getSeverity();

        if (preprocessor.isWhitelisted(processName) &&
                !severity.equals("HIGH") && !severity.equals("CRITICAL")) {
            return;
        }

        SwingUtilities.invokeLater(() -> {
            String msg = buildAlertMessage(rawFeatures, result);
            JOptionPane.showMessageDialog(
                    null,
                    msg,
                    "Cảnh báo - " + severity,
                    JOptionPane.WARNING_MESSAGE);
        });

    }

    private String buildAlertMessage(Map<String, Object> rawFeatures, AnomalyDetector.AnomalyResult result) {

        int risk = result.getRiskLevel();
        final String processName = Objects.toString(rawFeatures.get("process_name"), "-");
        final String userName = Objects.toString(rawFeatures.get("user"), "-");

        String cmdLine = Objects.toString(rawFeatures.get("command_line"), "-");
        if (cmdLine.length() > 100) {
            cmdLine = cmdLine.substring(0, 97) + "...";
        }

        StringBuilder msg = new StringBuilder(256);

        msg.append("PHÁT HIỆN HOẠT ĐỘNG BẤT THƯỜNG!\n\n");
        msg.append("Mức độ: ").append(result.getSeverity()).append("\n");
        msg.append("Mức độ rủi ro: ").append(risk).append("/10\n");
        msg.append("Process: ").append(processName).append("\n");
        msg.append("User: ").append(userName).append("\n");
        msg.append("Command: ").append(cmdLine).append("\n");

        msg.append("Thời gian: ").append(LocalDateTime.now().format(TIME_FORMAT)).append("\n");

        if (isDisconnect(result.getSeverity())) {
            msg.append("\nNgắt kết nối!");
        }

        return msg.toString();
    }

    private boolean isDisconnect(String severity) {
        return severity.equals("CRITICAL") || severity.equals("HIGH");
    }

}
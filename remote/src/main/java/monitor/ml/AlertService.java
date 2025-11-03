
package monitor.ml;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service để hiển thị và quản lý alerts khi phát hiện anomaly
 */
public class AlertService {

    // ANSI Color Codes
    private static final String ANSI_RESET = "\u001B[0m";
    private static final String ANSI_RED_BOLD = "\u001B[1;31m";
    private static final String ANSI_YELLOW_BOLD = "\u001B[1;33m";
    private static final String ANSI_CYAN_BOLD = "\u001B[1;36m";
    private static final String ANSI_WHITE_BOLD = "\u001B[1;37m";

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AtomicInteger totalAlerts = new AtomicInteger(0);
    private final AtomicInteger criticalAlerts = new AtomicInteger(0);
    private final AtomicInteger highSeverityAlerts = new AtomicInteger(0);
    private final AtomicInteger mediumSeverityAlerts = new AtomicInteger(0);
    private final AtomicInteger lowSeverityAlerts = new AtomicInteger(0);

    // Flag để bật/tắt alerts
    private boolean alertsEnabled = true;

    // Minimum severity level để hiển thị (LOW, MEDIUM, HIGH, CRITICAL)
    private String minSeverity = "LOW";

    /**
     * Hiển thị alert khi phát hiện anomaly
     * 
     * @param rawFeatures Raw features từ event
     * @param result      Kết quả dự đoán
     */
    public void showAlert(Map<String, Object> rawFeatures, AnomalyDetector.AnomalyResult result) {
        if (!alertsEnabled) {
            return;
        }

        if (!result.isAnomaly()) {
            return;
        }

        String severity = result.getSeverity();

        // Filter theo severity
        if (!shouldShowSeverity(severity)) {
            return;
        }

        // Update counters
        totalAlerts.incrementAndGet();
        switch (severity) {
            case "CRITICAL":
                criticalAlerts.incrementAndGet();
                break;
            case "HIGH":
                highSeverityAlerts.incrementAndGet();
                break;
            case "MEDIUM":
                mediumSeverityAlerts.incrementAndGet();
                break;
            case "LOW":
                lowSeverityAlerts.incrementAndGet();
                break;
        }

        // Print alert
        printAlert(rawFeatures, result);
    }

    /**
     * In ra alert với format đẹp
     */
    private void printAlert(Map<String, Object> rawFeatures, AnomalyDetector.AnomalyResult result) {
        String severity = result.getSeverity();
        String severityIcon = getSeverityIcon(severity);
        String severityColor = getSeverityColor(severity);

        System.out.println("\n" + "═".repeat(100));
        System.out.println(severityColor + severityIcon + " ANOMALY DETECTED - " + severity + " SEVERITY" + ANSI_RESET);
        System.out.println("═".repeat(100));

        // Time
        System.out.println("⏰ Time:        " + LocalDateTime.now().format(TIME_FORMAT));

        // Score, Confidence & Risk
        System.out.println(String.format("📊 Score:       %.4f (Confidence: %d%%, Risk: %d/10)",
                result.getScore(), result.getConfidence(), result.getRiskLevel()));

        // Event details
        System.out.println("\n📋 Event Details:");
        System.out.println("  • Event Code:   " + rawFeatures.get("event_code"));
        System.out.println("  • User:         " + rawFeatures.get("user"));
        System.out.println("  • Process:      " + rawFeatures.get("process_name"));
        System.out.println("  • Parent:       " + rawFeatures.get("parent_name"));

        // Command line (truncate nếu quá dài)
        String cmdLine = (String) rawFeatures.get("command_line");
        if (cmdLine != null && !cmdLine.isEmpty()) {
            if (cmdLine.length() > 80) {
                cmdLine = cmdLine.substring(0, 77) + "...";
            }
            System.out.println("  • Command:      " + cmdLine);
        }

        // Dest port nếu có
        int destPort = (Integer) rawFeatures.get("dest_port");
        if (destPort > 0) {
            System.out.println("  • Dest Port:    " + destPort);
        }

        // Recommendations
        System.out.println("\n💡 Recommended Actions:");
        printRecommendations(severity, rawFeatures);

        System.out.println("═".repeat(100) + "\n");
    }

    /**
     * In ra khuyến nghị dựa trên severity
     */
    private void printRecommendations(String severity, Map<String, Object> rawFeatures) {
        switch (severity) {
            case "CRITICAL":
                System.out.println("  🔴 1. IMMEDIATE ACTION REQUIRED - Critical threat detected!");
                System.out.println("  🔴 2. Isolate the system from network immediately");
                System.out.println("  🔴 3. Terminate suspicious process if safe to do so");
                System.out.println("  🔴 4. Capture memory dump for forensics");
                System.out.println("  🔴 5. Escalate to security team");
                break;

            case "HIGH":
                System.out.println("  🟠 1. INVESTIGATE IMMEDIATELY - High risk activity detected");
                System.out.println("  🟠 2. Check process details and command line");
                System.out.println("  🟠 3. Verify user legitimacy and authentication");
                System.out.println("  🟠 4. Review network connections and file access");
                System.out.println("  🟠 5. Consider system quarantine");
                break;

            case "MEDIUM":
                System.out.println("  🟡 1. Review activity logs for this process");
                System.out.println("  🟡 2. Check if this is expected behavior");
                System.out.println("  🟡 3. Monitor for additional suspicious events");
                System.out.println("  🟡 4. Document findings for analysis");
                break;

            case "LOW":
                System.out.println("  🟢 1. Log for future reference");
                System.out.println("  🟢 2. Review if pattern persists");
                System.out.println("  🟢 3. Update baseline if legitimate");
                break;
        }
    }

    private boolean shouldShowSeverity(String severity) {
        switch (minSeverity) {
            case "CRITICAL":
                return severity.equals("CRITICAL");
            case "HIGH":
                return severity.equals("CRITICAL") || severity.equals("HIGH");
            case "MEDIUM":
                return !severity.equals("LOW");
            case "LOW":
                return true; // Show all
            default:
                return true;
        }
    }

    /**
     * Get icon cho severity level
     */
    private String getSeverityIcon(String severity) {
        switch (severity) {
            case "CRITICAL":
                return "🚨";
            case "HIGH":
                return "⚠️";
            case "MEDIUM":
                return "⚡";
            case "LOW":
                return "ℹ️";
            default:
                return "❓";
        }
    }

    /**
     * Get ANSI color code cho severity
     */
    private String getSeverityColor(String severity) {
        switch (severity) {
            case "CRITICAL":
                return ANSI_RED_BOLD;
            case "HIGH":
                return ANSI_RED_BOLD;
            case "MEDIUM":
                return ANSI_YELLOW_BOLD;
            case "LOW":
                return ANSI_CYAN_BOLD;
            default:
                return ANSI_RESET;
        }
    }

    /**
     * In ra statistics về alerts
     */
    public void printStatistics() {
        System.out.println("\n" + "═".repeat(100));
        System.out.println("📊 ANOMALY DETECTION STATISTICS");
        System.out.println("═".repeat(100));
        System.out.println("Total Alerts:    " + totalAlerts.get());
        System.out.println("  🔴 Critical:   " + criticalAlerts.get());
        System.out.println("  🟠 High:       " + highSeverityAlerts.get());
        System.out.println("  🟡 Medium:     " + mediumSeverityAlerts.get());
        System.out.println("  🟢 Low:        " + lowSeverityAlerts.get());
        System.out.println("═".repeat(100) + "\n");
    }

    /**
     * Set minimum severity level để hiển thị
     */
    public void setMinSeverity(String severity) {
        this.minSeverity = severity;
    }

    /**
     * Enable/disable alerts
     */
    public void setAlertsEnabled(boolean enabled) {
        this.alertsEnabled = enabled;
    }

    /**
     * Get total alerts count
     */
    public int getTotalAlerts() {
        return totalAlerts.get();
    }
}
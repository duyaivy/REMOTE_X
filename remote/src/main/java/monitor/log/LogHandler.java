package monitor.log;

import monitor.config.AgentConfig;
import monitor.ml.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * Xử lý và lưu trữ log events với ML anomaly detection
 */
public class LogHandler {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    // ML Components
    private Preprocessor preprocessor;
    private AnomalyDetector detector;
    private AlertService alertService;
    private boolean mlEnabled = false;

    public LogHandler() {
        // Khởi tạo ML components
        initializeML();
    }

    /**
     * Khởi tạo ML components
     */
    private void initializeML() {
        try {
            System.out.println("\n[ML] Initializing ML components...");

            // 1. Load preprocessing artifacts
            preprocessor = new Preprocessor();
            preprocessor.loadArtifacts();

            // 2. Initialize anomaly detector
            detector = new AnomalyDetector();
            detector.initialize();

            // 3. Initialize alert service
            alertService = new AlertService();

            mlEnabled = true;
            System.out.println("[ML] ✓ ML components ready!\n");

        } catch (Exception e) {
            System.err.println("[ML] ⚠️  Failed to initialize ML: " + e.getMessage());
            System.err.println("[ML] Agent will continue without anomaly detection.");
            e.printStackTrace();
            mlEnabled = false;
        }
    }

    /**
     * Xử lý log event
     */
    public void handleLog(String jsonLine) {
        try {
            JsonNode root = MAPPER.readTree(jsonLine);
            String timestamp = extractTimestamp(root);
            // String humanReadableTime = formatTimestamp(timestamp);

            // // In ra console (bỏ qua nếu muốn giảm noise)
            // System.out.println("[" + humanReadableTime + "] " + jsonLine);

            // ML Detection (nếu enabled)
            if (mlEnabled) {
                performAnomalyDetection(jsonLine);
            }
            // Lưu vào file
            saveToFile(jsonLine);

        } catch (Exception e) {
            System.err.println("[PARSE] Lỗi xử lý log: " + e.getMessage());
        }
    }

    /**
     * Thực hiện anomaly detection
     */
    private void performAnomalyDetection(String jsonLine) {
        try {
            // 1. Extract features
            Map<String, Object> rawFeatures = FeatureExtractor.extractFeatures(jsonLine);

            // 2. Preprocess
            float[] features = preprocessor.preprocess(rawFeatures);

            // 3. Predict
            AnomalyDetector.AnomalyResult result = detector.predict(features);

            // 4. Log kết quả detection (cả normal và anomaly)
            if (result.isAnomaly()) {
                System.out.println("[ML DETECTION] ⚠️  ANOMALY detected - Score: " +
                        String.format("%.4f", result.getScore()));
                alertService.showAlert(rawFeatures, result);
            } else {
                // System.out.println("[ML DETECTION] ✓ NORMAL - Score: " +
                // String.format("%.4f", result.getScore()));
            }

        } catch (Exception e) {
            // Silent fail - không làm gián đoạn logging
            // Chỉ log error nếu debug mode
            System.err.println("[ML] Detection error: " + e.getMessage());
        }
    }

    private String extractTimestamp(JsonNode root) {
        JsonNode timestampNode = root.get("@timestamp");
        return (timestampNode != null && !timestampNode.isNull())
                ? timestampNode.asText()
                : null;
    }

    private String formatTimestamp(String isoTimestamp) {
        try {
            if (isoTimestamp != null && !isoTimestamp.isEmpty()) {
                return TIMESTAMP_FORMAT.format(Instant.parse(isoTimestamp));
            }
        } catch (Exception e) {
            // Fallback to current time
        }
        return TIMESTAMP_FORMAT.format(Instant.now());
    }

    private void saveToFile(String jsonLine) throws IOException {
        Files.writeString(
                AgentConfig.AGENT_LOG_FILE,
                jsonLine + System.lineSeparator(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND);
    }

    /**
     * Đọc logs đã lưu trước đó
     */
    public void loadPreviousLogs() {
        if (Files.exists(AgentConfig.AGENT_LOG_FILE)) {
            try {
                long lineCount = Files.lines(AgentConfig.AGENT_LOG_FILE).count();
                System.out.println("[LOG] Đã tải " + lineCount + " dòng log cũ.");
            } catch (IOException e) {
                System.err.println("[LOG] Lỗi đọc log cũ: " + e.getMessage());
            }
        }
    }

    /**
     * Shutdown ML components và print statistics
     */
    public void shutdown() {
        if (detector != null) {
            detector.close();
        }
        if (alertService != null && mlEnabled) {
            alertService.printStatistics();
        }
    }
}
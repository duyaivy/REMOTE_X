package monitor.log;

import monitor.ml.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;

public class LogHandler {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ML Components
    private Preprocessor preprocessor;
    private AnomalyDetector detector;
    private AlertService alertService;
    private boolean mlEnabled = false;

    public LogHandler(AlertService alertService) {
        this.alertService = alertService;
        initializeML();
    }

    /**
     * Khởi tạo ML components
     */
    private void initializeML() {
        try {
            System.out.println("Load ML");

            preprocessor = new Preprocessor();
            preprocessor.loadArtifacts();

            // Initialize anomaly detector
            detector = new AnomalyDetector();
            detector.initialize();

            mlEnabled = true;
            System.out.println("[ML] ✓ ML components ready!\n");

        } catch (Exception e) {
            System.err.println("Error load ML " + e.getMessage());
            mlEnabled = false;
        }
    }

    /**
     * Xử lý log event
     */
    public void handleLog(String jsonLine) {
        try {

            if (mlEnabled) {
                performAnomalyDetection(jsonLine);
            }

        } catch (Exception e) {
            System.err.println("[PARSE] Lỗi xử lý log: " + e.getMessage());
        }
    }

    private void performAnomalyDetection(String jsonLine) {
        try {
            // Extract features
            Map<String, Object> rawFeatures = FeatureExtractor.extractFeatures(jsonLine);

            // Preprocess
            float[] features = preprocessor.preprocess(rawFeatures);

            // Predict
            AnomalyDetector.AnomalyResult result = detector.predict(features);

            // Nếu phát hiện anomaly, gửi alert
            if (result.isAnomaly()) {
                System.out.println("[ML] ⚠️  ANOMALY detected - Score: " +
                        String.format("%.4f", result.getScore()));
                alertService.showAlert(rawFeatures, result);
            }

        } catch (Exception e) {
            System.err.println("[ML] Detection error: " + e.getMessage());
        }
    }

    public void shutdown() {
        if (detector != null) {
            detector.close();
        }
    }
}
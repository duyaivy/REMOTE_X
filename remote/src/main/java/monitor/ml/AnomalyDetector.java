package monitor.ml;

import ai.onnxruntime.*;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Map;

/**
 * Anomaly Detector sử dụng ONNX Isolation Forest model
 * 
 * Workflow:
 * 1. Load ONNX model khi khởi động
 * 2. Nhận feature vector đã preprocessed (69 features)
 * 3. Predict: -1 = anomaly, 1 = normal
 * 4. Trả về kết quả và score
 * 
 * Theo onnx_metadata.json:
 * - Input name: "float_input"
 * - Output names: ["label", "scores"]
 * - Thresholds: critical < -0.5, warning < 0.0
 */
public class AnomalyDetector {

    private OrtEnvironment env;
    private OrtSession session;
    private boolean isLoaded = false;

    // Thresholds từ model metadata
    private static final float ANOMALY_THRESHOLD_CRITICAL = -0.5f;
    private static final float ANOMALY_THRESHOLD_WARNING = 0.0f;

    /**
     * Khởi tạo và load ONNX model
     */
    public void initialize() throws Exception {
        System.out.println("\n[ANOMALY DETECTOR] Initializing...");

        // Create ONNX environment
        env = OrtEnvironment.getEnvironment();
        Path tempModel = Files.createTempFile("isolation_forest_model", ".onnx");
        try (InputStream is = getClass().getResourceAsStream("/isolation_forest_model.onnx")) {
            if (is == null) {
                throw new IllegalArgumentException("not found: /isolation_forest_model.onnx");
            }
            Files.copy(is, tempModel, StandardCopyOption.REPLACE_EXISTING);
        }

        // Create session
        OrtSession.SessionOptions options = new OrtSession.SessionOptions();
        session = env.createSession(tempModel.toString(), options);

        // Delete temp file khi JVM shutdown
        tempModel.toFile().deleteOnExit();

        System.out.println("✓");

        isLoaded = true;
        System.out.println("\n✓ Anomaly Detector ready!\n");
    }

    /**
     * Predict anomaly cho một event
     * 
     * @param features Feature vector (69 features đã scaled)
     * @return AnomalyResult chứa prediction và score
     */
    public AnomalyResult predict(float[] features) throws OrtException {
        if (!isLoaded) {
            throw new IllegalStateException("Model chưa được load! Gọi initialize() trước.");
        }

        float[][] input2D = new float[][] { features };
        OnnxTensor inputTensor = OnnxTensor.createTensor(env, input2D);

        Map<String, OnnxTensor> inputs = Collections.singletonMap("float_input", inputTensor);
        OrtSession.Result result = session.run(inputs);

        long[][] labels = (long[][]) result.get(0).getValue();
        float[][] scores = (float[][]) result.get(1).getValue();

        // Cleanup
        inputTensor.close();
        result.close();

        // Parse results
        boolean isAnomaly = labels[0][0] == -1;
        float score = scores[0][0];

        return new AnomalyResult(isAnomaly, score);
    }

    public void close() {
        try {
            if (session != null) {
                session.close();
            }
            if (env != null) {
                env.close();
            }
            System.out.println("[ANOMALY DETECTOR] Closed.");
        } catch (Exception e) {
            System.err.println("[ANOMALY DETECTOR] Error closing: " + e.getMessage());
        }
    }

    public static class AnomalyResult {
        private final boolean isAnomaly;
        private final float score;

        public AnomalyResult(boolean isAnomaly, float score) {
            this.isAnomaly = isAnomaly;
            this.score = score;
        }

        public boolean isAnomaly() {
            return isAnomaly;
        }

        public float getScore() {
            return score;
        }

        /**
         * Get severity level dựa trên score
         * 
         * Theo metadata:
         * - critical < -0.5
         * - warning < 0.0
         * - normal >= 0.0
         * 
         * @return "CRITICAL", "HIGH", "MEDIUM", "LOW", or "NORMAL"
         */
        public String getSeverity() {
            if (!isAnomaly) {
                return "NORMAL";
            }

            if (score < ANOMALY_THRESHOLD_CRITICAL) {

                return "CRITICAL";

            } else if (score < -0.2) {
                return "HIGH";
            } else if (score < -0.1) {
                return "MEDIUM";
            } else {
                return "LOW";
            }
        }

        /**
         * Get confidence percentage (0-100)
         * Score càng âm = confidence càng cao
         */
        public int getConfidence() {
            if (!isAnomaly) {
                return 0;
            }

            // Map score range [-1.0, 0.0] to confidence [0, 100]
            float normalized = Math.max(-1.0f, Math.min(0.0f, score));
            return (int) (Math.abs(normalized) * 100);
        }

        /**
         * Get risk level (0-10)
         */
        public int getRiskLevel() {
            if (!isAnomaly) {
                return 0;
            }

            if (score < -0.5)
                return 10; // CRITICAL
            if (score < -0.4)
                return 9;
            if (score < -0.3)
                return 8; // HIGH
            if (score < -0.2)
                return 7;
            if (score < -0.1)
                return 6; // MEDIUM
            if (score < -0.05)
                return 5;
            return 4; // LOW
        }

        @Override
        public String toString() {
            return String.format(
                    "AnomalyResult{isAnomaly=%b, score=%.4f, severity=%s, confidence=%d%%, risk=%d/10}",
                    isAnomaly, score, getSeverity(), getConfidence(), getRiskLevel());
        }
    }
}
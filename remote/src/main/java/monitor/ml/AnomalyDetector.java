package monitor.ml;

import ai.onnxruntime.*;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Map;

public class AnomalyDetector {

    private OrtEnvironment env;
    private OrtSession session;
    private boolean isLoaded = false;

    private static final float THRESHOLD_CRITICAL = -0.035f;
    private static final float THRESHOLD_HIGH = -0.025f;
    private static final float THRESHOLD_MEDIUM = -0.015f;
    private static final float THRESHOLD_LOW = -0.005f;

    public void initialize() throws Exception {
        env = OrtEnvironment.getEnvironment();
        Path tempModel = Files.createTempFile("isolation_forest_model", ".onnx");
        try (InputStream is = getClass().getResourceAsStream("/isolation_forest_model.onnx")) {
            if (is == null) {
                throw new IllegalArgumentException("Không tìm thấy: /isolation_forest_model.onnx");
            }
            Files.copy(is, tempModel, StandardCopyOption.REPLACE_EXISTING);
        }

        OrtSession.SessionOptions options = new OrtSession.SessionOptions();
        session = env.createSession(tempModel.toString(), options);
        tempModel.toFile().deleteOnExit();

        isLoaded = true;
    }

    public AnomalyResult predict(float[] features) throws OrtException {
        if (!isLoaded) {
            throw new IllegalStateException("Model chưa được load!");
        }

        float[][] input2D = new float[][] { features };
        OnnxTensor inputTensor = OnnxTensor.createTensor(env, input2D);

        Map<String, OnnxTensor> inputs = Collections.singletonMap("float_input", inputTensor);
        OrtSession.Result result = session.run(inputs);

        long[][] labels = (long[][]) result.get(0).getValue();
        float[][] scores = (float[][]) result.get(1).getValue();

        inputTensor.close();
        result.close();

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
            System.out.println("Đóng kiểm tra bất thường.");
        } catch (Exception e) {
            System.err.println("Lỗi! " + e.getMessage());
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

        public String getSeverity() {
            if (!isAnomaly) {
                return "NORMAL";
            }

            if (score < THRESHOLD_CRITICAL) {
                return "CRITICAL";
            } else if (score < THRESHOLD_HIGH) {
                return "HIGH";
            } else if (score < THRESHOLD_MEDIUM) {
                return "MEDIUM";
            } else if (score < THRESHOLD_LOW) {
                return "LOW";
            } else {
                return "SUSPICIOUS";
            }
        }

        public int getConfidence() {
            if (!isAnomaly) {
                return 0;
            }

            float normalized = Math.max(-1.0f, Math.min(0.0f, score));
            return (int) (Math.abs(normalized) * 100);
        }

        public int getRiskLevel() {
            if (!isAnomaly) {
                return 0;
            }

            if (score < -0.042)
                return 10;
            if (score < -0.038)
                return 9;
            if (score < -0.035)
                return 8;
            if (score < -0.030)
                return 7;
            if (score < -0.025)
                return 6;
            if (score < -0.020)
                return 5;
            if (score < -0.015)
                return 4;
            if (score < -0.010)
                return 3;
            if (score < -0.005)
                return 2;
            return 1;
        }

        @Override
        public String toString() {
            return String.format(
                    "AnomalyResult{isAnomaly=%b, score=%.4f, severity=%s, confidence=%d%%, risk=%d/10}",
                    isAnomaly, score, getSeverity(), getConfidence(), getRiskLevel());
        }
    }
}
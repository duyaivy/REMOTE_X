package monitor.util;

import ai.onnxruntime.*;
import java.util.*;

public class OnnxModelLoader {
    private static OrtEnvironment env;
    private static OrtSession session;

    public static void init() throws Exception {
        env = OrtEnvironment.getEnvironment();
        String modelPath = "src/main/resources/models/isolation_forest_model.onnx";
        session = env.createSession(modelPath, new OrtSession.SessionOptions());
        System.out.println("✅ ONNX model loaded successfully!");
    }

    public static float[] predict(float[] inputVector) throws Exception {

        OnnxTensor inputTensor = OnnxTensor.createTensor(env, new float[][] { inputVector });
        OrtSession.Result result = session.run(Collections.singletonMap("input", inputTensor));
        float[][] output = (float[][]) result.get(0).getValue();
        return output[0];
    }
}

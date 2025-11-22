package monitor.ml;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * Trích xuất raw features từ Sysmon JSON events
 */
public class FeatureExtractor {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static Map<String, Object> extractFeatures(String jsonLine) throws Exception {
        JsonNode root = MAPPER.readTree(jsonLine);

        Map<String, Object> features = new HashMap<>();

        JsonNode winlog = root.path("winlog");
        JsonNode eventData = winlog.path("event_data");

        int eventCode = winlog.path("event_id").asInt(0);
        features.put("event_code", eventCode);

        String timestamp = root.path("@timestamp").asText("");
        features.put("timestamp", timestamp);

        String user = eventData.path("User").asText("Unknown");
        features.put("user", user);

        String imagePath = eventData.path("Image").asText("");
        features.put("image_path", imagePath);

        String processName = extractProcessName(imagePath);
        features.put("process_name", processName);

        String parentPath = eventData.path("ParentImage").asText("");
        features.put("parent_image", parentPath);

        String parentName = extractProcessName(parentPath);
        if (parentName.isEmpty()) {
            parentName = "-";
        }
        features.put("parent_name", parentName);

        String commandLine = eventData.path("CommandLine").asText("");
        features.put("command_line", commandLine);
        int destPort = eventData.path("DestinationPort").asInt(0);
        features.put("dest_port", destPort);

        return features;
    }

    private static String extractProcessName(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            return "";
        }

        String filename = new File(imagePath).getName();
        return filename.toLowerCase();
    }

    public static void printFeatures(Map<String, Object> features) {
        System.out.println("\n[FEATURES] Extracted:");
        for (Map.Entry<String, Object> entry : features.entrySet()) {
            System.out.println("  • " + entry.getKey() + ": " + entry.getValue());
        }
    }
}
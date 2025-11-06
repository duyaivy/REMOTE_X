package monitor.ml;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Trích xuất raw features từ Sysmon JSON events
 */
public class FeatureExtractor {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Extract tất cả raw features từ một Sysmon event JSON
     * 
     * @param jsonLine NDJSON line từ Winlogbeat
     * @return Map chứa các raw features
     * @throws Exception nếu parse JSON thất bại
     */
    public static Map<String, Object> extractFeatures(String jsonLine) throws Exception {
        JsonNode root = MAPPER.readTree(jsonLine);

        Map<String, Object> features = new HashMap<>();

        JsonNode winlog = root.path("winlog");
        JsonNode eventData = winlog.path("event_data");

        // 1. Event Code (Sysmon Event ID)
        int eventCode = winlog.path("event_id").asInt(0);
        features.put("event_code", eventCode);

        // 2. Timestamp
        String timestamp = root.path("@timestamp").asText("");
        features.put("timestamp", timestamp);

        // 3. User
        String user = eventData.path("User").asText("Unknown");
        features.put("user", user);

        // 4. Image Path (FULL PATH for TF-IDF)
        String imagePath = eventData.path("Image").asText("");
        features.put("image_path", imagePath);

        // 5. Process Name (filename only for rules)
        String processName = extractProcessName(imagePath);
        features.put("process_name", processName);

        // 6. Parent Image Path (FULL PATH for TF-IDF)
        String parentPath = eventData.path("ParentImage").asText("");
        features.put("parent_image", parentPath);

        // 7. Parent Process Name (filename only for rules)
        String parentName = extractProcessName(parentPath);
        if (parentName.isEmpty()) {
            parentName = "-";
        }
        features.put("parent_name", parentName);

        // 8. Command Line
        String commandLine = eventData.path("CommandLine").asText("");
        features.put("command_line", commandLine);

        // 9. Destination Port (chỉ có trong Event ID 3 - Network Connection)
        int destPort = eventData.path("DestinationPort").asInt(0);
        features.put("dest_port", destPort);

        return features;
    }

    /**
     * Extract process name từ full path
     * Ví dụ: "C:\\Windows\\System32\\cmd.exe" -> "cmd.exe"
     */
    private static String extractProcessName(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            return "";
        }

        // Get filename from path
        String filename = new File(imagePath).getName();

        // Convert to lowercase
        return filename.toLowerCase();
    }

    /**
     * In ra features cho debugging
     */
    public static void printFeatures(Map<String, Object> features) {
        System.out.println("\n[FEATURES] Extracted:");
        for (Map.Entry<String, Object> entry : features.entrySet()) {
            System.out.println("  • " + entry.getKey() + ": " + entry.getValue());
        }
    }
}
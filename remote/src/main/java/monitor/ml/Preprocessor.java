package monitor.ml;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class Preprocessor {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // Label Encoders
    private Map<String, List<String>> labelEncoders;

    // TF-IDF components
    private Map<String, Integer> tfidfImageVocab;
    private double[] tfidfImageIdf;

    private Map<String, Integer> tfidfCmdlineVocab;
    private double[] tfidfCmdlineIdf;

    // StandardScaler
    private double[] scalerMean;
    private double[] scalerScale;

    // Suspicious patterns (từ feature_metadata.json)
    private Set<String> suspiciousProcesses;
    private List<String> suspiciousCmdlinePatterns;
    private List<String[]> suspiciousParentChild;

    // WHITELIST - Processes đáng tin cậy (skip LOW severity alerts)
    private Set<String> whitelistedProcesses;

    // Frequency tracking (simple cache)
    private Map<String, Integer> userEventCounter = new HashMap<>();
    private Map<String, Integer> processFrequencyCounter = new HashMap<>();

    // Feature metadata
    private int totalFeatures = 69;

    // Regex patterns cho tokenization (MATCH PYTHON EXACTLY)
    private Pattern imagePattern = Pattern.compile("[A-Za-z0-9_]+");
    private Pattern cmdlinePattern = Pattern.compile("\\b\\w+\\b");

    // Pattern cho clean_text (match Python: [^a-z0-9\s\-_\\/:.]')
    private Pattern cleanTextPattern = Pattern.compile("[^a-z0-9\\s\\-_\\\\/:.]");
    private Pattern multiSpacePattern = Pattern.compile("\\s+");

    public void loadArtifacts() throws Exception {
        System.out.println("\n[PREPROCESSOR] Loading artifacts...");

        // Load whitelist first
        initializeWhitelist();

        // 1. Load Feature Metadata (để lấy suspicious patterns)
        System.out.print("  • feature_metadata.json... ");
        JsonNode metadataNode = loadJsonResource("/feature_metadata.json");

        // Load suspicious processes
        suspiciousProcesses = new HashSet<>();
        JsonNode suspProcs = metadataNode.get("suspicious_processes");
        if (suspProcs != null) {
            for (JsonNode proc : suspProcs) {
                suspiciousProcesses.add(proc.asText().toLowerCase());
            }
        }

        // Load suspicious cmdline patterns
        suspiciousCmdlinePatterns = new ArrayList<>();
        JsonNode suspCmds = metadataNode.get("suspicious_cmdline_patterns");
        if (suspCmds != null) {
            for (JsonNode pattern : suspCmds) {
                suspiciousCmdlinePatterns.add(pattern.asText().toLowerCase());
            }
        }

        // Load suspicious parent-child pairs
        suspiciousParentChild = new ArrayList<>();
        JsonNode suspPairs = metadataNode.get("suspicious_parent_child");
        if (suspPairs != null) {
            for (JsonNode pair : suspPairs) {
                String[] pairArray = new String[2];
                pairArray[0] = pair.get(0).asText().toLowerCase();
                pairArray[1] = pair.get(1).asText().toLowerCase();
                suspiciousParentChild.add(pairArray);
            }
        }

        System.out.println("✓");

        // 2. Label Encoders
        System.out.print("  • label_encoders.json... ");
        JsonNode encodersNode = loadJsonResource("/label_encoders.json");
        labelEncoders = new HashMap<>();

        for (Iterator<String> it = encodersNode.fieldNames(); it.hasNext();) {
            String field = it.next();
            JsonNode classes = encodersNode.get(field).get("classes");

            List<String> classList = new ArrayList<>();
            for (JsonNode cls : classes) {
                classList.add(cls.asText());
            }
            labelEncoders.put(field, classList);
        }
        System.out.println("✓ (" + labelEncoders.size() + " encoders)");

        // 3. TF-IDF Image
        System.out.print("  • tfidf_image.json... ");
        JsonNode tfidfImageNode = loadJsonResource("/tfidf_image.json");
        tfidfImageVocab = new HashMap<>();
        JsonNode vocab = tfidfImageNode.get("vocabulary");
        if (vocab != null) {
            for (Iterator<String> it = vocab.fieldNames(); it.hasNext();) {
                String word = it.next();
                tfidfImageVocab.put(word, vocab.get(word).asInt());
            }
        }
        tfidfImageIdf = jsonArrayToDoubleArray(tfidfImageNode.get("idf"));
        System.out.println("✓ (" + tfidfImageVocab.size() + " words)");

        // 4. TF-IDF Command Line
        System.out.print("  • tfidf_cmdline.json... ");
        JsonNode tfidfCmdlineNode = loadJsonResource("/tfidf_cmdline.json");
        tfidfCmdlineVocab = new HashMap<>();
        JsonNode vocabCmd = tfidfCmdlineNode.get("vocabulary");
        if (vocabCmd != null) {
            for (Iterator<String> it = vocabCmd.fieldNames(); it.hasNext();) {
                String word = it.next();
                tfidfCmdlineVocab.put(word, vocabCmd.get(word).asInt());
            }
        }
        tfidfCmdlineIdf = jsonArrayToDoubleArray(tfidfCmdlineNode.get("idf"));
        System.out.println("✓ (" + tfidfCmdlineVocab.size() + " words)");

        // 5. Scaler
        System.out.print("  • scaler.json... ");
        JsonNode scalerNode = loadJsonResource("/scaler.json");
        scalerMean = jsonArrayToDoubleArray(scalerNode.get("mean"));
        scalerScale = jsonArrayToDoubleArray(scalerNode.get("scale"));
        System.out.println("✓ (" + scalerMean.length + " features)");

        System.out.println("\n✓ All preprocessing artifacts loaded!");
        System.out.println("  Whitelisted processes: " + whitelistedProcesses.size() + "\n");
    }

    /**
     * Initialize whitelist với các processes an toàn
     */
    private void initializeWhitelist() {
        whitelistedProcesses = new HashSet<>();

        // Development Tools
        whitelistedProcesses.add("code.exe"); // VS Code
        whitelistedProcesses.add("devenv.exe"); // Visual Studio
        whitelistedProcesses.add("idea64.exe"); // IntelliJ IDEA
        whitelistedProcesses.add("eclipse.exe"); // Eclipse

        // Messaging Apps
        whitelistedProcesses.add("zalo.exe"); // Zalo
        whitelistedProcesses.add("telegram.exe"); // Telegram
        whitelistedProcesses.add("discord.exe"); // Discord
        whitelistedProcesses.add("slack.exe"); // Slack
        whitelistedProcesses.add("teams.exe"); // Microsoft Teams

        // Browsers
        whitelistedProcesses.add("chrome.exe"); // Chrome
        whitelistedProcesses.add("firefox.exe"); // Firefox
        whitelistedProcesses.add("msedge.exe"); // Edge
        whitelistedProcesses.add("opera.exe"); // Opera
        whitelistedProcesses.add("brave.exe"); // Brave

        // Browser Helpers
        whitelistedProcesses.add("identity_helper.exe"); // Edge helper
        whitelistedProcesses.add("crashpad_handler.exe");

        // Git Tools
        whitelistedProcesses.add("bash.exe"); // Git Bash
        whitelistedProcesses.add("sh.exe"); // Shell
        whitelistedProcesses.add("git.exe"); // Git
        whitelistedProcesses.add("cygpath.exe"); // Cygwin path
        whitelistedProcesses.add("sed.exe"); // Stream editor
        whitelistedProcesses.add("awk.exe"); // AWK

        // Development
        whitelistedProcesses.add("python.exe"); // Python
        whitelistedProcesses.add("node.exe"); // Node.js
        whitelistedProcesses.add("java.exe"); // Java
        whitelistedProcesses.add("javaw.exe"); // Java GUI
        whitelistedProcesses.add("npm.exe"); // NPM
        whitelistedProcesses.add("pip.exe"); // PIP

        // System Processes
        whitelistedProcesses.add("nissrv.exe"); // Windows Defender
        whitelistedProcesses.add("svchost.exe"); // Service Host
        whitelistedProcesses.add("explorer.exe"); // Windows Explorer
        whitelistedProcesses.add("searchindexer.exe"); // Windows Search

        System.out.println("  • Whitelist initialized: " + whitelistedProcesses.size() + " processes");
    }

    /**
     * Check if process is whitelisted
     */
    public boolean isWhitelisted(String processName) {
        if (processName == null || processName.isEmpty()) {
            return false;
        }
        return whitelistedProcesses.contains(processName.toLowerCase());
    }

    /**
     * Clean text - MATCH PYTHON EXACTLY
     * Python: text = re.sub(r'[^a-z0-9\s\-_\\/:.]', ' ', text)
     */
    private String cleanText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        // Lowercase
        text = text.toLowerCase();

        // Keep only: a-z 0-9 space - _ \ / : .
        text = cleanTextPattern.matcher(text).replaceAll(" ");

        // Replace multiple spaces with single space
        text = multiSpacePattern.matcher(text).replaceAll(" ");

        return text.trim();
    }

    /**
     * Preprocess raw features thành feature vector cho model
     * 
     * @param rawFeatures
     * @return float[] vector
     */
    public float[] preprocess(Map<String, Object> rawFeatures) {
        float[] features = new float[totalFeatures];
        int idx = 0;

        // Parse timestamp
        String timestamp = (String) rawFeatures.get("timestamp");
        ZonedDateTime zdt = parseTimestamp(timestamp);

        // 1. Event Code
        features[idx++] = ((Integer) rawFeatures.get("event_code")).floatValue();

        // 2. Time Features (9 features)
        int hour = zdt.getHour();
        int weekday = zdt.getDayOfWeek().getValue() - 1; // Monday=0

        features[idx++] = hour; // hour
        features[idx++] = weekday; // weekday
        features[idx++] = (weekday >= 5) ? 1 : 0; // is_weekend
        features[idx++] = (hour < 6 || hour > 22) ? 1 : 0; // is_night (MATCH PYTHON)
        features[idx++] = (hour >= 9 && hour <= 17 && weekday < 5) ? 1 : 0; // is_business_hours
        features[idx++] = (float) Math.sin(2 * Math.PI * hour / 24); // hour_sin
        features[idx++] = (float) Math.cos(2 * Math.PI * hour / 24); // hour_cos
        features[idx++] = (float) Math.sin(2 * Math.PI * weekday / 7); // weekday_sin
        features[idx++] = (float) Math.cos(2 * Math.PI * weekday / 7); // weekday_cos

        // 3. Encoded Features (3 features)
        String user = (String) rawFeatures.get("user");
        String processName = (String) rawFeatures.get("process_name");
        String parentName = (String) rawFeatures.get("parent_name");

        features[idx++] = labelEncode("user", user); // user_encoded
        features[idx++] = labelEncode("process_name", processName); // process_name_encoded
        features[idx++] = labelEncode("parent_name", parentName); // parent_name_encoded

        // 4. Rule-based Features (3 features)
        String cmdLine = (String) rawFeatures.get("command_line");

        features[idx++] = isSuspiciousProcess(processName) ? 1 : 0; // is_suspicious_process
        features[idx++] = countSuspiciousCmdlinePatterns(cmdLine); // suspicious_cmdline_count
        features[idx++] = isSuspiciousParentChild(parentName, processName) ? 1 : 0; // is_suspicious_parent_child

        // 5. Frequency Features (3 features)
        userEventCounter.put(user, userEventCounter.getOrDefault(user, 0) + 1);
        processFrequencyCounter.put(processName, processFrequencyCounter.getOrDefault(processName, 0) + 1);

        features[idx++] = userEventCounter.get(user); // user_event_count
        features[idx++] = processFrequencyCounter.get(processName); // process_frequency
        features[idx++] = ((Integer) rawFeatures.get("dest_port")).floatValue(); // dest_port

        // 6. TF-IDF Image - CLEAN TEXT FIRST (MATCH PYTHON)
        String imagePath = (String) rawFeatures.get("image_path");
        String cleanedImagePath = cleanText(imagePath != null ? imagePath : "");
        double[] tfidfImage = computeTfidf(cleanedImagePath, tfidfImageVocab, tfidfImageIdf, 30, false);
        for (double val : tfidfImage) {
            features[idx++] = (float) val;
        }

        // 7. TF-IDF Command Line - CLEAN TEXT FIRST (MATCH PYTHON)
        String cleanedCmdLine = cleanText(cmdLine != null ? cmdLine : "");
        double[] tfidfCmdline = computeTfidf(cleanedCmdLine, tfidfCmdlineVocab, tfidfCmdlineIdf, 20, true);
        for (double val : tfidfCmdline) {
            features[idx++] = (float) val;
        }

        // 8. Standard Scaling
        for (int i = 0; i < features.length; i++) {
            if (scalerScale[i] != 0) {
                features[i] = (float) ((features[i] - scalerMean[i]) / scalerScale[i]);
            }
        }

        return features;
    }

    /**
     * Parse ISO timestamp
     */
    private ZonedDateTime parseTimestamp(String timestamp) {
        try {
            Instant instant = Instant.parse(timestamp);
            return instant.atZone(ZoneId.systemDefault());
        } catch (Exception e) {
            return ZonedDateTime.now();
        }
    }

    /**
     * Label encode một giá trị
     */
    private float labelEncode(String field, String value) {
        if (value == null || value.isEmpty()) {
            value = "Unknown";
        }

        List<String> classes = labelEncoders.get(field);
        if (classes == null) {
            return 0;
        }

        int index = classes.indexOf(value);
        return (index >= 0) ? index : 0;
    }

    /**
     * Check nếu process là suspicious
     */
    private boolean isSuspiciousProcess(String processName) {
        if (processName == null || processName.isEmpty()) {
            return false;
        }
        return suspiciousProcesses.contains(processName.toLowerCase());
    }

    /**
     * Đếm số lượng suspicious patterns trong command line
     */
    private float countSuspiciousCmdlinePatterns(String cmdLine) {
        if (cmdLine == null || cmdLine.isEmpty()) {
            return 0;
        }

        String cmdLower = cmdLine.toLowerCase();
        int count = 0;

        for (String pattern : suspiciousCmdlinePatterns) {
            if (cmdLower.contains(pattern)) {
                count++;
            }
        }

        return count;
    }

    /**
     * Check nếu parent-child process pair là suspicious
     */
    private boolean isSuspiciousParentChild(String parentName, String childName) {
        if (parentName == null || childName == null) {
            return false;
        }

        String parentLower = parentName.toLowerCase();
        String childLower = childName.toLowerCase();

        for (String[] pair : suspiciousParentChild) {
            if (pair[0].equals(parentLower) && pair[1].equals(childLower)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Tokenize cho IMAGE PATH (match Python: [A-Za-z0-9_]+)
     */
    private List<String> tokenizeImage(String text) {
        List<String> tokens = new ArrayList<>();

        if (text == null || text.isEmpty()) {
            return tokens;
        }

        Matcher matcher = imagePattern.matcher(text);
        while (matcher.find()) {
            tokens.add(matcher.group());
        }

        return tokens;
    }

    /**
     * Tokenize cho COMMAND LINE (match Python: \b\w+\b)
     */
    private List<String> tokenizeCommandLine(String text) {
        List<String> tokens = new ArrayList<>();

        if (text == null || text.isEmpty()) {
            return tokens;
        }

        Matcher matcher = cmdlinePattern.matcher(text);
        while (matcher.find()) {
            tokens.add(matcher.group());
        }

        return tokens;
    }

    /**
     * Compute TF-IDF vector cho một text
     */
    private double[] computeTfidf(String text, Map<String, Integer> vocabulary,
            double[] idf, int maxFeatures, boolean isCommandLine) {
        double[] tfidf = new double[maxFeatures];

        if (text == null || text.isEmpty()) {
            return tfidf;
        }

        // Tokenize với function phù hợp
        List<String> tokens;
        if (isCommandLine) {
            tokens = tokenizeCommandLine(text);
        } else {
            tokens = tokenizeImage(text);
        }

        if (tokens.isEmpty()) {
            return tfidf;
        }

        // Compute TF
        Map<String, Double> tf = new HashMap<>();
        for (String token : tokens) {
            tf.put(token, tf.getOrDefault(token, 0.0) + 1.0);
        }

        // Normalize TF
        double totalTokens = tokens.size();
        if (totalTokens > 0) {
            for (String token : tf.keySet()) {
                tf.put(token, tf.get(token) / totalTokens);
            }
        }

        // Compute TF-IDF
        for (Map.Entry<String, Double> entry : tf.entrySet()) {
            String token = entry.getKey();
            Integer idx = vocabulary.get(token);

            if (idx != null && idx < maxFeatures) {
                tfidf[idx] = entry.getValue() * idf[idx];
            }
        }

        // L2 Normalization
        double norm = 0.0;
        for (double val : tfidf) {
            norm += val * val;
        }
        norm = Math.sqrt(norm);

        if (norm > 0) {
            for (int i = 0; i < tfidf.length; i++) {
                tfidf[i] /= norm;
            }
        }

        return tfidf;
    }

    /**
     * Load JSON resource từ classpath
     */
    private JsonNode loadJsonResource(String resourcePath) throws Exception {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalArgumentException("Resource not found: " + resourcePath);
            }
            return MAPPER.readTree(is);
        }
    }

    /**
     * Convert JsonNode array to double[]
     */
    private double[] jsonArrayToDoubleArray(JsonNode arrayNode) {
        if (arrayNode == null || !arrayNode.isArray()) {
            return new double[0];
        }

        double[] array = new double[arrayNode.size()];
        for (int i = 0; i < arrayNode.size(); i++) {
            array[i] = arrayNode.get(i).asDouble();
        }
        return array;
    }

    /**
     * DEBUG: Print feature vector chi tiết
     */
    public void debugPrintFeatures(float[] features) {
        String[] featureNames = {
                "event_code", "hour", "weekday", "is_weekend", "is_night", "is_business_hours",
                "hour_sin", "hour_cos", "weekday_sin", "weekday_cos",
                "user_encoded", "process_name_encoded", "parent_name_encoded",
                "is_suspicious_process", "suspicious_cmdline_count", "is_suspicious_parent_child",
                "user_event_count", "process_frequency", "dest_port"
        };

        System.out.println("\n=== FEATURE VECTOR DEBUG ===");
        for (int i = 0; i < Math.min(19, features.length); i++) {
            System.out.printf("%s: %.4f\n", featureNames[i], features[i]);
        }
        System.out.println("... (TF-IDF features omitted)");
        System.out.println("Total features: " + features.length);
        System.out.println("=============================\n");
    }
}
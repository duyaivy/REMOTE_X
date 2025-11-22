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

    private Map<String, List<String>> labelEncoders;

    private Map<String, Integer> tfidfImageVocab;
    private double[] tfidfImageIdf;

    private Map<String, Integer> tfidfCmdlineVocab;
    private double[] tfidfCmdlineIdf;

    private double[] scalerMean;
    private double[] scalerScale;

    private Set<String> suspiciousProcesses;
    private List<String> suspiciousCmdlinePatterns;
    private List<String[]> suspiciousParentChild;

    private Set<String> whitelistedProcesses;

    private Map<String, Integer> userEventCounter = new HashMap<>();
    private Map<String, Integer> processFrequencyCounter = new HashMap<>();

    private int totalFeatures = 69;

    private Pattern imagePattern = Pattern.compile("[A-Za-z0-9_]+");
    private Pattern cmdlinePattern = Pattern.compile("\\b\\w+\\b");

    private Pattern cleanTextPattern = Pattern.compile("[^a-z0-9\\s\\-_\\\\/:.]");
    private Pattern multiSpacePattern = Pattern.compile("\\s+");

    public void loadArtifacts() throws Exception {

        initializeWhitelist();

        JsonNode metadataNode = loadJsonResource("/feature_metadata.json");

        suspiciousProcesses = new HashSet<>();
        JsonNode suspProcs = metadataNode.get("suspicious_processes");
        if (suspProcs != null) {
            for (JsonNode proc : suspProcs) {
                suspiciousProcesses.add(proc.asText().toLowerCase());
            }
        }

        suspiciousCmdlinePatterns = new ArrayList<>();
        JsonNode suspCmds = metadataNode.get("suspicious_cmdline_patterns");
        if (suspCmds != null) {
            for (JsonNode pattern : suspCmds) {
                suspiciousCmdlinePatterns.add(pattern.asText().toLowerCase());
            }
        }

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

        JsonNode scalerNode = loadJsonResource("/scaler.json");
        scalerMean = jsonArrayToDoubleArray(scalerNode.get("mean"));
        scalerScale = jsonArrayToDoubleArray(scalerNode.get("scale"));

    }

    private void initializeWhitelist() {
        whitelistedProcesses = new HashSet<>();

        // Development Tools
        whitelistedProcesses.add("code.exe");
        whitelistedProcesses.add("devenv.exe");
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
        whitelistedProcesses.add("opera.exe"); // Opera
        whitelistedProcesses.add("brave.exe"); // Brave

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
        whitelistedProcesses.add("rg.exe");
        whitelistedProcesses.add("python.exe");
        whitelistedProcesses.add("node.exe");
        whitelistedProcesses.add("java.exe");
        whitelistedProcesses.add("javaw.exe");
        whitelistedProcesses.add("npm.exe");
        whitelistedProcesses.add("pip.exe");

        // System Processes
        whitelistedProcesses.add("nissrv.exe"); // Windows Defender
        whitelistedProcesses.add("svchost.exe"); // Service Host
        whitelistedProcesses.add("explorer.exe"); // Windows Explorer
        whitelistedProcesses.add("searchindexer.exe");

        System.out.println("  • Whitelist initialized: " + whitelistedProcesses.size() + " processes");
    }

    public boolean isWhitelisted(String processName) {
        if (processName == null || processName.isEmpty()) {
            return false;
        }
        return whitelistedProcesses.contains(processName.toLowerCase());
    }

    private String cleanText(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        text = text.toLowerCase();
        text = cleanTextPattern.matcher(text).replaceAll(" ");
        text = multiSpacePattern.matcher(text).replaceAll(" ");

        return text.trim();
    }

    public float[] preprocess(Map<String, Object> rawFeatures) {
        float[] features = new float[totalFeatures];
        int idx = 0;

        String timestamp = (String) rawFeatures.get("timestamp");
        ZonedDateTime zdt = parseTimestamp(timestamp);

        features[idx++] = ((Integer) rawFeatures.get("event_code")).floatValue();

        int hour = zdt.getHour();
        int weekday = zdt.getDayOfWeek().getValue() - 1;

        features[idx++] = hour;
        features[idx++] = weekday;
        features[idx++] = (weekday >= 5) ? 1 : 0;
        features[idx++] = (hour < 6 || hour > 22) ? 1 : 0;
        features[idx++] = (hour >= 9 && hour <= 17 && weekday < 5) ? 1 : 0;
        features[idx++] = (float) Math.sin(2 * Math.PI * hour / 24);
        features[idx++] = (float) Math.cos(2 * Math.PI * hour / 24);
        features[idx++] = (float) Math.sin(2 * Math.PI * weekday / 7);
        features[idx++] = (float) Math.cos(2 * Math.PI * weekday / 7);

        String user = (String) rawFeatures.get("user");
        String processName = (String) rawFeatures.get("process_name");
        String parentName = (String) rawFeatures.get("parent_name");

        features[idx++] = labelEncode("user", user);
        features[idx++] = labelEncode("process_name", processName);
        features[idx++] = labelEncode("parent_name", parentName);

        String cmdLine = (String) rawFeatures.get("command_line");

        features[idx++] = isSuspiciousProcess(processName) ? 1 : 0;
        features[idx++] = countSuspiciousCmdlinePatterns(cmdLine);
        features[idx++] = isSuspiciousParentChild(parentName, processName) ? 1 : 0;

        userEventCounter.put(user, userEventCounter.getOrDefault(user, 0) + 1);
        processFrequencyCounter.put(processName, processFrequencyCounter.getOrDefault(processName, 0) + 1);

        features[idx++] = userEventCounter.get(user);
        features[idx++] = processFrequencyCounter.get(processName);
        features[idx++] = ((Integer) rawFeatures.get("dest_port")).floatValue();

        String imagePath = (String) rawFeatures.get("image_path");
        String cleanedImagePath = cleanText(imagePath != null ? imagePath : "");
        double[] tfidfImage = computeTfidf(cleanedImagePath, tfidfImageVocab, tfidfImageIdf, 30, false);
        for (double val : tfidfImage) {
            features[idx++] = (float) val;
        }

        String cleanedCmdLine = cleanText(cmdLine != null ? cmdLine : "");
        double[] tfidfCmdline = computeTfidf(cleanedCmdLine, tfidfCmdlineVocab, tfidfCmdlineIdf, 20, true);
        for (double val : tfidfCmdline) {
            features[idx++] = (float) val;
        }

        for (int i = 0; i < features.length; i++) {
            if (scalerScale[i] != 0) {
                features[i] = (float) ((features[i] - scalerMean[i]) / scalerScale[i]);
            }
        }

        return features;
    }

    private ZonedDateTime parseTimestamp(String timestamp) {
        try {
            Instant instant = Instant.parse(timestamp);
            return instant.atZone(ZoneId.systemDefault());
        } catch (Exception e) {
            return ZonedDateTime.now();
        }
    }

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

    private boolean isSuspiciousProcess(String processName) {
        if (processName == null || processName.isEmpty()) {
            return false;
        }
        return suspiciousProcesses.contains(processName.toLowerCase());
    }

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

    private double[] computeTfidf(String text, Map<String, Integer> vocabulary,
            double[] idf, int maxFeatures, boolean isCommandLine) {
        double[] tfidf = new double[maxFeatures];

        if (text == null || text.isEmpty()) {
            return tfidf;
        }

        List<String> tokens;
        if (isCommandLine) {
            tokens = tokenizeCommandLine(text);
        } else {
            tokens = tokenizeImage(text);
        }

        if (tokens.isEmpty()) {
            return tfidf;
        }

        Map<String, Double> tf = new HashMap<>();
        for (String token : tokens) {
            tf.put(token, tf.getOrDefault(token, 0.0) + 1.0);
        }

        double totalTokens = tokens.size();
        if (totalTokens > 0) {
            for (String token : tf.keySet()) {
                tf.put(token, tf.get(token) / totalTokens);
            }
        }

        for (Map.Entry<String, Double> entry : tf.entrySet()) {
            String token = entry.getKey();
            Integer idx = vocabulary.get(token);

            if (idx != null && idx < maxFeatures) {
                tfidf[idx] = entry.getValue() * idf[idx];
            }
        }

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

    private JsonNode loadJsonResource(String resourcePath) throws Exception {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IllegalArgumentException("Resource not found: " + resourcePath);
            }
            return MAPPER.readTree(is);
        }
    }

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

}
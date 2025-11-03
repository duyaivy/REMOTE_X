package monitor.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class CommandExecutor {

    public static int runCommand(File workingDir, boolean waitForCompletion, String... command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(workingDir);
        pb.redirectErrorStream(true);

        System.out.println("[CMD] Thực thi: " + String.join(" ", command));
        Process process = pb.start();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(" > " + line);
            }
        }

        return waitForCompletion ? process.waitFor() : 0;
    }
}
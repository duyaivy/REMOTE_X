package monitor.log;

import monitor.config.AgentConfig;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

public class NdjsonTailer {

    private final LogHandler logHandler;
    private volatile boolean running = true;
    private Path currentFile;
    private long lastPosition = 0L;

    public NdjsonTailer(LogHandler logHandler) {
        this.logHandler = logHandler;
    }

    public void start() throws IOException, InterruptedException {
        currentFile = findLatestNdjsonFile();

        if (currentFile == null) {
            System.out.println("Không tìm thấy NDJSON");
            Thread.sleep(5000);
            currentFile = findLatestNdjsonFile();

            if (currentFile == null) {
                throw new FileNotFoundException("Không tìm thấy NDJSON " + AgentConfig.INSTALL_DIR);
            }
        }

        System.out.println("Đang theo dõi file: " + currentFile);

        while (running) {
            try {
                checkForNewFile();
                processNewLines();
                Thread.sleep(AgentConfig.TAIL_INTERVAL_MS);
            } catch (IOException e) {
                System.err.println("Lỗi đọc file: " + e.getMessage());
                Thread.sleep(1000);
            }
        }
    }

    public void stop() {
        running = false;
    }

    private void checkForNewFile() throws IOException {
        Path latest = findLatestNdjsonFile();
        if (latest != null && !latest.equals(currentFile)) {
            System.out.println("New NDJSON file: " + latest.getFileName());
            currentFile = latest;
            lastPosition = 0L;
        }
    }

    private void processNewLines() throws IOException {
        long currentSize = Files.size(currentFile);

        if (currentSize < lastPosition) {
            lastPosition = 0;
        }

        if (currentSize > lastPosition) {
            try (RandomAccessFile raf = new RandomAccessFile(currentFile.toFile(), "r")) {
                raf.seek(lastPosition);
                String line;

                while ((line = raf.readLine()) != null) {
                    String decoded = new String(
                            line.getBytes(StandardCharsets.ISO_8859_1),
                            StandardCharsets.UTF_8).trim();

                    if (!decoded.isEmpty()) {
                        logHandler.handleLog(decoded);
                    }
                }

                lastPosition = raf.getFilePointer();
            }
        }
    }

    private Path findLatestNdjsonFile() throws IOException {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(
                AgentConfig.INSTALL_DIR, AgentConfig.NDJSON_PATTERN)) {

            Path newest = null;
            FileTime newestTime = FileTime.fromMillis(0);

            for (Path file : stream) {
                FileTime fileTime = Files.getLastModifiedTime(file);
                if (fileTime.compareTo(newestTime) > 0) {
                    newestTime = fileTime;
                    newest = file;
                }
            }

            return newest;
        }
    }
}
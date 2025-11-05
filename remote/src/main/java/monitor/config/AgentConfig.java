package monitor.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Cấu hình tập trung cho Agent
 */
public class AgentConfig {

    // Đường dẫn cài đặt
    public static final String INSTALL_DIR_STR = System.getenv("ProgramData") + "\\YourAppMonitor";
    public static final Path INSTALL_DIR = Paths.get(INSTALL_DIR_STR);

    // Winlogbeat
    public static final String WINLOGBEAT_DIR_NAME = "winlogbeat-9.2.0-windows-x86_64";
    public static final Path WINLOGBEAT_DIR = INSTALL_DIR.resolve(WINLOGBEAT_DIR_NAME);
    public static final Path WINLOGBEAT_YML = WINLOGBEAT_DIR.resolve("winlogbeat.yml");
    public static final Path WINLOGBEAT_PS1 = WINLOGBEAT_DIR.resolve("install-service-winlogbeat.ps1");

    // Resources
    public static final String RES_SYSMON = "Sysmon.exe";
    public static final String RES_SYSMON_CONFIG = "sysmonconfig.xml";
    public static final String RES_WINLOGBEAT_ZIP = "winlogbeat-9.2.0-windows-x86_64.zip";

    // Đường dẫn files
    public static final Path SYSMON_EXE = INSTALL_DIR.resolve(RES_SYSMON);
    public static final Path SYSMON_CONFIG = INSTALL_DIR.resolve(RES_SYSMON_CONFIG);
    public static final Path WINLOGBEAT_ZIP = INSTALL_DIR.resolve(RES_WINLOGBEAT_ZIP);

    // ✅ FIX: Tạo file log mới với timestamp mỗi lần chạy
    private static final String LOG_FILENAME = "agent_logs_" +
            LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".txt";
    public static final Path AGENT_LOG_FILE = INSTALL_DIR.resolve(LOG_FILENAME);

    // Cấu hình giám sát
    public static final long TAIL_INTERVAL_MS = 400;
    public static final int THREAD_POOL_SIZE = 4;
    public static final String NDJSON_PATTERN = "winlogbeat_output*.ndjson";

    private AgentConfig() {
    } // Prevent instantiation
}
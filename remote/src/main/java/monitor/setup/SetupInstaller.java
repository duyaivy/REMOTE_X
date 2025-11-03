package monitor.setup;

import monitor.config.AgentConfig;
import monitor.util.CommandExecutor;
import monitor.util.PrivilegeChecker;
import monitor.util.ResourceExtractor;
import net.lingala.zip4j.ZipFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Setup Installer - Chạy một lần với quyền Admin
 * Cài đặt Sysmon và Winlogbeat như Windows Services
 */
public class SetupInstaller {

    private static final Path INSTALL_FLAG = AgentConfig.INSTALL_DIR.resolve(".installed");
    private static final Path SERVICE_STATUS = AgentConfig.INSTALL_DIR.resolve("service_status.json");

    public static void main(String[] args) {
        printBanner();

        // Kiểm tra quyền admin
        if (!PrivilegeChecker.isAdmin()) {
            PrivilegeChecker.showAdminRequirementMessage();
            System.exit(1);
        }

        try {
            SetupInstaller installer = new SetupInstaller();
            installer.install();
            System.out.println("\n✓ Cài đặt hoàn tất! Bạn có thể chạy Agent bình thường.");
            System.exit(0);
        } catch (Exception e) {
            System.err.println("\n✗ Cài đặt thất bại: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void install() throws Exception {
        System.out.println("[SETUP] Bắt đầu cài đặt Agent...\n");

        // 1. Tạo thư mục
        createDirectories();

        // 2. Trích xuất resources
        extractResources();

        // 3. Giải nén Winlogbeat
        extractWinlogbeat();

        // 4. Ghi cấu hình Winlogbeat
        writeWinlogbeatConfig();

        // 5. Cài đặt Sysmon
        installSysmon();

        // 6. Cài đặt Winlogbeat service
        installWinlogbeatService();

        // 7. Khởi động services
        startServices();

        // 8. Đánh dấu đã cài đặt
        markAsInstalled();

        // 9. Tạo file status
        createServiceStatus();
    }

    private void createDirectories() throws IOException {
        System.out.println("[1/9] Tạo thư mục cài đặt...");
        Files.createDirectories(AgentConfig.INSTALL_DIR);
        System.out.println("      → " + AgentConfig.INSTALL_DIR);
    }

    private void extractResources() throws IOException {
        System.out.println("\n[2/9] Trích xuất resources...");
        ResourceExtractor.extractResource(AgentConfig.RES_SYSMON, AgentConfig.SYSMON_EXE);
        ResourceExtractor.extractResource(AgentConfig.RES_SYSMON_CONFIG, AgentConfig.SYSMON_CONFIG);
        ResourceExtractor.extractResource(AgentConfig.RES_WINLOGBEAT_ZIP, AgentConfig.WINLOGBEAT_ZIP);
    }

    private void extractWinlogbeat() throws Exception {
        System.out.println("\n[3/9] Giải nén Winlogbeat...");
        if (!Files.exists(AgentConfig.WINLOGBEAT_DIR)) {
            try (ZipFile zipFile = new ZipFile(AgentConfig.WINLOGBEAT_ZIP.toFile())) {
                zipFile.extractAll(AgentConfig.INSTALL_DIR_STR);
            }
            System.out.println("      ✓ Đã giải nén Winlogbeat");
        } else {
            System.out.println("      ⊙ Winlogbeat đã tồn tại, bỏ qua");
        }
    }

    private void writeWinlogbeatConfig() throws IOException {
        System.out.println("\n[4/9] Ghi cấu hình Winlogbeat...");
        String config = buildWinlogbeatConfig();
        Files.writeString(AgentConfig.WINLOGBEAT_YML, config, StandardCharsets.UTF_8);
        System.out.println("      → " + AgentConfig.WINLOGBEAT_YML);
    }

    private void installSysmon() throws Exception {
        System.out.println("\n[5/9] Cài đặt Sysmon...");

        if (PrivilegeChecker.isSysmonInstalled()) {
            System.out.println("      ⊙ Sysmon đã được cài đặt");
            // Update config nếu cần
            System.out.println("      → Cập nhật config...");
            CommandExecutor.runCommand(
                    AgentConfig.INSTALL_DIR.toFile(),
                    true,
                    AgentConfig.SYSMON_EXE.toString(),
                    "-c", AgentConfig.SYSMON_CONFIG.toString());
        } else {
            CommandExecutor.runCommand(
                    AgentConfig.INSTALL_DIR.toFile(),
                    true,
                    AgentConfig.SYSMON_EXE.toString(),
                    "-accepteula",
                    "-i", AgentConfig.SYSMON_CONFIG.toString());
            System.out.println("      ✓ Đã cài đặt Sysmon service");
        }
    }

    private void installWinlogbeatService() throws Exception {
        System.out.println("\n[6/9] Cài đặt Winlogbeat service...");

        if (PrivilegeChecker.isWinlogbeatInstalled()) {
            System.out.println("      ⊙ Winlogbeat service đã tồn tại");
        } else {
            CommandExecutor.runCommand(
                    AgentConfig.WINLOGBEAT_DIR.toFile(),
                    true,
                    "powershell.exe",
                    "-ExecutionPolicy", "Bypass",
                    "-File", AgentConfig.WINLOGBEAT_PS1.toAbsolutePath().toString());
            System.out.println("      ✓ Đã cài đặt Winlogbeat service");
        }
    }

    private void startServices() throws Exception {
        System.out.println("\n[7/9] Khởi động services...");

        // Start Sysmon
        try {
            CommandExecutor.runCommand(
                    AgentConfig.INSTALL_DIR.toFile(),
                    true,
                    "sc", "start", "Sysmon");
            System.out.println("      ✓ Sysmon đang chạy");
        } catch (Exception e) {
            System.out.println("      ⊙ Sysmon có thể đã chạy rồi");
        }

        // Start Winlogbeat
        try {
            CommandExecutor.runCommand(
                    AgentConfig.INSTALL_DIR.toFile(),
                    true,
                    "sc", "start", "winlogbeat");
            System.out.println("      ✓ Winlogbeat đang chạy");
        } catch (Exception e) {
            System.out.println("      ⊙ Winlogbeat có thể đã chạy rồi");
        }
    }

    private void markAsInstalled() throws IOException {
        System.out.println("\n[8/9] Đánh dấu cài đặt hoàn tất...");
        String info = String.format(
                "Installed at: %s\nVersion: 2.0\nSysmon: %s\nWinlogbeat: %s",
                java.time.Instant.now(),
                PrivilegeChecker.isSysmonInstalled() ? "Installed" : "Failed",
                PrivilegeChecker.isWinlogbeatInstalled() ? "Installed" : "Failed");
        Files.writeString(INSTALL_FLAG, info, StandardCharsets.UTF_8);
    }

    private void createServiceStatus() throws IOException {
        System.out.println("\n[9/9] Tạo file trạng thái...");
        String status = String.format(
                "{\n" +
                        "  \"installed\": true,\n" +
                        "  \"timestamp\": \"%s\",\n" +
                        "  \"sysmon_installed\": %b,\n" +
                        "  \"winlogbeat_installed\": %b,\n" +
                        "  \"install_dir\": \"%s\"\n" +
                        "}",
                java.time.Instant.now(),
                PrivilegeChecker.isSysmonInstalled(),
                PrivilegeChecker.isWinlogbeatInstalled(),
                AgentConfig.INSTALL_DIR.toString().replace("\\", "\\\\"));
        Files.writeString(SERVICE_STATUS, status, StandardCharsets.UTF_8);
        System.out.println("      → " + SERVICE_STATUS);
    }

    private String buildWinlogbeatConfig() {
        return "winlogbeat.event_logs:\n" +
                "  - name: Microsoft-Windows-Sysmon/Operational\n" +
                "    ignore_older: 72h\n" +
                "\n" +
                "output.file:\n" +
                "  path: \"" + AgentConfig.INSTALL_DIR.toString().replace("\\", "\\\\") + "\"\n" +
                "  filename: winlogbeat_output.ndjson\n" +
                "  rotate_every_kb: 10240\n" +
                "  number_of_files: 5\n" +
                "  codec.json:\n" +
                "    pretty: false\n" +
                "\n" +
                "logging.level: info\n" +
                "logging.to_files: true\n" +
                "logging.files:\n" +
                "  path: " + AgentConfig.WINLOGBEAT_DIR.toString().replace("\\", "\\\\") + "\\\\logs\n" +
                "  name: winlogbeat\n" +
                "  keepfiles: 5\n" +
                "\n" +
                "queue.mem:\n" +
                "  events: 4096\n" +
                "  flush.min_events: 512\n" +
                "  flush.timeout: 1s\n";
    }

    private static void printBanner() {
        System.out.println("╔═══════════════════════════════════════════════════════╗");
        System.out.println("║                                                       ║");
        System.out.println("║       🔧 AGENT MONITORING SYSTEM - INSTALLER          ║");
        System.out.println("║                                                       ║");
        System.out.println("║  Chương trình này sẽ cài đặt:                        ║");
        System.out.println("║  • Sysmon (System Monitor)                           ║");
        System.out.println("║  • Winlogbeat (Log Shipper)                          ║");
        System.out.println("║                                                       ║");
        System.out.println("║  ⚠️  Yêu cầu: Quyền Administrator                     ║");
        System.out.println("║                                                       ║");
        System.out.println("╚═══════════════════════════════════════════════════════╝");
        System.out.println();
    }

    /**
     * Kiểm tra xem đã cài đặt chưa
     */
    public static boolean isInstalled() {
        return Files.exists(INSTALL_FLAG);
    }
}
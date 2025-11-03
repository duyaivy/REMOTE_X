package monitor.setup;

import monitor.config.AgentConfig;
import monitor.util.CommandExecutor;
import monitor.util.PrivilegeChecker;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/**
 * Setup Uninstaller - Gỡ cài đặt toàn bộ Agent
 */
public class SetupUninstaller {

    public static void main(String[] args) {
        printBanner();

        // Kiểm tra quyền admin
        if (!PrivilegeChecker.isAdmin()) {
            PrivilegeChecker.showAdminRequirementMessage();
            System.exit(1);
        }

        try {
            SetupUninstaller uninstaller = new SetupUninstaller();
            uninstaller.uninstall();
            System.out.println("\n✓ Gỡ cài đặt hoàn tất!");
            System.exit(0);
        } catch (Exception e) {
            System.err.println("\n✗ Gỡ cài đặt thất bại: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    public void uninstall() throws Exception {
        System.out.println("[UNINSTALL] Bắt đầu gỡ cài đặt Agent...\n");

        // 1. Dừng services
        stopServices();

        // 2. Gỡ Winlogbeat service
        uninstallWinlogbeatService();

        // 3. Gỡ Sysmon
        uninstallSysmon();

        // 4. Xóa files
        deleteFiles();
    }

    private void stopServices() throws Exception {
        System.out.println("[1/4] Dừng services...");

        // Stop Winlogbeat
        try {
            CommandExecutor.runCommand(
                    AgentConfig.INSTALL_DIR.toFile(),
                    true,
                    "sc", "stop", "winlogbeat");
            Thread.sleep(2000);
            System.out.println("      ✓ Đã dừng Winlogbeat");
        } catch (Exception e) {
            System.out.println("      ⊙ Winlogbeat không chạy hoặc đã dừng");
        }

        // Stop Sysmon
        try {
            CommandExecutor.runCommand(
                    AgentConfig.INSTALL_DIR.toFile(),
                    true,
                    "sc", "stop", "Sysmon");
            Thread.sleep(2000);
            System.out.println("      ✓ Đã dừng Sysmon");
        } catch (Exception e) {
            System.out.println("      ⊙ Sysmon không chạy hoặc đã dừng");
        }
    }

    private void uninstallWinlogbeatService() throws Exception {
        System.out.println("\n[2/4] Gỡ Winlogbeat service...");

        if (PrivilegeChecker.isWinlogbeatInstalled()) {
            Path uninstallScript = AgentConfig.WINLOGBEAT_DIR.resolve("uninstall-service-winlogbeat.ps1");

            if (Files.exists(uninstallScript)) {
                CommandExecutor.runCommand(
                        AgentConfig.WINLOGBEAT_DIR.toFile(),
                        true,
                        "powershell.exe",
                        "-ExecutionPolicy", "Bypass",
                        "-File", uninstallScript.toAbsolutePath().toString());
            } else {
                // Manual uninstall
                CommandExecutor.runCommand(
                        AgentConfig.INSTALL_DIR.toFile(),
                        true,
                        "sc", "delete", "winlogbeat");
            }
            System.out.println("      ✓ Đã gỡ Winlogbeat service");
        } else {
            System.out.println("      ⊙ Winlogbeat service không tồn tại");
        }
    }

    private void uninstallSysmon() throws Exception {
        System.out.println("\n[3/4] Gỡ Sysmon...");

        if (PrivilegeChecker.isSysmonInstalled()) {
            if (Files.exists(AgentConfig.SYSMON_EXE)) {
                CommandExecutor.runCommand(
                        AgentConfig.INSTALL_DIR.toFile(),
                        true,
                        AgentConfig.SYSMON_EXE.toString(),
                        "-u");
                System.out.println("      ✓ Đã gỡ Sysmon");
            } else {
                System.out.println("      ⚠ Không tìm thấy Sysmon.exe để gỡ");
            }
        } else {
            System.out.println("      ⊙ Sysmon không được cài đặt");
        }
    }

    private void deleteFiles() throws Exception {
        System.out.println("\n[4/4] Xóa files...");

        if (Files.exists(AgentConfig.INSTALL_DIR)) {
            // Xóa toàn bộ thư mục
            Files.walk(AgentConfig.INSTALL_DIR)
                    .sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                        } catch (Exception e) {
                            System.err.println("      ⚠ Không thể xóa: " + path);
                        }
                    });
            System.out.println("      ✓ Đã xóa thư mục: " + AgentConfig.INSTALL_DIR);
        } else {
            System.out.println("      ⊙ Thư mục không tồn tại");
        }
    }

    private static void printBanner() {
        System.out.println("╔═══════════════════════════════════════════════════════╗");
        System.out.println("║                                                       ║");
        System.out.println("║       🗑️  AGENT MONITORING SYSTEM - UNINSTALLER       ║");
        System.out.println("║                                                       ║");
        System.out.println("║  Chương trình này sẽ gỡ bỏ:                          ║");
        System.out.println("║  • Sysmon service                                    ║");
        System.out.println("║  • Winlogbeat service                                ║");
        System.out.println("║  • Tất cả files đã cài đặt                           ║");
        System.out.println("║                                                       ║");
        System.out.println("║  ⚠️  Yêu cầu: Quyền Administrator                     ║");
        System.out.println("║                                                       ║");
        System.out.println("╚═══════════════════════════════════════════════════════╝");
        System.out.println();
    }
}
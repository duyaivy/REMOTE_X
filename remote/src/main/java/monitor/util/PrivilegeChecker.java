package monitor.util;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PrivilegeChecker {

    /**
     * Kiểm tra xem có đang chạy với quyền admin không
     * 
     * @return true nếu có quyền admin
     */
    public static boolean isAdmin() {
        // Phương pháp 1: Thử ghi vào System32
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            try {
                Path testFile = Paths.get(System.getenv("windir"), "system32", "test_admin.tmp");
                Files.createFile(testFile);
                Files.delete(testFile);
                return true;
            } catch (Exception e) {
                return false;
            }
        }
        return false;
    }

    /**
     * Kiểm tra xem Sysmon đã được cài đặt chưa
     */
    public static boolean isSysmonInstalled() {
        try {
            Process process = new ProcessBuilder("sc", "query", "Sysmon").start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Kiểm tra xem Winlogbeat service đã được cài đặt chưa
     */
    public static boolean isWinlogbeatInstalled() {
        try {
            Process process = new ProcessBuilder("sc", "query", "winlogbeat").start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Hiển thị hướng dẫn chạy với admin
     */
    public static void showAdminRequirementMessage() {
        System.err.println("╔════════════════════════════════════════════════════════╗");
        System.err.println("║  ⚠️  YÊU CẦU QUYỀN ADMINISTRATOR                       ║");
        System.err.println("╠════════════════════════════════════════════════════════╣");
        System.err.println("║  Agent này cần quyền admin để:                        ║");
        System.err.println("║  • Cài đặt Sysmon driver                              ║");
        System.err.println("║  • Tạo Windows Services                               ║");
        System.err.println("║  • Ghi vào ProgramData                                ║");
        System.err.println("║                                                        ║");
        System.err.println("║  Vui lòng:                                            ║");
        System.err.println("║  1. Đóng ứng dụng này                                 ║");
        System.err.println("║  2. Click phải → 'Run as Administrator'               ║");
        System.err.println("║  3. Khởi động lại                                     ║");
        System.err.println("╚════════════════════════════════════════════════════════╝");
    }
}
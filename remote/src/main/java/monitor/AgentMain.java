package monitor;

import monitor.log.LogHandler;
import monitor.log.NdjsonTailer;
import monitor.setup.SetupInstaller;
import monitor.util.PrivilegeChecker;

import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AgentMain {

    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(4);
    private static volatile boolean running = true;
    private static NdjsonTailer tailer;
    private static LogHandler logHandler;

    public static void main(String[] args) {
        printBanner();

        // Kiểm tra xem đã setup chưa
        if (!SetupInstaller.isInstalled()) {
            System.err.println("╔═══════════════════════════════════════════════════════╗");
            System.err.println("║  ⚠️  CHƯA CÀI ĐẶT AGENT                               ║");
            System.err.println("╠═══════════════════════════════════════════════════════╣");
            System.err.println("║                                                       ║");
            System.err.println("║  Agent chưa được cài đặt. Bạn cần chạy:             ║");
            System.err.println("║                                                       ║");
            System.err.println("║    java -jar agent.jar --setup                       ║");
            System.err.println("║                                                       ║");
            System.err.println("║  với quyền Administrator để cài đặt services.        ║");
            System.err.println("║                                                       ║");
            System.err.println("╚═══════════════════════════════════════════════════════╝");
            System.out.println("\nBạn có muốn chạy setup ngay bây giờ? (y/n)");

            Scanner scanner = new Scanner(System.in);
            String response = scanner.nextLine().trim().toLowerCase();

            if (response.equals("y") || response.equals("yes")) {
                if (!PrivilegeChecker.isAdmin()) {
                    System.err.println("\n⚠️  Vui lòng khởi động lại với quyền Administrator!");
                    System.exit(1);
                }
                try {
                    new SetupInstaller().install();
                    System.out.println("\n✓ Setup hoàn tất! Khởi động Agent...\n");
                } catch (Exception e) {
                    System.err.println("✗ Setup thất bại: " + e.getMessage());
                    System.exit(1);
                }
            } else {
                System.exit(1);
            }
        }

        // Kiểm tra services có đang chạy không
        checkServicesRunning();

        // Đăng ký shutdown hook
        registerShutdownHook();

        try {
            System.out.println("[START] Khởi động Agent (chế độ chỉ đọc)...\n");

            // Khởi tạo log handler
            logHandler = new LogHandler();
            logHandler.loadPreviousLogs();

            // Bắt đầu theo dõi logs
            System.out.println("[INFO] Agent đang hoạt động - Nhấn Ctrl+C để dừng\n");
            tailer = new NdjsonTailer(logHandler);
            tailer.start();

        } catch (Exception e) {
            System.err.println("╔═══════════════════════════════════════════════════════╗");
            System.err.println("║  ❌ LỖI NGHIÊM TRỌNG                                 ║");
            System.err.println("╚═══════════════════════════════════════════════════════╝");
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void checkServicesRunning() {
        System.out.println("[CHECK] Kiểm tra trạng thái services...");

        boolean sysmonOk = PrivilegeChecker.isSysmonInstalled();
        boolean winlogbeatOk = PrivilegeChecker.isWinlogbeatInstalled();

        System.out.println("  • Sysmon:     " + (sysmonOk ? "✓ Running" : "✗ Not Running"));
        System.out.println("  • Winlogbeat: " + (winlogbeatOk ? "✓ Running" : "✗ Not Running"));
        System.out.println();

        if (!sysmonOk || !winlogbeatOk) {
            System.err.println("⚠️  WARNING: Một hoặc nhiều service không chạy!");
            System.err.println("   Hãy kiểm tra Windows Services hoặc chạy lại setup.\n");
        }
    }

    private static void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[SHUTDOWN] Đang tắt Agent...");

            running = false;

            if (tailer != null) {
                tailer.stop();
            }
            if (logHandler != null) {
                logHandler.shutdown();
            }
            EXECUTOR.shutdown();
            try {
                if (!EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                    EXECUTOR.shutdownNow();
                }
            } catch (InterruptedException e) {
                EXECUTOR.shutdownNow();
            }

            System.out.println("[SHUTDOWN] ✓ Đã tắt Agent");
        }));
    }

    private static void printBanner() {
        System.out.println("╔═══════════════════════════════════════════════════════╗");
        System.out.println("║                                                       ║");
        System.out.println("║       📊 AGENT MONITORING SYSTEM v2.0                 ║");
        System.out.println("║          Sysmon + Winlogbeat Logger                   ║");
        System.out.println("║                                                       ║");
        System.out.println("║  Mode: Read-Only (Không cần quyền Admin)            ║");
        System.out.println("║                                                       ║");
        System.out.println("╚═══════════════════════════════════════════════════════╝");
        System.out.println();
    }
}
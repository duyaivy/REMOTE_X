package monitor;

import monitor.setup.SetupInstaller;
import monitor.setup.SetupUninstaller;

public class Launcher {

    public static void main(String[] args) {
        if (args.length > 0) {
            String command = args[0].toLowerCase();

            switch (command) {
                case "--setup":
                case "-s":
                case "setup":
                    SetupInstaller.main(new String[0]);
                    break;

                case "--uninstall":
                case "-u":
                case "uninstall":
                    SetupUninstaller.main(new String[0]);
                    break;

                case "--help":
                case "-h":
                case "help":
                    printHelp();
                    break;

                default:
                    System.err.println("Unknown command: " + command);
                    printHelp();
                    System.exit(1);
            }
        } else {
            // Không có argument -> chạy Agent bình thường
            AgentMain.main(args);
        }
    }

    private static void printHelp() {
        System.out.println("╔═══════════════════════════════════════════════════════╗");
        System.out.println("║                                                       ║");
        System.out.println("║       Agent Monitoring System - Command Line          ║");
        System.out.println("║                                                       ║");
        System.out.println("╚═══════════════════════════════════════════════════════╝");
        System.out.println();
        System.out.println("Usage:");
        System.out.println("  java -jar agent.jar [command]");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  --setup, -s      Cài đặt services (cần Admin)");
        System.out.println("  --uninstall, -u  Gỡ cài đặt (cần Admin)");
        System.out.println("  --help, -h       Hiển thị hướng dẫn này");
        System.out.println("  (no args)        Chạy Agent bình thường");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java -jar agent.jar --setup       # Cài đặt lần đầu");
        System.out.println("  java -jar agent.jar               # Chạy agent");
        System.out.println("  java -jar agent.jar --uninstall   # Gỡ bỏ");
        System.out.println();
    }
}
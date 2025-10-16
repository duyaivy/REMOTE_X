package server;

import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.Socket;
import java.util.Scanner;

public class ReceiveEvents extends Thread {
    Socket socket = null;
    Robot robot = null;
    boolean continueLoop = true;

    public ReceiveEvents(Socket socket, Robot robot) {
        this.socket = socket;
        this.robot = robot;
        start();
    }

    public void run() {
        Scanner scanner = null;
        try {
            scanner = new Scanner(socket.getInputStream());
            while (continueLoop) {
                int command = scanner.nextInt();
                switch (command) {
                    case -1:
                        robot.mousePress(scanner.nextInt());
                        break;
                    case -2:
                        robot.mouseRelease(scanner.nextInt());
                        break;
                    case -3:
                        robot.keyPress(scanner.nextInt());
                        break;
                    case -4:
                        robot.keyRelease(scanner.nextInt());
                        break;
                    case -5:
                        robot.mouseMove(scanner.nextInt(), scanner.nextInt());
                        break;
                    case -7:
                        robot.mouseWheel(scanner.nextInt());
                        break;
                    case -8:
                        robot.mouseMove(scanner.nextInt(), scanner.nextInt());
                        break;
                    default:
                        break;
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}

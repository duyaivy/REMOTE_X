package server;

import java.awt.Robot;
import java.io.DataInputStream;
import java.net.Socket;
import java.awt.event.InputEvent;

public class ReceiveEvent extends Thread {
    private DataInputStream dis;
    private Robot robot;
    private int h, w;

    public ReceiveEvent(Socket controlSocket, Robot robot, int h, int w) {
        try {
            this.dis = new DataInputStream(controlSocket.getInputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.robot = robot;

        this.h = h;
        this.w = w;
        start();
    }

    @Override
    public void run() {
        try {
            while (true) {

                String data = dis.readUTF();
                String[] parts = data.split(",");
                int command = Integer.parseInt(parts[0]);
                switch (command) {
                    case -1:
                        int buttonMask = getButtonMask(Integer.parseInt(parts[1]));
                        if (buttonMask != 0) {
                            robot.mousePress(buttonMask);
                        }
                        break;
                    case -2: {
                        int releaseMask = getButtonMask(Integer.parseInt(parts[1]));
                        if (releaseMask != 0) {
                            robot.mouseRelease(releaseMask);
                        }
                        break;
                    }
                    case -3: {
                        try {
                            int keycode = Integer.parseInt(parts[1]);
                            if (isValidKeyCode(keycode)) {
                                robot.keyPress(keycode);
                                break;
                            }
                        } catch (Exception e) {

                            System.out.println("Invalid keycode");

                        }

                    }
                    case -4: {
                        try {
                            int keycode = Integer.parseInt(parts[1]);
                            if (isValidKeyCode(keycode)) {
                                robot.keyRelease(keycode);
                                break;
                            }
                        } catch (Exception e) {

                            System.out.println("Invalid keycode");

                        }

                    }
                    case -5: {
                        double xRatio = Double.parseDouble(parts[2]);
                        double yRatio = Double.parseDouble(parts[3]);
                        int x = (int) (xRatio * w);
                        int y = (int) (yRatio * h);
                        robot.mouseMove(x, y);
                        break;
                    }
                    case -6:

                        robot.mousePress(Integer.parseInt(parts[1]));
                        robot.mouseRelease(Integer.parseInt(parts[1]));
                        break;
                    case -7:
                        robot.mouseWheel(Integer.parseInt(parts[1]));
                        break;
                    case -8: {
                        double xRatio = Double.parseDouble(parts[2]);
                        double yRatio = Double.parseDouble(parts[3]);
                        int x = (int) (xRatio * w);
                        int y = (int) (yRatio * h);
                        robot.mouseMove(x, y);
                        break;
                    }
                    default:
                        break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public int getButtonMask(int button) {
        switch (button) {
            case 1:
                return InputEvent.BUTTON1_DOWN_MASK;
            case 2:
                return InputEvent.BUTTON2_DOWN_MASK;
            case 3:
                return InputEvent.BUTTON3_DOWN_MASK;
            default:
                return 0;
        }
    }

    private boolean isValidKeyCode(int keyCode) {

        if (keyCode < 0 || keyCode > 65535) {
            return false;
        }
        if (keyCode == 0) {
            return false;
        }

        return true;
    }
}

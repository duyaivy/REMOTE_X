package server;

import java.awt.Robot;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;
import java.awt.event.InputEvent;
import javax.swing.JButton;
import javax.swing.SwingUtilities;

public class ReceiveEvent extends Thread {
    private DataInputStream dis;
    private Robot robot;
    private int h, w;

    private Socket controlSocket;
    private Socket screenSocket;
    private Socket chatSocket;
    private JButton btnStartShare;
    private ShareScreen currentShareScreen = null;

    public ReceiveEvent(Socket controlSocket, Socket screenSocket, Socket chatSocket,
            Robot robot, int h, int w, JButton btnStartShare) {

        this.controlSocket = controlSocket;
        this.screenSocket = screenSocket;
        this.chatSocket = chatSocket;
        this.robot = robot;
        this.h = h;
        this.w = w;

        this.btnStartShare = btnStartShare;
        try {
            this.dis = new DataInputStream(this.controlSocket.getInputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }

        start();
    }

    @Override
    public void run() {
        try {

            while (true) {
                String data = dis.readUTF();

                if (data.equals("START_SESSION")) {
                    System.out.println("ReceiveEvent (Sharer): Nhận START_SESSION - Khởi động ShareScreen");

                    new Thread(() -> {
                        try {
                            currentShareScreen = new ShareScreen(this.screenSocket, this.chatSocket);
                        } catch (Exception e) {
                            System.err.println("Error starting ShareScreen: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }).start();

                    continue;
                }

                if (data.equals("RESTART_SHARESCREEN")) {
                    System.out.println("ReceiveEvent (Sharer): Nhận RESTART_SHARESCREEN");

                    if (currentShareScreen != null) {
                        currentShareScreen.stop();
                        System.out.println("ReceiveEvent (Sharer): Đã dừng ShareScreen cũ");
                        Thread.sleep(500); // Đợi thread cũ dừng hoàn toàn
                    }

                    new Thread(() -> {
                        try {
                            currentShareScreen = new ShareScreen(this.screenSocket, this.chatSocket);
                            System.out.println("ReceiveEvent (Sharer): Đã khởi động ShareScreen mới");
                        } catch (Exception e) {
                            System.err.println("Error restarting ShareScreen: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }).start();

                    continue;
                }

                try {
                    String[] parts = data.split(",");
                    if (parts.length == 0) {
                        System.err.println("Invalid control data (empty): " + data);
                        continue;
                    }

                    int command = Integer.parseInt(parts[0]);

                    switch (command) {
                        case -1: {
                            int buttonMask = getButtonMask(Integer.parseInt(parts[1]));
                            if (buttonMask != 0) {
                                robot.mousePress(buttonMask);
                            }
                            break;
                        }
                        case -2: {
                            int releaseMask = getButtonMask(Integer.parseInt(parts[1]));
                            if (releaseMask != 0) {
                                robot.mouseRelease(releaseMask);
                            }
                            break;
                        }
                        case -3: {
                            int keycode = Integer.parseInt(parts[1]);
                            if (isValidKeyCode(keycode)) {
                                robot.keyPress(keycode);
                            }
                            break;
                        }
                        case -4: {
                            int keycode = Integer.parseInt(parts[1]);
                            if (isValidKeyCode(keycode)) {
                                robot.keyRelease(keycode);
                            }
                            break;
                        }
                        case -5: {
                            if (parts.length < 4) {
                                break;
                            }
                            double xRatio = Double.parseDouble(parts[2]);
                            double yRatio = Double.parseDouble(parts[3]);
                            int x = (int) (xRatio * w);
                            int y = (int) (yRatio * h);
                            robot.mouseMove(x, y);
                            break;
                        }
                        case -6: // Mouse click
                            robot.mousePress(Integer.parseInt(parts[1]));
                            robot.mouseRelease(Integer.parseInt(parts[1]));
                            break;
                        case -7: // Mouse wheel
                            robot.mouseWheel(Integer.parseInt(parts[1]));
                            break;
                        case -8: { // Mouse drag
                            if (parts.length < 4) {

                                break;
                            }
                            double xRatio = Double.parseDouble(parts[2]);
                            double yRatio = Double.parseDouble(parts[3]);
                            int x = (int) (xRatio * w);
                            int y = (int) (yRatio * h);
                            robot.mouseMove(x, y);
                            break;
                        }
                        default:
                            System.out.println("Unknown command: " + command);
                            break;
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Invalid control data format (not a number): " + data);
                } catch (ArrayIndexOutOfBoundsException e) {
                    System.err.println("Invalid control data format (missing parts): " + data);
                } catch (Exception e) {
                    System.err.println("Error processing control event: " + e.getMessage());
                    e.printStackTrace();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {

            e.printStackTrace();
        } finally {

            if (currentShareScreen != null) {
                currentShareScreen.stop();
            }

            try {
                if (controlSocket != null)
                    controlSocket.close();
            } catch (IOException e) {
            }
            try {
                if (screenSocket != null)
                    screenSocket.close();
            } catch (IOException e) {
            }
            try {
                if (chatSocket != null)
                    chatSocket.close();
            } catch (IOException e) {
            }

            SwingUtilities.invokeLater(() -> {
                if (btnStartShare != null) {
                    btnStartShare.setEnabled(true);
                    btnStartShare.setText("Cho phép điều khiển");
                }
            });
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
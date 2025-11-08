package server;

import java.awt.Robot;
import java.io.DataInputStream;
import java.io.IOException; 
import java.net.Socket;
import java.awt.event.InputEvent;
import server.ShareScreen; 
import javax.swing.JButton; // <-- THÊM IMPORT NÀY
import javax.swing.SwingUtilities; // <-- THÊM IMPORT NÀY

public class ReceiveEvent extends Thread {
    private DataInputStream dis;
    private Robot robot;
    private int h, w;

    private Socket controlSocket;
    private Socket screenSocket;
    private Socket chatSocket;
    private JButton btnStartShare; 
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
            // CHỜ TÍN HIỆU "GO"
            String signal = dis.readUTF(); 

            if (signal.equals("START_SESSION")) {
                System.out.println("ReceiveEvent (Sharer): Đã nhận tín hiệu GO! Bắt đầu ShareScreen.");

                //  KHỞI ĐỘNG SHARESCREEN
                new Thread(() -> {
                    try {
                        new ShareScreen(this.screenSocket, this.chatSocket);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();

                //  BẮT ĐẦU VÒNG LẶP NHẬN SỰ KIỆN
                System.out.println("ReceiveEvent (Sharer): Bắt đầu lắng nghe điều khiển...");
                while (true) {
                    String data = dis.readUTF();
                    String[] parts = data.split(",");
                    int command = Integer.parseInt(parts[0]);
                    switch (command) {
                        case -1:
                            int buttonMask = getButtonMask(Integer.parseInt(parts[1]));
                            if (buttonMask != 0) robot.mousePress(buttonMask);
                            break;
                        case -2: 
                            int releaseMask = getButtonMask(Integer.parseInt(parts[1]));
                            if (releaseMask != 0) robot.mouseRelease(releaseMask);
                            break;
                        case -3:
                            robot.keyPress(Integer.parseInt(parts[1]));
                            break;
                        case -4:
                            robot.keyRelease(Integer.parseInt(parts[1]));
                            break;
                        case -5: 
                            double xRatio = Double.parseDouble(parts[2]);
                            double yRatio = Double.parseDouble(parts[3]);
                            int x = (int) (xRatio * w);
                            int y = (int) (yRatio * h);
                            robot.mouseMove(x, y);
                            break;
                        case -6:
                            robot.mousePress(Integer.parseInt(parts[1]));
                            robot.mouseRelease(Integer.parseInt(parts[1]));
                            break;
                        case -7:
                            robot.mouseWheel(Integer.parseInt(parts[1]));
                            break;
                        case -8: 
                            double xRatioD = Double.parseDouble(parts[2]);
                            double yRatioD = Double.parseDouble(parts[3]);
                            int xD = (int) (xRatioD * w);
                            int yD = (int) (yRatioD * h);
                            robot.mouseMove(xD, yD);
                            break;
                        default:
                            break;
                    }
                }
            } else {
                System.err.println("ReceiveEvent (Sharer): Lỗi! Tín hiệu đầu tiên không phải START_SESSION.");
            }
        } catch (IOException e) {
            System.out.println("ReceiveEvent (Sharer): Kết nối điều khiển đã đóng. (" + e.getMessage() + ")");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Dọn dẹp tất cả 3 socket
            try { if (controlSocket != null) controlSocket.close(); } catch (IOException e) {}
            try { if (screenSocket != null) screenSocket.close(); } catch (IOException e) {}
            try { if (chatSocket != null) chatSocket.close(); } catch (IOException e) {}
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
            case 1: return InputEvent.BUTTON1_DOWN_MASK;
            case 2: return InputEvent.BUTTON2_DOWN_MASK;
            case 3: return InputEvent.BUTTON3_DOWN_MASK;
            default: return 0;
        }
    }
}
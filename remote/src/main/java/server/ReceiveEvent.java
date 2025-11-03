package server;

import java.awt.Robot;
import java.io.DataInputStream;
import java.io.IOException; // Thêm import
import java.net.Socket;
import java.awt.event.InputEvent;
// Thêm import cho ShareScreen (mặc dù cùng package nhưng để rõ ràng)
import server.ShareScreen; 

public class ReceiveEvent extends Thread {
    private DataInputStream dis;
    private Robot robot;
    private int h, w;

    // --- BƯỚC 1: Thêm 3 socket vào đây ---
    private Socket controlSocket;
    private Socket screenSocket;
    private Socket chatSocket;

    // ----------------------------------------------------
    // ---- BƯỚC 2: SỬA LẠI HÀM KHỞI TẠO (ĐỂ SỬA LỖI) ----
    // ----------------------------------------------------
    // Hàm này giờ đã khớp với lời gọi từ MainStart (Socket, Socket, Socket, Robot, int, int)
    public ReceiveEvent(Socket controlSocket, Socket screenSocket, Socket chatSocket, Robot robot, int h, int w) {
        // Lưu trữ tất cả 3 socket
        this.controlSocket = controlSocket;
        this.screenSocket = screenSocket;
        this.chatSocket = chatSocket;
        
        this.robot = robot;
        this.h = h;
        this.w = w;

        try {
            // Chỉ tạo DataInputStream từ controlSocket (để nghe tín hiệu và lệnh)
            this.dis = new DataInputStream(this.controlSocket.getInputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // Bắt đầu luồng này (chính là hàm run() bên dưới)
        start(); 
    }

    // ----------------------------------------------------
    // ---- BƯỚC 3: SỬA LẠI HÀM RUN() ----
    // ----------------------------------------------------
    @Override
    public void run() {
        try {
            // --- PHA 1: CHỜ TÍN HIỆU "GO!" TỪ SERVERRELAY ---
            String signal = dis.readUTF(); // Dòng này sẽ "treo" luồng lại, chờ tin nhắn đầu tiên

            // Kiểm tra tín hiệu
            if (signal.equals("START_SESSION")) {
                System.out.println("ReceiveEvent (Sharer): Đã nhận tín hiệu GO! Bắt đầu ShareScreen.");

                // --- PHA 2: KHỞI ĐỘNG SHARESCREEN VÀ CHATBUTTON ---
                new Thread(() -> {
                    try {
                        // Khởi động ShareScreen (để gửi màn hình)
                        new ShareScreen(this.screenSocket, this.chatSocket);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }).start();

                // --- PHA 3: BẮT ĐẦU VÒNG LẶP NHẬN SỰ KIỆN (CODE CŨ CỦA BẠN) ---
                System.out.println("ReceiveEvent (Sharer): Bắt đầu lắng nghe điều khiển...");
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
                        case -3:
                            robot.keyPress(Integer.parseInt(parts[1]));
                            break;
                        case -4:
                            robot.keyRelease(Integer.parseInt(parts[1]));
                            break;
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
            } else {
                System.err.println("ReceiveEvent (Sharer): Lỗi! Tín hiệu đầu tiên không phải START_SESSION, mà là: " + signal);
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
}
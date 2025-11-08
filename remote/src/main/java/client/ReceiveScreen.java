package client;

import common.ChatWindow;
import javax.swing.*;
import java.awt.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream; 
import java.io.IOException;
import java.net.Socket;

public class ReceiveScreen extends JFrame {

    private volatile BufferedImage currentImage = null;
    private volatile String statusMessage = "Đang kết nối tới server...";
    private final JPanel screenPanel;
    private ChatWindow chatWindow; 

    // --- HOÀN NGUYÊN HÀM KHỞI TẠO (5 THAM SỐ) ---
    public ReceiveScreen(Socket dataSocket, float width, float height, Socket controlSocket, Socket chatSocket) {
        
        // ... (Toàn bộ code UI, JMenuBar, setSize, ... của bạn giữ nguyên) ...
        setTitle("RemoteX Screen Viewer");
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        double remoteWidth = width;
        double remoteHeight = height;
        if (remoteWidth > screenSize.width || remoteHeight > screenSize.height) {
            double widthRatio = screenSize.width / remoteWidth;
            double heightRatio = screenSize.height / remoteHeight;
            double scaleFactor = Math.min(widthRatio, heightRatio);
            width = (int) (remoteWidth * scaleFactor);
            height = (int) (remoteHeight * scaleFactor);
        }
        setSize((int) (int) width, (int) height);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        screenPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (currentImage != null) {
                    g.drawImage(currentImage, 0, 0, getWidth(), getHeight(), null);
                } else {
                    g.setColor(Color.BLACK);
                    g.fillRect(0, 0, getWidth(), getHeight());
                    g.setColor(Color.WHITE);
                    g.setFont(new Font("Arial", Font.BOLD, 18));
                    g.drawString(statusMessage, 50, 50);
                }
            }
        };
        add(screenPanel);

        // Khởi động Chat (chạy ngầm)
        this.chatWindow = new ChatWindow(chatSocket, "Server");
        
        // Tạo Menu Bar
        JMenuBar menuBar = new JMenuBar();
        JMenu toolsMenu = new JMenu("Công cụ");
        JMenuItem chatMenuItem = new JMenuItem("Mở Chat");
        chatMenuItem.addActionListener(e -> {
            if (this.chatWindow != null) {
                this.chatWindow.showWindow();
            }
        });
        toolsMenu.add(chatMenuItem);
        menuBar.add(toolsMenu);
        this.setJMenuBar(menuBar);
        
        setVisible(true); 

        // ----------------------------------------------------
        // ---- SỬA LẠI: CHẠY NGAY LẬP TỨC (NHƯ BAN ĐẦU) ----
        // ----------------------------------------------------

        // 1. Xóa bỏ luồng chờ "GO!"
        // new Thread(() -> { ... controlInputStream.readUTF() ... }).start(); // <-- ĐÃ XÓA

        // 2. Khởi động luồng NHẬN màn hình ngay
        //    Nó sẽ tự động chờ (block) ở 'receiveFrames' cho đến khi Sharer gửi
        new Thread(() -> receiveFrames(dataSocket)).start();

        // 3. Khởi động luồng GỬI điều khiển ngay
        new ControlEvent(controlSocket, screenPanel);
    }
    
    private void receiveFrames(Socket socket) {
        try (DataInputStream in = new DataInputStream(socket.getInputStream())) {
            
            // Dòng này sẽ "treo" lại, chờ ShareScreen (Sharer) gửi int đầu tiên
            // (khi Sharer nhận được tín hiệu "GO!")
            int screenWidth = in.readInt(); 
            int screenHeight = in.readInt();

            currentImage = new BufferedImage(screenWidth, screenHeight, BufferedImage.TYPE_INT_RGB);
            statusMessage = null; // Xóa chữ "Đang kết nối..."

            while (!socket.isClosed()) {
                boolean isFullFrame = in.readBoolean();
                in.readInt(); 
                if (isFullFrame) {
                    processFullFrame(in);
                } else {
                    processDeltaFrame(in);
                }
                screenPanel.repaint();
            }
        } catch (Exception e) {
            e.printStackTrace();
            statusMessage = "Mất kết nối tới server: " + e.getMessage();
            currentImage = null;
            screenPanel.repaint();
        }
    }
    
    // ... (Các hàm processFullFrame, processDeltaFrame giữ nguyên) ...
    private void processFullFrame(DataInputStream in) throws IOException {
        int dataLength = in.readInt();
        byte[] frameData = new byte[dataLength];
        in.readFully(frameData);
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(frameData));
        if (img != null) {
            currentImage = img;
        }
    }

    private void processDeltaFrame(DataInputStream in) throws IOException {
        int x = in.readInt();
        int y = in.readInt();
        int width = in.readInt();
        int height = in.readInt();
        int dataLength = in.readInt();
        byte[] frameData = new byte[dataLength];
        in.readFully(frameData);
        BufferedImage deltaImg = ImageIO.read(new ByteArrayInputStream(frameData));
        if (currentImage != null && deltaImg != null) {
            Graphics2D g2d = currentImage.createGraphics();
            g2d.drawImage(deltaImg, x, y, null);
            g2d.dispose();
        }
    }
}
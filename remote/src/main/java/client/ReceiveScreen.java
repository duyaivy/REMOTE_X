package client;

// --- CÁC IMPORT THÊM VÀO ---
import common.ChatWindow; // <-- Đảm bảo import đúng package
import javax.swing.*;
import java.awt.*;
//... (các import khác của bạn) ...
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream; // <-- THÊM IMPORT NÀY
import java.io.IOException;
import java.net.Socket;

public class ReceiveScreen extends JFrame {

    // (Các biến cũ của bạn giữ nguyên)
    private volatile BufferedImage currentImage = null;
    private volatile String statusMessage = "Đang kết nối tới server...";
    private final JPanel screenPanel;
    private ChatWindow chatWindow; 

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
        setSize((int) width, (int) height);
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
        // ---- SỬA LẠI LOGIC CHỜ TÍN HIỆU "GO!" ----
        // ----------------------------------------------------

        // 1. XÓA DÒNG GỌI `new ControlEvent(...)` CŨ Ở ĐÂY
        // new ControlEvent(controlSocket, screenPanel); // <-- LỖI LÀ Ở ĐÂY

        // 2. Tạo luồng chờ tín hiệu "GO!"
        new Thread(() -> {
            try {
                // Dùng controlSocket (InputStream) để chờ tín hiệu
                DataInputStream disControl = new DataInputStream(controlSocket.getInputStream());
                
                System.out.println("ReceiveScreen (Viewer): Đang chờ tín hiệu START_SESSION...");
                String signal = disControl.readUTF(); 
                
                if (signal.equals("START_SESSION")) {
                    System.out.println("ReceiveScreen (Viewer): Đã nhận tín hiệu GO!");
                    
                    // PHA 1: KHỞI ĐỘNG LUỒNG NHẬN MÀN HÌNH
                    new Thread(() -> receiveFrames(dataSocket)).start();
                    
                    // PHA 2: KHỞI ĐỘNG LUỒNG GỬI ĐIỀU KHIỂN
                    // (Chỉ chạy sau khi đã nhận được tín hiệu "GO")
                    new ControlEvent(controlSocket, screenPanel);
                    
                } else {
                     System.err.println("ReceiveScreen (Viewer): Nhận được tín hiệu lạ: " + signal);
                }
            } catch (IOException e) {
                System.err.println("ReceiveScreen (Viewer): Mất kết nối khi chờ tín hiệu GO: " + e.getMessage());
                statusMessage = "Mất kết nối tới server: " + e.getMessage();
                currentImage = null;
                screenPanel.repaint();
            }
        }).start();

        // 3. XÓA dòng chạy `receiveFrames` cũ
        // new Thread(() -> receiveFrames(dataSocket)).start(); // <-- ĐÃ XÓA
    }
    
    // ... (Các hàm receiveFrames, processFullFrame, processDeltaFrame giữ nguyên) ...
    
    private void receiveFrames(Socket socket) {
        try (DataInputStream in = new DataInputStream(socket.getInputStream())) {
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
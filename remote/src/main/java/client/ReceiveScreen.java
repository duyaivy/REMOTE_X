package client;

// --- CÁC IMPORT THÊM VÀO ---
import common.ChatWindow; // <-- Đảm bảo import đúng package
import javax.swing.*;
import java.awt.*;
//... (các import khác của bạn) ...
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.net.Socket;

public class ReceiveScreen extends JFrame {

    // Sử dụng volatile để đảm bảo an toàn khi truy cập từ nhiều luồng
    private volatile BufferedImage currentImage = null;
    private volatile String statusMessage = "Đang kết nối tới server...";

    private final JPanel screenPanel;
    
    // --- THÊM MỚI: Biến để giữ tham chiếu đến cửa sổ chat ---
    private ChatWindow chatWindow; 

    /**
     * Constructor nhận vào socket đã được kết nối sẵn từ MainStart.
     * * @param dataSocket    Socket của kênh dữ liệu (port 5000).
     * @param controlSocket Socket của kênh điều khiển (port 6000).
     * @param chatSocket    Socket của kênh chat (port 7000).
     */
    public ReceiveScreen(Socket dataSocket, float width, float height, Socket controlSocket, Socket chatSocket) {
        setTitle("RemoteX Screen Viewer");
        // ... (Toàn bộ code setSize, setLocation, tính toán kích thước... của bạn giữ nguyên) ...
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
                // ... (code paintComponent của bạn, không đổi) ...
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

        // ----------------------------------------------------
        // ---- PHẦN SỬA ĐỔI ĐỂ GIỐNG ULTRAVIEW ----
        // ----------------------------------------------------
        
        // 1. Khởi tạo ChatWindow ở chế độ chạy ngầm (KHÔNG hiển thị)
        // Bỏ SwingUtilities.invokeLater
        this.chatWindow = new ChatWindow(chatSocket, "Server");

        // 2. Tạo Menu Bar
        JMenuBar menuBar = new JMenuBar();
        JMenu toolsMenu = new JMenu("Công cụ");
        JMenuItem chatMenuItem = new JMenuItem("Mở Chat");
        
        // 3. Thêm hành động khi bấm nút
        chatMenuItem.addActionListener(e -> {
            // Chỉ cần gọi hàm showWindow()
            if (this.chatWindow != null) {
                this.chatWindow.showWindow();
            }
        });

        toolsMenu.add(chatMenuItem);
        menuBar.add(toolsMenu);
        
        // 4. Thêm Menu Bar vào JFrame này
        this.setJMenuBar(menuBar);

        // ----------------------------------------------------
        
        // 5. Dời setVisible(true) xuống sau khi thêm menu
        setVisible(true); 

        // Bắt đầu một luồng mới để nhận khung hình
        new Thread(() -> receiveFrames(dataSocket)).start();
        new ControlEvent(controlSocket, screenPanel);
    }
    
    // ... (Toàn bộ các hàm receiveFrames, processFullFrame, processDeltaFrame của bạn giữ nguyên) ...
    // ... (Không cần thay đổi gì ở các hàm bên dưới) ...

    private void receiveFrames(Socket socket) {
        try (DataInputStream in = new DataInputStream(socket.getInputStream())) {

            // Đọc kích thước màn hình gốc (gửi 1 lần duy nhất)
            int screenWidth = in.readInt();
            int screenHeight = in.readInt();

            // Tạo ảnh nền ban đầu
            currentImage = new BufferedImage(screenWidth, screenHeight, BufferedImage.TYPE_INT_RGB);
            statusMessage = null; // Xóa thông báo "đang kết nối"

            while (!socket.isClosed()) {
                boolean isFullFrame = in.readBoolean();
                in.readInt(); // Đọc sequence number (tạm thời không dùng)

                if (isFullFrame) {
                    processFullFrame(in);
                } else {
                    processDeltaFrame(in);
                }

                // Yêu cầu vẽ lại màn hình SAU KHI đã có ảnh mới
                screenPanel.repaint();
            }

        } catch (Exception e) {
            e.printStackTrace();
            // Cập nhật thông báo lỗi và yêu cầu vẽ lại để hiển thị lỗi
            statusMessage = "Mất kết nối tới server: " + e.getMessage();
            currentImage = null; // Xóa ảnh cũ
            screenPanel.repaint();
        }
    }

    private void processFullFrame(DataInputStream in) throws IOException {
        int dataLength = in.readInt();
        byte[] frameData = new byte[dataLength];
        in.readFully(frameData);

        BufferedImage img = ImageIO.read(new ByteArrayInputStream(frameData));
        if (img != null) {
            // Thay thế hoàn toàn ảnh hiện tại
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

        // Vẽ ảnh delta lên ảnh hiện tại
        if (currentImage != null && deltaImg != null) {
            Graphics2D g2d = currentImage.createGraphics();
            g2d.drawImage(deltaImg, x, y, null);
            g2d.dispose();
        }
    }
}
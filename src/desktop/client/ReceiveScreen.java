package client;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
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

    /**
     * Constructor nhận vào socket đã được kết nối sẵn từ MainStart.
     * 
     * @param dataSocket Socket của kênh dữ liệu (port 5000).
     */
    public ReceiveScreen(Socket dataSocket, float width, float height, Socket controlSocket) {
        setTitle("Đang xem màn hình từ xa");
        // kiem tra kich thuoc, hop le, neu nho hon hoạc bang kic thuoc thuc te thi de
        // nguyen, neu lon hon thi thi nho lai cho dung tile

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();

        // Tỉ lệ gốc từ máy remote
        float aspectRatio = (float) width / (float) height;

        if (width > screen.width || height > screen.height) {

            if (width > screen.width) {
                width = screen.width;
                height = (int) (width / aspectRatio);
            }

            if (height > screen.height) {
                height = screen.height;
                width = (int) (height * aspectRatio);
            }
        }

        // Đặt kích thước cuối cùng
        setSize((int) width, (int) height);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        screenPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                // Vẽ ảnh nhận được
                if (currentImage != null) {
                    g.drawImage(currentImage, 0, 0, getWidth(), getHeight(), null);
                }
                // Hoặc vẽ thông báo trạng thái/lỗi
                else {
                    g.setColor(Color.BLACK);
                    g.fillRect(0, 0, getWidth(), getHeight());
                    g.setColor(Color.WHITE);
                    g.setFont(new Font("Arial", Font.BOLD, 18));
                    g.drawString(statusMessage, 50, 50);
                }
            }
        };

        add(screenPanel);
        setVisible(true);

        // Bắt đầu một luồng mới để nhận khung hình
        new Thread(() -> receiveFrames(dataSocket)).start();
        new ControlEvent(controlSocket, screenPanel);
    }

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
        int w = in.readInt();
        int h = in.readInt();
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
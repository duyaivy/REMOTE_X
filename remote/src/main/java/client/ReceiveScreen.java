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

    public ReceiveScreen(Socket dataSocket, float width, float height, Socket controlSocket, Socket chatSocket) {
        
        setTitle("RemoteX Screen Viewer");

        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        double remoteWidth = width;
        double remoteHeight = height;

        double widthRatio = screenSize.width / remoteWidth;
        double heightRatio = screenSize.height / remoteHeight;
        double scaleFactor = Math.min(widthRatio, heightRatio);

        width = (int) (remoteWidth * scaleFactor);
        height = (int) (remoteHeight * scaleFactor);

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
                    setResizable(false);
                }
            }
        };
        add(screenPanel);
        this.chatWindow = new ChatWindow(chatSocket, "Server");

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
            statusMessage = null; 

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

        } catch (java.io.EOFException e) {

            System.out.println("[CLIENT] Server đã ngắt kết nối (có thể do phát hiện mối đe dọa bảo mật)");
            statusMessage = "Server đã ngắt kết nối.\n\nCó thể do:\n- Server phát hiện hoạt động nguy hiểm\n- Kết nối không ổn định\n- Server đã tắt";
            currentImage = null;
            screenPanel.repaint();

            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(
                        this,
                        "Kết nối với server đã bị ngắt.\n\n" +
                                "Có thể do server phát hiện hoạt động nguy hiểm\n" +
                                "hoặc kết nối không ổn định.",
                        "Mất kết nối",
                        JOptionPane.WARNING_MESSAGE);
                dispose();
            });
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
         // Vẽ ảnh delta lên ảnh hiện tại
        if (currentImage != null && deltaImg != null) {
            Graphics2D g2d = currentImage.createGraphics();
            g2d.drawImage(deltaImg, x, y, null);
            g2d.dispose();
        }
    }
}
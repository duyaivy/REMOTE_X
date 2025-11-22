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

        // Lấy kích thước màn hình khả dụng (trừ taskbar)
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        GraphicsConfiguration gc = gd.getDefaultConfiguration();
        Rectangle bounds = gc.getBounds();
        Insets screenInsets = Toolkit.getDefaultToolkit().getScreenInsets(gc);

        // Tính toán kích thước cửa sổ (full màn hình nhưng không che taskbar)
        int windowX = bounds.x + screenInsets.left;
        int windowY = bounds.y + screenInsets.top;
        int windowWidth = bounds.width - screenInsets.left - screenInsets.right;
        int windowHeight = bounds.height - screenInsets.top - screenInsets.bottom;

        // Đặt vị trí và kích thước cửa sổ
        setBounds(windowX, windowY, windowWidth, windowHeight);
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

        new ControlEvent(controlSocket, screenPanel);
    }

    private void receiveFrames(Socket socket) {
        try (DataInputStream in = new DataInputStream(socket.getInputStream())) {

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

        if (currentImage != null && deltaImg != null) {
            Graphics2D g2d = currentImage.createGraphics();
            g2d.drawImage(deltaImg, x, y, null);
            g2d.dispose();
        }
    }
}
package server;

import common.ChatWindow;
import common.ScreenPacket;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

/**
 * ShareScreen với UDP thay vì TCP
 * Giữ nguyên logic delta compression, chỉ thay đổi cách truyền
 */
public class ShareScreenUDP implements Runnable {
    private static final int BLOCK_SIZE = 16;
    private static final float FULL_FRAME_THRESHOLD = 0.35f;
    private final AtomicReference<ScreenFrame> latestFrame = new AtomicReference<>();
    private static final int fps = 20;
    private static float quality = 0.5f; // Giảm xuống 0.5 để packet nhỏ hơn

    private int lastSentSeq = -1;
    private BufferedImage lastSentImage = null;

    // UDP
    private DatagramSocket udpSocket;
    private String relayHost;
    private int relayUdpPort = 5001;
    private int clientId;

    // TCP (chỉ cho Chat)
    private Socket chatSocket;
    private ChatWindow chatWindow;

    private Thread captureThread;
    private volatile boolean running = true;

    public ShareScreenUDP(String relayHost, String username, Socket chatSocket) throws Exception {
        this.relayHost = relayHost;
        this.clientId = username.hashCode();
        this.chatSocket = chatSocket;
        this.udpSocket = new DatagramSocket();

        Thread shareThread = new Thread(this);
        shareThread.setDaemon(true);
        shareThread.start();

        this.chatWindow = new ChatWindow(chatSocket, "Client");
        new ChatToggleButton(this.chatWindow);

        System.out.println("[ShareScreenUDP] Started");
        System.out.println("[ShareScreenUDP] ClientId: " + clientId);
        System.out.println("[ShareScreenUDP] Relay UDP: " + relayHost + ":" + relayUdpPort);
    }

    @Override
    public void run() {
        try {
            // Khởi động thread chụp màn hình
            captureThread = new Thread(new CaptureTask());
            captureThread.start();

            // Đợi frame đầu tiên
            ScreenFrame firstFrame;
            while ((firstFrame = latestFrame.get()) == null && running) {
                Thread.sleep(100);
            }

            if (!running) {
                System.out.println("[ShareScreenUDP] Stopped before first frame");
                return;
            }

            System.out.println("[ShareScreenUDP] ✓ Screen: " +
                    firstFrame.rawImage.getWidth() + "x" +
                    firstFrame.rawImage.getHeight());

            // Đăng ký với relay
            registerWithRelay();

            // Lắng nghe REQUEST từ relay
            listenForRequests();

        } catch (Exception e) {
            System.err.println("[ShareScreenUDP] Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            stopCapture();
            if (udpSocket != null && !udpSocket.isClosed()) {
                udpSocket.close();
            }
            System.out.println("[ShareScreenUDP] Thread ended");
        }
    }

    /**
     * Đăng ký địa chỉ UDP với relay
     */
    private void registerWithRelay() throws Exception {
        byte[] registerPacket = ScreenPacket.createRegisterPacket(clientId);

        InetAddress relayAddress = InetAddress.getByName(relayHost);
        DatagramPacket packet = new DatagramPacket(
                registerPacket, registerPacket.length,
                relayAddress, relayUdpPort);

        udpSocket.send(packet);
        System.out.println("[ShareScreenUDP] → Sent REGISTER to relay");

        // Chờ ACK
        byte[] ackBuffer = new byte[10];
        DatagramPacket ack = new DatagramPacket(ackBuffer, ackBuffer.length);
        udpSocket.setSoTimeout(3000);
        udpSocket.receive(ack);

        System.out.println("[ShareScreenUDP] ✓ Registration confirmed");
    }

    /**
     * BLOCKING - Chờ REQUEST từ relay
     */
    private void listenForRequests() {
        System.out.println("[ShareScreenUDP] ═══════════════════════════════════");
        System.out.println("[ShareScreenUDP] Waiting for REQUEST...");
        System.out.println("[ShareScreenUDP] ═══════════════════════════════════");

        while (running) {
            try {
                byte[] buffer = new byte[100];
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(packet); // BLOCKING

                // Parse request
                byte[] data = Arrays.copyOf(buffer, packet.getLength());
                ScreenPacket.PacketInfo info = ScreenPacket.parsePacket(data);

                if (info.type == ScreenPacket.TYPE_REQUEST && info.clientId == this.clientId) {
                    // Gửi frame ngay
                    sendFrame(packet.getAddress(), packet.getPort());
                }

            } catch (Exception e) {
                if (running) {
                    System.err.println("[ShareScreenUDP] Error: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Gửi frame (FULL hoặc DELTA)
     */
    private void sendFrame(InetAddress relayAddr, int relayPort) throws Exception {
        ScreenFrame currentFrame = latestFrame.get();
        if (currentFrame == null || currentFrame.sequence <= lastSentSeq) {
            return;
        }

        // Quyết định gửi FULL hay DELTA
        boolean sendFull = (lastSentImage == null);
        Rectangle changeBox = null;

        if (!sendFull) {
            changeBox = findChangeBoundingBox(lastSentImage, currentFrame.rawImage);
            if (changeBox == null) {
                // Không có thay đổi
                return;
            }

            float changedRatio = (float) (changeBox.width * changeBox.height)
                    / (lastSentImage.getWidth() * lastSentImage.getHeight());
            sendFull = (changedRatio > FULL_FRAME_THRESHOLD);
        }

        if (sendFull) {
            sendFullFrame(currentFrame, relayAddr, relayPort);
        } else {
            sendDeltaFrame(currentFrame, changeBox, relayAddr, relayPort);
        }
    }

    /**
     * Gửi FULL frame qua UDP
     */
    private void sendFullFrame(ScreenFrame frame, InetAddress addr, int port) throws Exception {
        byte[] jpgData = compressImage(frame.rawImage, quality);

        byte[] packet = ScreenPacket.createFullFramePacket(
                clientId,
                frame.sequence,
                frame.rawImage.getWidth(),
                frame.rawImage.getHeight(),
                jpgData);

        // Kiểm tra kích thước
        if (packet.length > 60000) {
            System.err.println("[ShareScreenUDP] ⚠ WARNING: Packet " + packet.length +
                    " bytes - TOO LARGE! May be dropped!");
            System.err.println("[ShareScreenUDP] ⚠ Try reducing quality or screen resolution");
        }

        DatagramPacket udpPacket = new DatagramPacket(packet, packet.length, addr, port);
        udpSocket.send(udpPacket);

        lastSentImage = frame.rawImage;
        lastSentSeq = frame.sequence;

        if (frame.sequence % 30 == 0) {
            System.out.println("[ShareScreenUDP] → FULL #" + frame.sequence +
                    " (" + packet.length / 1024 + "KB)");
        }
    }

    /**
     * Gửi DELTA frame qua UDP
     */
    private void sendDeltaFrame(ScreenFrame frame, Rectangle rect,
            InetAddress addr, int port) throws Exception {

        BufferedImage deltaImg = frame.rawImage.getSubimage(
                rect.x, rect.y, rect.width, rect.height);
        byte[] jpgData = compressImage(deltaImg, quality);

        byte[] packet = ScreenPacket.createDeltaFramePacket(
                clientId,
                frame.sequence,
                frame.rawImage.getWidth(),
                frame.rawImage.getHeight(),
                rect.x, rect.y, rect.width, rect.height,
                jpgData);

        // Kiểm tra kích thước
        if (packet.length > 60000) {
            System.err.println("[ShareScreenUDP] ⚠ DELTA packet too large: " + packet.length);
        }

        DatagramPacket udpPacket = new DatagramPacket(packet, packet.length, addr, port);
        udpSocket.send(udpPacket);

        lastSentImage = frame.rawImage;
        lastSentSeq = frame.sequence;

        if (frame.sequence % 30 == 0) {
            System.out.println("[ShareScreenUDP] → DELTA #" + frame.sequence +
                    " (" + packet.length / 1024 + "KB) " +
                    rect.width + "x" + rect.height);
        }
    }

    public void stop() {
        this.running = false;
        stopCapture();
        System.out.println("[ShareScreenUDP] Stop requested");
    }

    private void stopCapture() {
        if (captureThread != null && captureThread.isAlive()) {
            captureThread.interrupt();
            System.out.println("[ShareScreenUDP] Stopped CaptureTask");
        }
    }

    public static void setQuality(float q) {
        quality = Math.max(0.1f, Math.min(1.0f, q));
    }

    // ===== GIỮ NGUYÊN TỪ CODE CŨ =====

    static class ScreenFrame {
        final BufferedImage rawImage;
        final int sequence;

        ScreenFrame(BufferedImage rawImage, int sequence) {
            this.rawImage = rawImage;
            this.sequence = sequence;
        }
    }

    class CaptureTask implements Runnable {
        private int sequence = 0;

        public void run() {
            try {
                Robot robot = new Robot();
                Rectangle screenRect = new Rectangle(
                        Toolkit.getDefaultToolkit().getScreenSize());
                System.out.println("[CaptureTask] Started at " + fps + " FPS");
                System.out.println("[CaptureTask] Screen: " + screenRect.width + "x" + screenRect.height);

                while (!Thread.currentThread().isInterrupted() && running) {
                    try {
                        BufferedImage screen = robot.createScreenCapture(screenRect);
                        latestFrame.set(new ScreenFrame(screen, ++sequence));
                        Thread.sleep(1000 / fps);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        System.err.println("[CaptureTask] Error: " + e.getMessage());
                        Thread.sleep(1000);
                    }
                }
                System.out.println("[CaptureTask] Stopped");
            } catch (Exception e) {
                System.err.println("[CaptureTask] Init failed: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private byte[] compressImage(BufferedImage image, float quality) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(quality);

        try (MemoryCacheImageOutputStream ios = new MemoryCacheImageOutputStream(baos)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        return baos.toByteArray();
    }

    private Rectangle findChangeBoundingBox(BufferedImage oldImg, BufferedImage newImg) {
        if (oldImg == null) {
            return new Rectangle(newImg.getWidth(), newImg.getHeight());
        }

        int width = newImg.getWidth();
        int height = newImg.getHeight();
        int minX = width, minY = height, maxX = 0, maxY = 0;
        boolean changed = false;

        for (int y = 0; y < height; y += BLOCK_SIZE) {
            for (int x = 0; x < width; x += BLOCK_SIZE) {
                if (!isBlockSame(oldImg, newImg, x, y)) {
                    if (x < minX)
                        minX = x;
                    if (y < minY)
                        minY = y;
                    if (x + BLOCK_SIZE > maxX)
                        maxX = x + BLOCK_SIZE;
                    if (y + BLOCK_SIZE > maxY)
                        maxY = y + BLOCK_SIZE;
                    changed = true;
                }
            }
        }

        if (changed) {
            maxX = Math.min(width, maxX);
            maxY = Math.min(height, maxY);
            return new Rectangle(minX, minY, maxX - minX, maxY - minY);
        }
        return null;
    }

    private boolean isBlockSame(BufferedImage oldImg, BufferedImage newImg, int x, int y) {
        int endX = Math.min(x + BLOCK_SIZE, newImg.getWidth());
        int endY = Math.min(y + BLOCK_SIZE, newImg.getHeight());
        for (int j = y; j < endY; j++) {
            for (int i = x; i < endX; i++) {
                if (oldImg.getRGB(i, j) != newImg.getRGB(i, j)) {
                    return false;
                }
            }
        }
        return true;
    }
}
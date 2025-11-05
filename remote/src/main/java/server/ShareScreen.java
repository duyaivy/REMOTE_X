package server;

import common.ChatWindow;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicReference;

public class ShareScreen implements Runnable {
    private static final int BLOCK_SIZE = 16;
    private static final float FULL_FRAME_THRESHOLD = 0.35f;
    private final AtomicReference<ScreenFrame> latestFrame = new AtomicReference<>();
    private static final int fps = 20;
    private static float quality = 0.7f;

    private int lastSentSeq = -1;
    private BufferedImage lastSentImage = null;

    private Socket screenSocket = null;
    private ChatWindow chatWindow;

    public ShareScreen(Socket screenSocket, Socket chatSocket) throws Exception {
        this.screenSocket = screenSocket;
        Thread shareThread = new Thread(this);
        shareThread.setDaemon(true);
        shareThread.start();
        this.chatWindow = new ChatWindow(chatSocket, "Client");
        new ChatToggleButton(this.chatWindow);
    }

    @Override
    public void run() {
        try {
            new Thread(new CaptureTask()).start();
            try (DataOutputStream out = new DataOutputStream(screenSocket.getOutputStream())) {
                System.out.println("ip: " + screenSocket.getInetAddress());
                ScreenFrame firstFrame;
                while ((firstFrame = latestFrame.get()) == null) {
                    Thread.sleep(100);
                }
                out.writeInt(firstFrame.rawImage.getWidth());
                out.writeInt(firstFrame.rawImage.getHeight());

                sendFullFrame(out, firstFrame);
                while (!screenSocket.isClosed()) {
                    ScreenFrame currentFrame = latestFrame.get();
                    if (currentFrame != null && currentFrame.sequence > lastSentSeq) {
                        Rectangle changeBox = findChangeBoundingBox(lastSentImage, currentFrame.rawImage);
                        if (changeBox != null) {
                            float changedAreaRatio = (float) (changeBox.width * changeBox.height)
                                    / (lastSentImage.getWidth() * lastSentImage.getHeight());
                            if (changedAreaRatio > FULL_FRAME_THRESHOLD) {
                                sendFullFrame(out, currentFrame);
                            } else {
                                sendDeltaFrame(out, currentFrame, changeBox);
                            }
                        }
                    }
                    Thread.sleep(1000 / fps);
                }
            } catch (IOException e) {
                // Sửa `socket` thành `screenSocket`
                System.out.println("Client disconnected: " + screenSocket.getInetAddress());
            }
        } catch (Exception e) {
            System.err.println("ShareScreen error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                // Sửa `socket` thành `screenSocket`
                if (screenSocket != null && !screenSocket.isClosed()) {
                    screenSocket.close();
                }
            } catch (IOException e) {
                System.err.println("Error closing socket: " + e.getMessage());
            }
        }
    }

    public static void setQuality(float q) {
        quality = Math.max(0.1f, Math.min(1.0f, q));
    }

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
                Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        BufferedImage screen = robot.createScreenCapture(screenRect);
                        latestFrame.set(new ScreenFrame(screen, ++sequence));
                        Thread.sleep(1000 / fps);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    } catch (Exception e) {
                        System.err.println("Error capturing screen: " + e.getMessage());
                        Thread.sleep(1000); // Wait before retry
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to initialize screen capture: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private void sendFullFrame(DataOutputStream out, ScreenFrame frame) throws IOException {
        byte[] compressedData = compressImage(frame.rawImage, quality);

        out.writeBoolean(true); // isFullFrame = true
        out.writeInt(frame.sequence);
        out.writeInt(compressedData.length);
        out.write(compressedData);
        out.flush();

        this.lastSentImage = frame.rawImage;
        this.lastSentSeq = frame.sequence;
        if (frame.sequence % 30 == 0) {
            System.out.println("FULL " + frame.sequence + " (" + compressedData.length / 1024 + " KB) cho "
                    + screenSocket.getInetAddress());
        }
    }

    private void sendDeltaFrame(DataOutputStream out, ScreenFrame frame, Rectangle rect) throws IOException {

        BufferedImage deltaImage = frame.rawImage.getSubimage(rect.x, rect.y, rect.width, rect.height);
        byte[] compressedData = compressImage(deltaImage, quality);

        out.writeBoolean(false); // isFullFrame = false
        out.writeInt(frame.sequence);

        out.writeInt(rect.x);
        out.writeInt(rect.y);
        out.writeInt(rect.width);
        out.writeInt(rect.height);

        out.writeInt(compressedData.length);
        out.write(compressedData);
        out.flush();

        this.lastSentImage = frame.rawImage;
        this.lastSentSeq = frame.sequence;
        if (frame.sequence % 30 == 0) {
            System.out.println("DELTA " + frame.sequence + " (" + compressedData.length / 1024 + " KB) cho "
                    + screenSocket.getInetAddress());
        }
    }

    // Các hàm compressImage, findChangeBoundingBox, isBlockSame không thay đổi...
    private byte[] compressImage(BufferedImage image, float quality) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        // SỬA LẠI:
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
                    return false; // Tìm thấy pixel khác nhau.
                }
            }
        }
        return true;
    }
}
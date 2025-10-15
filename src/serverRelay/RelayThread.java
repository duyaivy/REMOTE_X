package serverRelay;

// package serverRelay;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class RelayThread extends Thread {
    private final InputStream inputStream;
    private final OutputStream outputStream;
    private final Socket fromSocket;
    private final Socket toSocket;

    public RelayThread(Socket fromSocket, Socket toSocket) throws Exception {
        this.fromSocket = fromSocket;
        this.toSocket = toSocket;
        this.inputStream = fromSocket.getInputStream();
        this.outputStream = toSocket.getOutputStream();
    }

    @Override
    public void run() {
        byte[] buffer = new byte[16384]; // 16KB buffer
        int bytesRead;
        try {
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                outputStream.flush();
            }
        } catch (Exception e) {
            // Thường là do một trong hai socket đã đóng
            System.out.println("Relay thread terminated: " + e.getMessage());
        } finally {
            try {
                fromSocket.close();
            } catch (Exception e) {
            }
            try {
                toSocket.close();
            } catch (Exception e) {
            }
        }
    }
}
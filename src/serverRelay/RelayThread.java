package serverRelay;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class RelayThread extends Thread {
    private final InputStream inputStream;
    private final OutputStream outputStream;

    public RelayThread(InputStream in, OutputStream out) {
        this.inputStream = in;
        this.outputStream = out;
    }

    @Override
    public void run() {
        byte[] buffer = new byte[65536];
        int bytesRead;
        try {
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                outputStream.flush();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                inputStream.close();
            } catch (IOException e) {
            }
            try {
                outputStream.close();
            } catch (IOException e) {
            }
        }
    }
}
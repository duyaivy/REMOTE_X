package server;

import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import javax.swing.JOptionPane;

public class InitConnection {
    ServerSocket socket = null;
    DataInputStream password = null;
    DataOutputStream verify = null;
    String width = "", height = "";

    InitConnection(int port, String passwordValue) {
        Robot robot = null;
        Rectangle rectangle = null;
        try {
            System.out.println("Waiting for client to get connected...");
            socket = new ServerSocket(port);
            GraphicsEnvironment gEnv = GraphicsEnvironment.getLocalGraphicsEnvironment();
            GraphicsDevice gDev = gEnv.getDefaultScreenDevice();
            Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
            String width = "" + dim.getWidth();
            String height = "" + dim.getHeight();
            rectangle = new Rectangle(dim);
            robot = new Robot(gDev);

            drawGUI();

            while (true) {
                Socket sc = socket.accept();
                password = new DataInputStream(sc.getInputStream());
                verify = new DataOutputStream(sc.getOutputStream());

                String passwordInput = password.readUTF();
                if (passwordInput.equals(passwordValue)) {
                    verify.writeUTF("valid");
                    verify.writeUTF(width);
                    verify.writeUTF(height);
                    new SendScreen(sc, robot, rectangle);
                    new ReceiveEvents(sc, robot);
                } else {
                    verify.writeUTF("invalid");
                }
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "An error occurred: " + ex.getMessage(), "Error",
                    JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
    }

    private void drawGUI() {

    }
}

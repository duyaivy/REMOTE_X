package server;

import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

public class SendScreen extends Thread {
    Socket socket = null;
    Robot robot = null;
    Rectangle rectangle = null;
    boolean continueLoop = true;
    OutputStream oos = null;

    public SendScreen(Socket socket, Robot robot, Rectangle rectangle) {
        this.socket = socket;
        this.robot = robot;
        this.rectangle = rectangle;
        start();
    }

    public void run() {
        try {
            oos = socket.getOutputStream();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        while (continueLoop) {
            BufferedImage image = robot.createScreenCapture(rectangle);
            try {
                ImageIO.write(image, "jpeg", oos);
            } catch (IOException ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null, "An error occurred", "Error",
                        JOptionPane.ERROR_MESSAGE);
            }

            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                JOptionPane.showMessageDialog(null, "An error occurred", "Error",
                        JOptionPane.ERROR_MESSAGE);
                ex.printStackTrace();
            }
        }
    }
}

package client;

import java.net.Socket;
import java.net.UnknownHostException;

import javax.swing.JOptionPane;

public class Start {
    static String port = "8080";

    public static void main(String[] args) {
        String ip = JOptionPane.showInputDialog("Please enter server IP:");
        new Start().initialize(ip, Integer.parseInt(port));
    }

    public void initialize(String ip, int port) {
        try {
            Socket sc = new Socket(ip, port);
            System.out.println("Connected to server");
            Authentication frame1 = new Authentication(sc);
            frame1.setSize(300, 80);
            frame1.setLocation(500, 300);
            frame1.setVisible(true);
        } catch (UnknownHostException e) {
            JOptionPane.showMessageDialog(null,
                    "Could not connect to server. Please check the IP address and try again.", "Connection Error",
                    JOptionPane.ERROR_MESSAGE);
            String newIp = JOptionPane.showInputDialog("Please enter a valid server IP:");
            initialize(newIp, port);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Could not connect to server. Please check the IP address and try again.", "Connection Error",
                    JOptionPane.ERROR_MESSAGE);
            String newIp = JOptionPane.showInputDialog("Please enter a valid server IP:");
            initialize(newIp, port);
        }
    }
}

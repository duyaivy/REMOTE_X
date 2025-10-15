package client;

import java.awt.BorderLayout;
import java.beans.PropertyVetoException;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

import javax.swing.*;
import java.util.zip.*;

public class CreateFrame extends Thread {
    String width = "", height = "";
    private JFrame frame = new JFrame();
    private JDesktopPane desktop = new JDesktopPane();
    private Socket cSocket = null;
    private JInternalFrame interFrame = new JInternalFrame("Server Screen", true, true, true);
    private JPanel cPanel = new JPanel();

    public CreateFrame(Socket sSocket, String width, String height) {
        this.cSocket = sSocket;
        this.width = width;
        this.height = height;
        start();
    }

    public void drawGUI() {
        frame.add(desktop, BorderLayout.CENTER);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        frame.setExtendedState(frame.getExtendedState() | JFrame.MAXIMIZED_BOTH);
        frame.setVisible(true);
        interFrame.setLayout(new BorderLayout());
        interFrame.getContentPane().add(cPanel, BorderLayout.CENTER);
        interFrame.setSize(100, 100);
        desktop.add(interFrame);

        try

        {
            interFrame.setMaximum(true);
        } catch (PropertyVetoException ex) {
            ex.printStackTrace();
        }
        cPanel.setFocusable(true);
        interFrame.setVisible(true);
    }

    public void run() {
        InputStream in = null;
        drawGUI();
        try {
            in = cSocket.getInputStream();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        new ReceivingScreen(in, cPanel);
        new SendEvents(cSocket, cPanel, width, height);
    }
}

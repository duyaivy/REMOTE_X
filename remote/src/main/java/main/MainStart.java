package main;

import javax.swing.*;
import javax.swing.border.*;
import client.ReceiveScreen;
import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.awt.Robot; 
import server.ReceiveEvent;
import server.ShareScreen;

public class MainStart extends JFrame {

    private static final int CHAT_PORT = 7000;

    public MainStart(String ipServer) {
        setTitle("Remote X");
        setSize(800, 500);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(1, 2));

        JPanel leftPanel = new JPanel(new GridBagLayout());
        leftPanel.setBorder(new TitledBorder(new EtchedBorder(), "Cho phép điều khiển"));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        JLabel lblIDBan = new JLabel("ID của bạn");
        JTextField txtIDBan = new JTextField("", 15);
        JLabel lblMatKhau = new JLabel("Mật khẩu");
        JTextField txtMatKhau = new JTextField("", 15);
        JButton btnStartShare = new JButton("Cho phép điều khiển");
        btnStartShare.addActionListener(e -> {
            String username = txtIDBan.getText().trim();
            String password = txtMatKhau.getText().trim();
            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập ID và mật khẩu.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            } else {
                btnStartShare.setEnabled(false);
                btnStartShare.setText("Đang kết nối...");
                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                    private Socket socketScreen;
                    private Socket socketControl;
                    private Socket socketChat; 
                    private GraphicsEnvironment gEnv = GraphicsEnvironment.getLocalGraphicsEnvironment();
                    private GraphicsDevice gDev = gEnv.getDefaultScreenDevice();

                    @Override
                    protected Void doInBackground() throws Exception {

                        try {
                            socketScreen = new Socket(ipServer, 5000);
                            socketControl = new Socket(ipServer, 6000);
                            socketChat = new Socket(ipServer, CHAT_PORT); 

                            String initMessage = username + "," + password + ",sharer," + screen.getWidth() + "," + screen.getHeight();

                            DataOutputStream dosScreen = new DataOutputStream(socketScreen.getOutputStream());
                            DataOutputStream dosControl = new DataOutputStream(socketControl.getOutputStream());
                            DataOutputStream dosChat = new DataOutputStream(socketChat.getOutputStream()); 

                            dosScreen.writeUTF(initMessage + ",screen");
                            dosControl.writeUTF(initMessage + ",control");
                            dosChat.writeUTF(initMessage + ",chat");

                            dosScreen.flush();
                            dosControl.flush();
                            dosChat.flush(); 

                            DataInputStream disScreen = new DataInputStream(socketScreen.getInputStream());
                            DataInputStream disControl = new DataInputStream(socketControl.getInputStream());
                            DataInputStream disChat = new DataInputStream(socketChat.getInputStream()); 

                            String responseScreen = disScreen.readUTF();
                            String responseControl = disControl.readUTF();
                            String responseChat = disChat.readUTF(); 

                            if (responseScreen.startsWith("false") || responseControl.startsWith("false") || responseChat.startsWith("false")) {
                                throw new Exception("Server response: " + responseScreen + " | " + responseControl + " | " + responseChat);
                            }
                        } catch (Exception e) {
                            try { if (socketScreen != null) socketScreen.close(); } catch (Exception ex) {}
                            try { if (socketControl != null) socketControl.close(); } catch (Exception ex) {}
                            try { if (socketChat != null) socketChat.close(); } catch (Exception ex) {} 
                            throw e; 
                        }
                        return null;
                    }
                    
                    @Override
                    protected void done() {
                        try {
                            get(); 
                            JOptionPane.showMessageDialog(MainStart.this, "Kết nối thành công! Đang chờ đối tác...");
                            btnStartShare.setText("Đang chia sẻ..."); 
                            Robot rb = new Robot(gDev);
                            new ReceiveEvent(socketControl, socketScreen, socketChat, rb, screen.height, screen.width, btnStartShare);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(MainStart.this, "Không thể kết nối đến server: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);

                            try { if (socketScreen != null) socketScreen.close(); } catch (Exception e) {}
                            try { if (socketControl != null) socketControl.close(); } catch (Exception e) {}
                            try { if (socketChat != null) socketChat.close(); } catch (Exception e) {}

                            btnStartShare.setEnabled(true);
                            btnStartShare.setText("Cho phép điều khiển");
                        } 
                    }
                };
                worker.execute();
            }
        });
        gbc.gridx = 0; gbc.gridy = 0; leftPanel.add(lblIDBan, gbc);
        gbc.gridy = 1; leftPanel.add(txtIDBan, gbc);
        gbc.gridy = 2; leftPanel.add(lblMatKhau, gbc);
        gbc.gridy = 3; leftPanel.add(txtMatKhau, gbc);
        gbc.gridy = 4; leftPanel.add(btnStartShare, gbc);

        JPanel rightPanel = new JPanel(new GridBagLayout());
        rightPanel.setBorder(new TitledBorder(new EtchedBorder(), "Điều khiển máy tính khác"));
        GridBagConstraints gbc2 = new GridBagConstraints();
        gbc2.insets = new Insets(5, 5, 5, 5);
        gbc2.anchor = GridBagConstraints.WEST;
        gbc2.fill = GridBagConstraints.HORIZONTAL;
        JLabel lblIDDT = new JLabel("ID đối tác");
        JTextField txtIDDT = new JTextField(15);
        JLabel lblMKDT = new JLabel("Mật khẩu");
        JTextField txtMKDT = new JTextField(15);
        JButton btnStart = new JButton("Bắt đầu điều khiển");

        btnStart.addActionListener(e -> {
            String partnerID = txtIDDT.getText().trim();
            String partnerPassword = txtMKDT.getText().trim();
            if (partnerID.isEmpty() || partnerPassword.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập ID và mật khẩu đối tác.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            btnStart.setEnabled(false);
            btnStart.setText("Đang kết nối...");

            SwingWorker<Void, Void> controlWorker = new SwingWorker<Void, Void>() {
                private Socket socketScreen;
                private Socket socketControl;
                private Socket socketChat;
                private float remoteWidth;
                private float remoteHeight;
                private GraphicsEnvironment gEnv = GraphicsEnvironment.getLocalGraphicsEnvironment();
                private GraphicsDevice gDev = gEnv.getDefaultScreenDevice();

                @Override
                protected Void doInBackground() throws Exception {
                    try {
                        socketScreen = new Socket(ipServer, 5000);
                        socketControl = new Socket(ipServer, 6000);
                        socketChat = new Socket(ipServer, CHAT_PORT); 

                        String initMessage = partnerID + "," + partnerPassword + ",viewer," + screen.getWidth() + "," + screen.getHeight();

                        DataOutputStream dosScreen = new DataOutputStream(socketScreen.getOutputStream());
                        DataOutputStream dosControl = new DataOutputStream(socketControl.getOutputStream());
                        DataOutputStream dosChat = new DataOutputStream(socketChat.getOutputStream()); 

                        dosScreen.writeUTF(initMessage + ",screen");
                        dosControl.writeUTF(initMessage + ",control");
                        dosChat.writeUTF(initMessage + ",chat"); 
                        dosScreen.flush(); dosControl.flush(); dosChat.flush(); 


                        DataInputStream disScreen = new DataInputStream(socketScreen.getInputStream());
                        DataInputStream disControl = new DataInputStream(socketControl.getInputStream()); 
                        DataInputStream disChat = new DataInputStream(socketChat.getInputStream()); 
                        String responseScreen = disScreen.readUTF();
                        String responseControl = disControl.readUTF(); 
                        String responseChat = disChat.readUTF(); 


                        if (responseScreen.startsWith("false") || responseControl.startsWith("false") || responseChat.startsWith("false")) {
                            throw new Exception("Server response: " + responseScreen + " | " + responseControl + " | " + responseChat);
                        }
                        String[] res = responseScreen.split(",");
                        if (res.length < 3) {
                            throw new Exception("Lỗi dữ liệu trả về: " + responseScreen);
                        }
                        remoteWidth = Float.parseFloat(res[1]);
                        remoteHeight = Float.parseFloat(res[2]);
                    } catch (Exception ex) {
                        try { if (socketScreen != null) socketScreen.close(); } catch (Exception e) {}
                        try { if (socketControl != null) socketControl.close(); } catch (Exception e) {}
                        try { if (socketChat != null) socketChat.close(); } catch (Exception e) {} 
                        throw ex;
                    }
                    return null;
                }

                @Override
                protected void done() {
                    try {
                        get();
                        JOptionPane.showMessageDialog(MainStart.this, "Kết nối thành công! Bắt đầu điều khiển.");
                        
                
                        new ReceiveScreen(socketScreen, remoteWidth, remoteHeight, socketControl, socketChat);
                        
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        JOptionPane.showMessageDialog(MainStart.this, "Không thể kết nối đến đối tác: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                    } finally {
                        btnStart.setEnabled(true);
                        btnStart.setText("Bắt đầu điều khiển");
                    }
                }
            };
            controlWorker.execute();
        });

        gbc2.gridx = 0; gbc2.gridy = 0; rightPanel.add(lblIDDT, gbc2);
        gbc2.gridy = 1; rightPanel.add(txtIDDT, gbc2);
        gbc2.gridy = 2; rightPanel.add(lblMKDT, gbc2);
        gbc2.gridy = 3; rightPanel.add(txtMKDT, gbc2);
        gbc2.gridy = 4; rightPanel.add(btnStart, gbc2);

        add(leftPanel);
        add(rightPanel);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                String ipServer = "localhost"; 
                MainStart main = new MainStart(ipServer);
                main.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Không thể khởi tạo ứng dụng: " + e.getMessage(), "Lỗi khởi tạo", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
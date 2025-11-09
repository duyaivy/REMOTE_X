package main;

import javax.swing.*;
import javax.swing.border.*;

import client.ReceiveScreen;
import monitor.MonitoringManager; // <-- THÊM IMPORT TỪ BẢN MỚI

import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.awt.Robot; 
import server.ReceiveEvent;
import server.ShareScreen;

public class MainStart extends JFrame {

    private static final int CHAT_PORT = 7000;

    // --- CÁC BIẾN STYLE MỚI (Giống UltraViewer) ---
    private final Color COLOR_TITLE = new Color(0, 114, 188); // Màu xanh dương
    private final Font FONT_TITLE = new Font("Arial", Font.BOLD, 16);
    private final Font FONT_LABEL = new Font("Arial", Font.PLAIN, 12);
    private final Font FONT_FIELD = new Font("Arial", Font.BOLD, 14);
    // --- KẾT THÚC STYLE ---

    public MainStart(String ipServer) {
        setTitle("Remote X");
        setSize(800, 500); 
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(1, 2, 10, 10));

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();

        // -----------------------------------------------------------------
        // --- BÊN TRÁI (CHO PHÉP ĐIỀU KHIỂN - SHARER) ---
        // (Đã gộp UI từ cả 2 bản)
        // -----------------------------------------------------------------
        
        JPanel leftPanel = new JPanel(new GridBagLayout());
        leftPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15)); 
        
        // --- KHAI BÁO gbcLeft (tên mới của gbc) ---
        GridBagConstraints gbcLeft = new GridBagConstraints();
        gbcLeft.insets = new Insets(8, 5, 8, 5); 
        gbcLeft.fill = GridBagConstraints.HORIZONTAL; 

        // --- Tiêu đề (Từ HEAD) ---
        JLabel lblTitleLeft = new JLabel("Cho phép điều khiển");
        lblTitleLeft.setFont(FONT_TITLE);
        lblTitleLeft.setForeground(COLOR_TITLE);
        gbcLeft.gridx = 0;
        gbcLeft.gridy = 0;
        gbcLeft.gridwidth = 2; 
        gbcLeft.anchor = GridBagConstraints.CENTER;
        leftPanel.add(lblTitleLeft, gbcLeft);

        // --- Mô tả (Từ HEAD) ---
        JLabel lblDescLeft = new JLabel("Gửi ID và Mật khẩu này cho đối tác của bạn.");
        lblDescLeft.setFont(FONT_LABEL);
        gbcLeft.gridy = 1;
        leftPanel.add(lblDescLeft, gbcLeft);
        
        // --- Reset GBC cho form (Từ HEAD) ---
        gbcLeft.gridwidth = 1;
        gbcLeft.anchor = GridBagConstraints.WEST;

        // --- Hàng 1: ID (Từ HEAD) ---
        JLabel lblIDBan = new JLabel("ID của bạn");
        lblIDBan.setFont(FONT_LABEL);
        gbcLeft.gridx = 0;
        gbcLeft.gridy = 2;
        gbcLeft.fill = GridBagConstraints.NONE;
        leftPanel.add(lblIDBan, gbcLeft);

        JTextField txtIDBan = new JTextField("", 15);
        txtIDBan.setFont(FONT_FIELD);
        gbcLeft.gridx = 1;
        gbcLeft.gridy = 2;
        gbcLeft.fill = GridBagConstraints.HORIZONTAL; 
        leftPanel.add(txtIDBan, gbcLeft);

        // --- Hàng 2: Mật khẩu (Từ HEAD) ---
        JLabel lblMatKhau = new JLabel("Mật khẩu");
        lblMatKhau.setFont(FONT_LABEL);
        gbcLeft.gridx = 0;
        gbcLeft.gridy = 3;
        gbcLeft.fill = GridBagConstraints.NONE;
        leftPanel.add(lblMatKhau, gbcLeft);

        // --- Dùng JPasswordField (Từ HEAD) ---
        JPasswordField txtMatKhau = new JPasswordField("", 15); 
        txtMatKhau.setFont(FONT_FIELD);
        gbcLeft.gridx = 1;
        gbcLeft.gridy = 3;
        gbcLeft.fill = GridBagConstraints.HORIZONTAL;
        leftPanel.add(txtMatKhau, gbcLeft);

        // --- Hàng 4: Checkbox (Từ 43a...) ---
        JCheckBox chkMonitoring = new JCheckBox("Bật giám sát bảo mật (AI)", true);
        chkMonitoring.setToolTipText("Giám sát các hoạt động nguy hiểm trên máy bằng AI");
        gbcLeft.gridx = 0;
        gbcLeft.gridy = 4;
        gbcLeft.gridwidth = 2;
        gbcLeft.anchor = GridBagConstraints.CENTER;
        leftPanel.add(chkMonitoring, gbcLeft);

        // --- Hàng 5: Nút bấm (Từ HEAD, sửa gridy) ---
        JButton btnStartShare = new JButton("Cho phép điều khiển");
        btnStartShare.setFont(FONT_LABEL);
        gbcLeft.gridx = 0;
        gbcLeft.gridy = 5; // Sửa gridy
        gbcLeft.gridwidth = 2;
        gbcLeft.fill = GridBagConstraints.NONE; 
        gbcLeft.anchor = GridBagConstraints.CENTER;
        leftPanel.add(btnStartShare, gbcLeft);
        
        // --- LOGIC (ĐÃ GỘP) ---
        btnStartShare.addActionListener(e -> {
            String username = txtIDBan.getText().trim();
            
            // Lấy password từ JPasswordField (Từ HEAD)
            String password = new String(txtMatKhau.getPassword()).trim(); 
            
            // Lấy trạng thái checkbox (Từ 43a...)
            boolean enableMonitoring = chkMonitoring.isSelected(); 

            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập ID và mật khẩu.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            } else {
                btnStartShare.setEnabled(false);
                btnStartShare.setText("Đang kết nối...");
                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                    // Giữ logic kết nối 3 socket (Từ HEAD)
                    private Socket socketScreen, socketControl, socketChat; 
                    private GraphicsEnvironment gEnv = GraphicsEnvironment.getLocalGraphicsEnvironment();
                    private GraphicsDevice gDev = gEnv.getDefaultScreenDevice();

                    @Override
                    protected Void doInBackground() throws Exception {
                        // Giữ logic doInBackground (Từ HEAD)
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
                        // Gộp logic done()
                        try {
                            get(); 
                            JOptionPane.showMessageDialog(MainStart.this, "Kết nối thành công! Đang chờ đối tác...");
                            btnStartShare.setText("Đang chia sẻ..."); // Từ HEAD
                            
                            // Thêm logic MonitoringManager (Từ 43a...)
                            if (enableMonitoring) {
                                new Thread(() -> {
                                    try {
                                        MonitoringManager.getInstance().startMonitoring(socketScreen, socketChat,
                                                socketControl);
                                    } catch (Exception monitorEx) {
                                        JOptionPane.showMessageDialog(MainStart.this,
                                                "Lỗi khi khởi tạo giám sát AI: " + monitorEx.getMessage(),
                                                "Lỗi", JOptionPane.ERROR_MESSAGE);
                                        monitorEx.printStackTrace();
                                    }
                                }).start();
                            } else {
                                System.out.println("[MAIN] Monitoring disabled by user");
                            }
                            
                            // Giữ logic gọi ReceiveEvent (Từ HEAD)
                            Robot rb = new Robot(gDev);
                            new ReceiveEvent(socketControl, socketScreen, socketChat, rb, screen.height, screen.width, btnStartShare);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(MainStart.this, "Không thể kết nối đến server: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                            // Giữ logic dọn dẹp (Từ HEAD)
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

        // --- KHÔNG CÓ CODE LỖI (gbc) Ở ĐÂY NỮA ---

        // -----------------------------------------------------------------
        // --- BÊN PHẢI (ĐIỀU KHIỂN - VIEWER) ---
        // (Giữ UI từ HEAD, Logic từ HEAD)
        // -----------------------------------------------------------------

        JPanel rightPanel = new JPanel(new GridBagLayout());
        rightPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        // --- KHAI BÁO gbcRight (tên mới của gbc2) ---
        GridBagConstraints gbcRight = new GridBagConstraints();
        gbcRight.insets = new Insets(8, 5, 8, 5);
        gbcRight.fill = GridBagConstraints.HORIZONTAL;

        JLabel lblTitleRight = new JLabel("Điều khiển máy tính khác");
        lblTitleRight.setFont(FONT_TITLE);
        lblTitleRight.setForeground(COLOR_TITLE);
        gbcRight.gridx = 0;
        gbcRight.gridy = 0;
        gbcRight.gridwidth = 2;
        gbcRight.anchor = GridBagConstraints.CENTER;
        rightPanel.add(lblTitleRight, gbcRight);

        JLabel lblDescRight = new JLabel("Nhập ID và Mật khẩu của máy bạn cần điều khiển.");
        lblDescRight.setFont(FONT_LABEL);
        gbcRight.gridy = 1;
        rightPanel.add(lblDescRight, gbcRight);

        gbcRight.gridwidth = 1;
        gbcRight.anchor = GridBagConstraints.WEST;

        JLabel lblIDDT = new JLabel("ID đối tác");
        lblIDDT.setFont(FONT_LABEL);
        gbcRight.gridx = 0;
        gbcRight.gridy = 2;
        gbcRight.fill = GridBagConstraints.NONE;
        rightPanel.add(lblIDDT, gbcRight);

        JTextField txtIDDT = new JTextField(15);
        txtIDDT.setFont(FONT_FIELD);
        gbcRight.gridx = 1;
        gbcRight.gridy = 2;
        gbcRight.fill = GridBagConstraints.HORIZONTAL;
        rightPanel.add(txtIDDT, gbcRight);

        JLabel lblMKDT = new JLabel("Mật khẩu");
        lblMKDT.setFont(FONT_LABEL);
        gbcRight.gridx = 0;
        gbcRight.gridy = 3;
        gbcRight.fill = GridBagConstraints.NONE;
        rightPanel.add(lblMKDT, gbcRight);

        JPasswordField txtMKDT = new JPasswordField(15);
        txtMKDT.setFont(FONT_FIELD);
        gbcRight.gridx = 1;
        gbcRight.gridy = 3;
        gbcRight.fill = GridBagConstraints.HORIZONTAL;
        rightPanel.add(txtMKDT, gbcRight);

        JButton btnStart = new JButton("Bắt đầu điều khiển");
        btnStart.setFont(FONT_LABEL);
        gbcRight.gridx = 0;
        gbcRight.gridy = 4;
        gbcRight.gridwidth = 2;
        gbcRight.fill = GridBagConstraints.NONE;
        gbcRight.anchor = GridBagConstraints.CENTER;
        rightPanel.add(btnStart, gbcRight);

        // --- LOGIC (Giữ logic từ HEAD để sửa lỗi "tín hiệu lạ") ---
        btnStart.addActionListener(e -> {
            String partnerID = txtIDDT.getText().trim();
            String partnerPassword = new String(txtMKDT.getPassword()).trim(); 

            if (partnerID.isEmpty() || partnerPassword.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập ID và mật khẩu đối tác.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            btnStart.setEnabled(false);
            btnStart.setText("Đang kết nối...");

            SwingWorker<Void, Void> controlWorker = new SwingWorker<Void, Void>() {
                // Giữ logic 6 tham số (Từ HEAD)
                private Socket socketScreen, socketControl, socketChat;
                private DataInputStream disControl; // Giữ luồng đọc
                private float remoteWidth, remoteHeight;
                // --- THÊM KHAI BÁO (Từ 43a...) ---
                private GraphicsEnvironment gEnv = GraphicsEnvironment.getLocalGraphicsEnvironment();
                private GraphicsDevice gDev = gEnv.getDefaultScreenDevice();


                @Override
                protected Void doInBackground() throws Exception {
                    // Giữ logic 6 tham số (Từ HEAD)
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
                        this.disControl = new DataInputStream(socketControl.getInputStream()); 
                        DataInputStream disChat = new DataInputStream(socketChat.getInputStream()); 
                        
                        String responseScreen = disScreen.readUTF();
                        String responseControl = this.disControl.readUTF(); 
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
                    // Giữ logic 6 tham số (Từ HEAD)
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

        // -----------------------------------------------------------------
        // --- THÊM 2 PANEL VÀO FRAME CHÍNH ---
        // -----------------------------------------------------------------
        add(leftPanel);
        add(rightPanel);
    }

    // --- MAIN (GỘP CẢ HAI) ---
    public static void main(String[] args) {

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            try {
                // Giữ 'localhost' (Từ HEAD) vì nó an toàn hơn
                String ipServer = "localhost"; 
                // String ipServer = "192.168.2.95"; // (Từ 43a...)
                
                MainStart main = new MainStart(ipServer);
                main.setVisible(true);
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(null,
                        "Không thể khởi tạo ứng dụng: " + e.getMessage(),
                        "Lỗi khởi tạo", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
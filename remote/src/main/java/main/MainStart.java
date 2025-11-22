package main;

import javax.swing.*;
import client.ReceiveScreen;
import io.github.cdimascio.dotenv.Dotenv;
import monitor.MonitoringManager;
import java.awt.*;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;

import server.ReceiveEvent;

public class MainStart extends JFrame {

    private static final int CHAT_PORT = 7000;

    public MainStart(String ipServer) {
        setTitle("Remote X");
        setSize(900, 520);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        getContentPane().setBackground(UIHelper.COLOR_BACKGROUND);
        setLayout(new BorderLayout(0, 0));

        setJMenuBar(UIHelper.createStyledMenuBar());

        JPanel mainPanel = new JPanel(new GridLayout(1, 2, 15, 0));
        mainPanel.setBackground(UIHelper.COLOR_BACKGROUND);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();

        JPanel leftPanel = UIHelper.createStyledPanel();

        JPanel leftHeader = UIHelper.createHeaderPanel("Cho phép điều khiển", UIHelper.createAntennaIcon());
        leftPanel.add(leftHeader, BorderLayout.NORTH);

        JPanel leftContent = new JPanel(new GridBagLayout());
        leftContent.setBackground(Color.WHITE);
        leftContent.setBorder(BorderFactory.createEmptyBorder(15, 20, 20, 20));
        GridBagConstraints gbcLeft = new GridBagConstraints();
        gbcLeft.insets = new Insets(5, 5, 5, 5);
        gbcLeft.fill = GridBagConstraints.HORIZONTAL;
        gbcLeft.anchor = GridBagConstraints.WEST;

        JLabel lblDescLeft = new JLabel("<html>Gửi ID và Mật khẩu này cho đối tác của bạn.</html>");
        lblDescLeft.setFont(UIHelper.FONT_LABEL);
        lblDescLeft.setForeground(UIHelper.COLOR_TEXT_DARK);
        gbcLeft.gridx = 0;
        gbcLeft.gridy = 0;
        gbcLeft.gridwidth = 2;
        gbcLeft.insets = new Insets(0, 5, 20, 5);
        leftContent.add(lblDescLeft, gbcLeft);

        gbcLeft.gridwidth = 1;
        gbcLeft.insets = new Insets(5, 5, 5, 5);

        JLabel lblIDBan = new JLabel("ID của bạn");
        lblIDBan.setFont(UIHelper.FONT_LABEL);
        lblIDBan.setForeground(UIHelper.COLOR_TEXT_DARK);
        gbcLeft.gridx = 0;
        gbcLeft.gridy = 1;
        gbcLeft.fill = GridBagConstraints.NONE;
        gbcLeft.weightx = 0.0;
        leftContent.add(lblIDBan, gbcLeft);

        JTextField txtIDBan = new JTextField("", 15);
        txtIDBan.setFont(UIHelper.FONT_FIELD);
        UIHelper.styleTextField(txtIDBan);
        gbcLeft.gridx = 1;
        gbcLeft.gridy = 1;
        gbcLeft.fill = GridBagConstraints.HORIZONTAL;
        gbcLeft.weightx = 1.0;
        leftContent.add(txtIDBan, gbcLeft);

        JLabel lblMatKhau = new JLabel("Mật khẩu");
        lblMatKhau.setFont(UIHelper.FONT_LABEL);
        lblMatKhau.setForeground(UIHelper.COLOR_TEXT_DARK);
        gbcLeft.gridx = 0;
        gbcLeft.gridy = 2;
        gbcLeft.fill = GridBagConstraints.NONE;
        gbcLeft.weightx = 0.0;
        leftContent.add(lblMatKhau, gbcLeft);

        JPasswordField txtMatKhau = new JPasswordField("", 15);
        txtMatKhau.setFont(UIHelper.FONT_FIELD);
        UIHelper.styleTextField(txtMatKhau);
        gbcLeft.gridx = 1;
        gbcLeft.gridy = 2;
        gbcLeft.fill = GridBagConstraints.HORIZONTAL;
        gbcLeft.weightx = 1.0;
        leftContent.add(txtMatKhau, gbcLeft);

        JCheckBox chkMonitoring = new JCheckBox("Bật giám sát bảo mật (AI)", true);
        chkMonitoring.setToolTipText("Giám sát các hoạt động nguy hiểm trên máy bằng AI");
        chkMonitoring.setFont(UIHelper.FONT_LABEL);
        chkMonitoring.setBackground(Color.WHITE);
        chkMonitoring.setForeground(UIHelper.COLOR_TEXT_DARK);
        gbcLeft.gridx = 0;
        gbcLeft.gridy = 3;
        gbcLeft.gridwidth = 2;
        gbcLeft.anchor = GridBagConstraints.WEST;
        gbcLeft.fill = GridBagConstraints.NONE;
        gbcLeft.weightx = 0.0;
        gbcLeft.insets = new Insets(15, 5, 15, 5);
        leftContent.add(chkMonitoring, gbcLeft);

        JPanel spacerLeft = new JPanel();
        spacerLeft.setOpaque(false);
        gbcLeft.gridy = 4;
        gbcLeft.weighty = 1.0;
        leftContent.add(spacerLeft, gbcLeft);

        JButton btnStartShare = UIHelper.createStyledButton("Cho phép điều khiển");
        gbcLeft.gridx = 0;
        gbcLeft.gridy = 5;
        gbcLeft.gridwidth = 2;
        gbcLeft.fill = GridBagConstraints.NONE;
        gbcLeft.anchor = GridBagConstraints.CENTER;
        gbcLeft.weighty = 0.0;
        gbcLeft.insets = new Insets(0, 5, 10, 5);
        leftContent.add(btnStartShare, gbcLeft);

        leftPanel.add(leftContent, BorderLayout.CENTER);

        btnStartShare.addActionListener(e -> {
            String username = txtIDBan.getText().trim();

            String password = new String(txtMatKhau.getPassword()).trim();

            boolean enableMonitoring = chkMonitoring.isSelected();

            if (username.isEmpty() || password.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập ID và mật khẩu.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            } else {
                btnStartShare.setEnabled(false);
                btnStartShare.setText("Đang kết nối...");
                SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

                    private Socket socketScreen, socketControl, socketChat;
                    private GraphicsEnvironment gEnv = GraphicsEnvironment.getLocalGraphicsEnvironment();
                    private GraphicsDevice gDev = gEnv.getDefaultScreenDevice();

                    @Override
                    protected Void doInBackground() throws Exception {

                        try {
                            socketScreen = new Socket(ipServer, 5000);
                            socketControl = new Socket(ipServer, 6000);
                            socketChat = new Socket(ipServer, CHAT_PORT);
                            String initMessage = username + "," + password + ",sharer," + screen.getWidth() + ","
                                    + screen.getHeight();
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
                            if (responseScreen.startsWith("false") || responseControl.startsWith("false")
                                    || responseChat.startsWith("false")) {
                                throw new Exception("Server response: " + responseScreen + " | " + responseControl
                                        + " | " + responseChat);
                            }
                        } catch (Exception e) {
                            try {
                                if (socketScreen != null)
                                    socketScreen.close();
                            } catch (Exception ex) {
                            }
                            try {
                                if (socketControl != null)
                                    socketControl.close();
                            } catch (Exception ex) {
                            }
                            try {
                                if (socketChat != null)
                                    socketChat.close();
                            } catch (Exception ex) {
                            }
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

                            Robot rb = new Robot(gDev);
                            new ReceiveEvent(socketControl, socketScreen, socketChat, rb, screen.height, screen.width,
                                    btnStartShare);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                            JOptionPane.showMessageDialog(MainStart.this,
                                    "Không thể kết nối đến server: " + ex.getMessage(), "Lỗi",
                                    JOptionPane.ERROR_MESSAGE);

                            try {
                                if (socketScreen != null)
                                    socketScreen.close();
                            } catch (Exception e) {
                            }
                            try {
                                if (socketControl != null)
                                    socketControl.close();
                            } catch (Exception e) {
                            }
                            try {
                                if (socketChat != null)
                                    socketChat.close();
                            } catch (Exception e) {
                            }
                            btnStartShare.setEnabled(true);
                            btnStartShare.setText("Cho phép điều khiển");
                        }
                    }
                };
                worker.execute();
            }
        });

        JPanel rightPanel = UIHelper.createStyledPanel();

        JPanel rightHeader = UIHelper.createHeaderPanel("Điều khiển máy tính khác", UIHelper.createComputerIcon());
        rightPanel.add(rightHeader, BorderLayout.NORTH);
        JPanel rightContent = new JPanel(new GridBagLayout());
        rightContent.setBackground(Color.WHITE);
        rightContent.setBorder(BorderFactory.createEmptyBorder(15, 20, 20, 20));
        GridBagConstraints gbcRight = new GridBagConstraints();
        gbcRight.insets = new Insets(5, 5, 5, 5);
        gbcRight.fill = GridBagConstraints.HORIZONTAL;
        gbcRight.anchor = GridBagConstraints.WEST;

        JLabel lblDescRight = new JLabel("<html>Nhập ID và Mật khẩu của máy bạn cần điều khiển.</html>");
        lblDescRight.setFont(UIHelper.FONT_LABEL);
        lblDescRight.setForeground(UIHelper.COLOR_TEXT_DARK);
        gbcRight.gridy = 0;
        gbcRight.gridx = 0;
        gbcRight.gridwidth = 2;
        gbcRight.insets = new Insets(0, 5, 20, 5);
        rightContent.add(lblDescRight, gbcRight);

        // --- Reset GBC ---
        gbcRight.gridwidth = 1;
        gbcRight.insets = new Insets(5, 5, 5, 5);

        JLabel lblIDDT = new JLabel("ID đối tác");
        lblIDDT.setFont(UIHelper.FONT_LABEL);
        lblIDDT.setForeground(UIHelper.COLOR_TEXT_DARK);
        gbcRight.gridx = 0;
        gbcRight.gridy = 1;
        gbcRight.fill = GridBagConstraints.NONE;
        gbcRight.weightx = 0.0;
        rightContent.add(lblIDDT, gbcRight);

        JTextField txtIDDT = new JTextField(15);
        txtIDDT.setFont(UIHelper.FONT_FIELD);
        UIHelper.styleTextField(txtIDDT);
        gbcRight.gridx = 1;
        gbcRight.gridy = 1;
        gbcRight.weightx = 1.0;
        gbcRight.fill = GridBagConstraints.HORIZONTAL;
        rightContent.add(txtIDDT, gbcRight);

        JLabel lblMKDT = new JLabel("Mật khẩu");
        lblMKDT.setFont(UIHelper.FONT_LABEL);
        lblMKDT.setForeground(UIHelper.COLOR_TEXT_DARK);
        gbcRight.gridx = 0;
        gbcRight.gridy = 2;
        gbcRight.weightx = 0.0;
        gbcRight.fill = GridBagConstraints.NONE;
        rightContent.add(lblMKDT, gbcRight);

        JPasswordField txtMKDT = new JPasswordField(15);
        txtMKDT.setFont(UIHelper.FONT_FIELD);
        UIHelper.styleTextField(txtMKDT);
        gbcRight.gridx = 1;
        gbcRight.gridy = 2;
        gbcRight.weightx = 1.0;
        gbcRight.fill = GridBagConstraints.HORIZONTAL;
        rightContent.add(txtMKDT, gbcRight);

        JPanel spacerPanel = new JPanel();
        spacerPanel.setOpaque(false);
        gbcRight.gridx = 0;
        gbcRight.gridy = 3;
        gbcRight.gridwidth = 2;
        gbcRight.weighty = 1.0;
        rightContent.add(spacerPanel, gbcRight);

        JButton btnStart = UIHelper.createStyledButton("Bắt đầu điều khiển");
        gbcRight.gridx = 0;
        gbcRight.gridy = 4;
        gbcRight.gridwidth = 2;
        gbcRight.fill = GridBagConstraints.NONE;
        gbcRight.anchor = GridBagConstraints.CENTER;
        gbcRight.weighty = 0.0;
        gbcRight.insets = new Insets(0, 5, 10, 5);
        rightContent.add(btnStart, gbcRight);

        rightPanel.add(rightContent, BorderLayout.CENTER);
        btnStart.addActionListener(e -> {
            String partnerID = txtIDDT.getText().trim();
            String partnerPassword = new String(txtMKDT.getPassword()).trim();

            if (partnerID.isEmpty() || partnerPassword.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập ID và mật khẩu đối tác.", "Lỗi",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }

            btnStart.setEnabled(false);
            btnStart.setText("Đang kết nối...");

            SwingWorker<Void, Void> controlWorker = new SwingWorker<Void, Void>() {

                private Socket socketScreen, socketControl, socketChat;
                private DataInputStream disControl; // Giữ luồng đọc
                private float remoteWidth, remoteHeight;

                private GraphicsEnvironment gEnv = GraphicsEnvironment.getLocalGraphicsEnvironment();
                private GraphicsDevice gDev = gEnv.getDefaultScreenDevice();

                private String serverErrorMessage = null;

                @Override
                protected Void doInBackground() throws Exception {

                    try {
                        socketScreen = new Socket(ipServer, 5000);
                        socketControl = new Socket(ipServer, 6000);
                        socketChat = new Socket(ipServer, CHAT_PORT);
                        String initMessage = partnerID + "," + partnerPassword + ",viewer," + screen.getWidth() + ","
                                + screen.getHeight();
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
                        this.disControl = new DataInputStream(socketControl.getInputStream());
                        DataInputStream disChat = new DataInputStream(socketChat.getInputStream());

                        String responseScreen = disScreen.readUTF();
                        String responseControl = this.disControl.readUTF();
                        String responseChat = disChat.readUTF();

                        if (responseScreen.startsWith("false")) {
                            serverErrorMessage = responseScreen.split(",")[1]; // Lấy "Session not found"
                            throw new Exception(serverErrorMessage);
                        }
                        if (responseControl.startsWith("false")) {
                            serverErrorMessage = responseControl.split(",")[1];
                            throw new Exception(serverErrorMessage);
                        }
                        if (responseChat.startsWith("false")) {
                            serverErrorMessage = responseChat.split(",")[1];
                            throw new Exception(serverErrorMessage);
                        }
                        String[] res = responseScreen.split(",");
                        if (res.length < 3) {
                            throw new Exception("Lỗi dữ liệu trả về: " + responseScreen);
                        }
                        remoteWidth = Float.parseFloat(res[1]);
                        remoteHeight = Float.parseFloat(res[2]);
                    } catch (Exception ex) {
                        try {
                            if (socketScreen != null)
                                socketScreen.close();
                        } catch (Exception e) {
                        }
                        try {
                            if (socketControl != null)
                                socketControl.close();
                        } catch (Exception e) {
                        }
                        try {
                            if (socketChat != null)
                                socketChat.close();
                        } catch (Exception e) {
                        }
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
                        JOptionPane.showMessageDialog(MainStart.this,
                                "Không thể kết nối đến đối tác: " + serverErrorMessage, // Dùng biến mới
                                "Lỗi kết nối",
                                JOptionPane.ERROR_MESSAGE);
                    } finally {
                        btnStart.setEnabled(true);
                        btnStart.setText("Bắt đầu điều khiển");
                    }
                }
            };
            controlWorker.execute();
        });

        mainPanel.add(leftPanel);
        mainPanel.add(rightPanel);
        add(mainPanel, BorderLayout.CENTER);

        add(UIHelper.createStatusBar(), BorderLayout.SOUTH);
    }

    public static void main(String[] args) {
        Dotenv dotenv = Dotenv.configure()
                .directory("./")
                .ignoreIfMissing()
                .load();
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            UIManager.put("Button.disabledText", new Color(173, 216, 230));

        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            try {

                String ipServer = dotenv.get("IP_SERVER", "192.168.2.1");
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
